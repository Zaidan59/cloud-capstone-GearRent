package id.ac.upnvj.gearrent.service;

import id.ac.upnvj.gearrent.dto.CreateReservationRequest;
import id.ac.upnvj.gearrent.dto.HandoverRequest;
import id.ac.upnvj.gearrent.dto.ReturnRequest;
import id.ac.upnvj.gearrent.entity.EquipmentUnit;
import id.ac.upnvj.gearrent.entity.InspectionRecord;
import id.ac.upnvj.gearrent.entity.MaintenanceRecord;
import id.ac.upnvj.gearrent.entity.Reservation;
import id.ac.upnvj.gearrent.entity.ReservationItem;
import id.ac.upnvj.gearrent.security.AccessContextResolver;
import id.ac.upnvj.gearrent.security.CallerContext;
import io.quarkus.hibernate.orm.panache.Panache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Handles multi-unit availability checks, atomic pessimistic-locked allocations, handovers, and returns. */
@ApplicationScoped
public class ReservationService {

    @Inject
    AccessContextResolver access;

    public List<EquipmentUnit> findAvailableEquipments(String tenantId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return EquipmentUnit.find(
            "tenantId = ?1 and status <> 'MAINTENANCE' and id not in ("
                + "select ri.equipment.id from ReservationItem ri "
                + "where ri.reservation.tenantId = ?1 "
                + "and ri.reservation.status in ('APPROVED', 'CHECKED_OUT') "
                + "and ri.reservation.startDate < ?3 and ri.reservation.endDate > ?2"
                + ")",
            tenantId, startDate, endDate
        ).list();
    }

    @Transactional
    public Reservation createReservation(CallerContext ctx, CreateReservationRequest req) {
        if (req == null || req.customerName == null || req.customerName.isBlank()) {
            throw AccessContextResolver.error(Response.Status.BAD_REQUEST, "customerName is required.");
        }
        validateDateRange(req.startDate, req.endDate);
        if (req.equipmentIds == null || req.equipmentIds.isEmpty()) {
            throw AccessContextResolver.error(Response.Status.BAD_REQUEST, "At least one equipmentId is required.");
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(req.equipmentIds);
        long days = ChronoUnit.DAYS.between(req.startDate, req.endDate);

        Reservation reservation = new Reservation();
        reservation.tenantId = ctx.tenantId();
        reservation.customerName = req.customerName.trim();
        reservation.customerEmail = req.customerEmail;
        reservation.createdBy = ctx.username();
        reservation.startDate = req.startDate;
        reservation.endDate = req.endDate;
        reservation.notes = req.notes;
        reservation.status = "PENDING";

        double total = 0.0;
        for (Long equipmentId : uniqueIds) {
            if (equipmentId == null) {
                throw AccessContextResolver.error(Response.Status.BAD_REQUEST, "equipmentIds cannot contain null.");
            }
            EquipmentUnit unit = EquipmentUnit.findById(equipmentId);
            if (unit == null) {
                throw AccessContextResolver.error(Response.Status.NOT_FOUND, "Equipment unit not found: " + equipmentId);
            }
            access.requireTenantMatch(ctx, unit.tenantId);
            ensureUnitBookableForPeriod(unit, null, req.startDate, req.endDate);

            ReservationItem item = new ReservationItem();
            item.reservation = reservation;
            item.equipment = unit;
            item.dailyRateSnapshot = unit.dailyRate != null ? unit.dailyRate : 0.0;
            item.itemStatus = "REQUESTED";
            reservation.items.add(item);

            total += item.dailyRateSnapshot * days;
        }
        reservation.totalCost = total;
        reservation.persist();
        return reservation;
    }

    @Transactional
    public Reservation approveReservation(CallerContext ctx, Long reservationId) {
        access.requireOperatorOrAdmin(ctx);
        Reservation res = requireReservationForTenant(ctx, reservationId);
        if (!"PENDING".equals(res.status)) {
            throw AccessContextResolver.error(
                Response.Status.CONFLICT,
                "Only PENDING reservations can be approved (current: " + res.status + ")."
            );
        }

        // Canonical ascending lock order prevents deadlocks across concurrent multi-unit approvals.
        List<Long> sortedIds = res.items.stream().map(i -> i.equipment.id).distinct().sorted().toList();
        for (Long unitId : sortedIds) {
            EquipmentUnit lockedUnit = lockUnit(unitId);
            if (lockedUnit == null) {
                throw AccessContextResolver.error(Response.Status.NOT_FOUND, "Equipment unit not found: " + unitId);
            }
            access.requireTenantMatch(ctx, lockedUnit.tenantId);
            ensureUnitBookableForPeriod(lockedUnit, res.id, res.startDate, res.endDate);
        }

        res.status = "APPROVED";
        for (ReservationItem item : res.items) {
            item.itemStatus = "ALLOCATED";
        }
        return res;
    }

    @Transactional
    public Reservation rejectReservation(CallerContext ctx, Long reservationId, String reason) {
        access.requireOperatorOrAdmin(ctx);
        Reservation res = requireReservationForTenant(ctx, reservationId);
        if (!"PENDING".equals(res.status)) {
            throw AccessContextResolver.error(
                Response.Status.CONFLICT,
                "Only PENDING reservations can be rejected (current: " + res.status + ")."
            );
        }
        res.status = "REJECTED";
        if (reason != null && !reason.isBlank()) {
            res.notes = reason;
        }
        for (ReservationItem item : res.items) {
            item.itemStatus = "CANCELLED";
        }
        return res;
    }

    @Transactional
    public Reservation cancelReservation(CallerContext ctx, Long reservationId) {
        Reservation res = requireReservationForTenant(ctx, reservationId);
        access.requireReservationAccess(ctx, res);
        if (!"PENDING".equals(res.status) && !"APPROVED".equals(res.status)) {
            throw AccessContextResolver.error(
                Response.Status.CONFLICT,
                "Cannot cancel reservation in status: " + res.status
            );
        }
        res.status = "CANCELLED";
        for (ReservationItem item : res.items) {
            item.itemStatus = "CANCELLED";
        }
        return res;
    }

    @Transactional
    public Map<String, Object> handoverReservation(CallerContext ctx, Long reservationId, HandoverRequest req) {
        access.requireOperatorOrAdmin(ctx);
        Reservation res = requireReservationForTenant(ctx, reservationId);
        if (!"APPROVED".equals(res.status)) {
            throw AccessContextResolver.error(
                Response.Status.CONFLICT,
                "Only APPROVED reservations can be handed over (current: " + res.status + ")."
            );
        }

        Map<Long, HandoverRequest.ItemInspectionInput> overrides = (req != null && req.items != null)
            ? req.items.stream()
                .filter(i -> i.equipmentId != null)
                .collect(Collectors.toMap(i -> i.equipmentId, Function.identity(), (a, b) -> b))
            : Map.of();

        List<Long> sortedIds = res.items.stream().map(i -> i.equipment.id).distinct().sorted().toList();
        Map<Long, EquipmentUnit> lockedUnits = sortedIds.stream()
            .collect(Collectors.toMap(Function.identity(), this::lockUnit));

        for (EquipmentUnit unit : lockedUnits.values()) {
            if (unit == null) {
                throw AccessContextResolver.error(Response.Status.NOT_FOUND, "Equipment unit missing during handover.");
            }
            if (!"AVAILABLE".equalsIgnoreCase(unit.status)) {
                throw AccessContextResolver.error(
                    Response.Status.CONFLICT,
                    "Unit " + unit.assetCode + " is physically not AVAILABLE (status: " + unit.status + ")."
                );
            }
        }

        List<InspectionRecord> inspections = new ArrayList<>();
        for (ReservationItem item : res.items) {
            EquipmentUnit unit = lockedUnits.get(item.equipment.id);
            unit.status = "RENTED";
            item.itemStatus = "CHECKED_OUT";

            HandoverRequest.ItemInspectionInput itemInput = overrides.get(unit.id);
            InspectionRecord rec = new InspectionRecord();
            rec.tenantId = res.tenantId;
            rec.reservation = res;
            rec.equipment = unit;
            rec.stage = "HANDOVER";
            rec.accessoriesComplete = itemInput != null && itemInput.accessoriesComplete != null
                ? itemInput.accessoriesComplete
                : (req != null && req.accessoriesComplete != null ? req.accessoriesComplete : Boolean.TRUE);
            rec.accessoriesChecklist = itemInput != null && itemInput.accessoriesChecklist != null
                ? itemInput.accessoriesChecklist
                : (req != null ? req.accessoriesChecklist : null);
            rec.conditionNotes = itemInput != null && itemInput.conditionNotes != null
                ? itemInput.conditionNotes
                : (req != null ? req.conditionNotes : "Handed over in good condition");
            rec.hasDamage = Boolean.FALSE;
            rec.photoObjectKey = itemInput != null && itemInput.photoObjectKey != null
                ? itemInput.photoObjectKey
                : (req != null ? req.photoObjectKey : null);
            rec.inspectedBy = ctx.username();
            rec.inspectedAt = LocalDateTime.now();
            rec.persist();
            inspections.add(rec);
        }

        res.status = "CHECKED_OUT";
        return Map.of("reservation", res, "inspections", inspections);
    }

    @Transactional
    public Map<String, Object> returnReservation(CallerContext ctx, Long reservationId, ReturnRequest req) {
        access.requireOperatorOrAdmin(ctx);
        Reservation res = requireReservationForTenant(ctx, reservationId);
        if (!"CHECKED_OUT".equals(res.status) && !"APPROVED".equals(res.status)) {
            throw AccessContextResolver.error(
                Response.Status.CONFLICT,
                "Only CHECKED_OUT or APPROVED reservations can be returned (current: " + res.status + ")."
            );
        }

        LocalDate returnDate = req != null && req.actualReturnDate != null ? req.actualReturnDate : LocalDate.now();
        res.actualReturnDate = returnDate;
        res.lateReturn = returnDate.isAfter(res.endDate);
        res.status = "RETURNED";
        if (req != null && req.notes != null) {
            res.notes = req.notes;
        }

        Map<Long, ReturnRequest.ItemReturnInput> overrides = (req != null && req.items != null)
            ? req.items.stream()
                .filter(i -> i.equipmentId != null)
                .collect(Collectors.toMap(i -> i.equipmentId, Function.identity(), (a, b) -> b))
            : Map.of();

        List<Long> sortedIds = res.items.stream().map(i -> i.equipment.id).distinct().sorted().toList();
        Map<Long, EquipmentUnit> lockedUnits = sortedIds.stream()
            .collect(Collectors.toMap(Function.identity(), this::lockUnit));

        List<InspectionRecord> inspections = new ArrayList<>();
        int maintenanceCreated = 0;

        for (ReservationItem item : res.items) {
            EquipmentUnit unit = lockedUnits.get(item.equipment.id);
            item.itemStatus = "RETURNED";

            ReturnRequest.ItemReturnInput itemInput = overrides.get(unit.id);
            boolean damaged = itemInput != null && itemInput.hasDamage != null
                ? itemInput.hasDamage
                : (req != null && Boolean.TRUE.equals(req.hasDamage));
            boolean complete = itemInput != null && itemInput.accessoriesComplete != null
                ? itemInput.accessoriesComplete
                : (req != null && req.accessoriesComplete != null ? req.accessoriesComplete : Boolean.TRUE);
            String checklist = itemInput != null && itemInput.accessoriesChecklist != null
                ? itemInput.accessoriesChecklist
                : (req != null ? req.accessoriesChecklist : null);
            String notes = itemInput != null && itemInput.conditionNotes != null
                ? itemInput.conditionNotes
                : (req != null && req.conditionNotes != null ? req.conditionNotes
                    : (req != null && req.notes != null ? req.notes : "Normal return"));
            String photoKey = itemInput != null && itemInput.photoObjectKey != null
                ? itemInput.photoObjectKey
                : (req != null ? req.photoObjectKey : null);

            InspectionRecord rec = new InspectionRecord();
            rec.tenantId = res.tenantId;
            rec.reservation = res;
            rec.equipment = unit;
            rec.stage = "RETURN";
            rec.accessoriesComplete = complete;
            rec.accessoriesChecklist = checklist;
            rec.conditionNotes = notes;
            rec.hasDamage = damaged;
            rec.photoObjectKey = photoKey;
            rec.inspectedBy = ctx.username();
            rec.inspectedAt = LocalDateTime.now();
            rec.persist();
            inspections.add(rec);

            if (damaged) {
                unit.status = "MAINTENANCE";
                unit.conditionNotes = "Damaged during rental #" + res.id + ": " + notes;

                MaintenanceRecord m = new MaintenanceRecord();
                m.tenantId = res.tenantId;
                m.equipment = unit;
                m.sourceReservationId = res.id;
                m.issueDescription = notes;
                m.status = "OPEN";
                m.persist();
                maintenanceCreated++;
            } else {
                unit.status = "AVAILABLE";
            }
        }

        return Map.of(
            "reservation", res,
            "lateReturn", Boolean.TRUE.equals(res.lateReturn),
            "inspections", inspections,
            "maintenanceOrdersCreated", maintenanceCreated
        );
    }

    private Reservation requireReservationForTenant(CallerContext ctx, Long id) {
        Reservation res = Reservation.findById(id);
        if (res == null) {
            throw AccessContextResolver.error(Response.Status.NOT_FOUND, "Reservation not found: " + id);
        }
        access.requireTenantMatch(ctx, res.tenantId);
        return res;
    }

    private EquipmentUnit lockUnit(Long unitId) {
        return Panache.getEntityManager().find(EquipmentUnit.class, unitId, LockModeType.PESSIMISTIC_WRITE);
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw AccessContextResolver.error(Response.Status.BAD_REQUEST, "startDate and endDate are required.");
        }
        if (!endDate.isAfter(startDate)) {
            throw AccessContextResolver.error(Response.Status.BAD_REQUEST, "endDate must be after startDate.");
        }
    }

    private static void ensureUnitBookableForPeriod(
        EquipmentUnit unit, Long excludeReservationId, LocalDate startDate, LocalDate endDate
    ) {
        if ("MAINTENANCE".equalsIgnoreCase(unit.status)
            || MaintenanceRecord.count("equipment.id = ?1 and status = 'OPEN'", unit.id) > 0) {
            throw AccessContextResolver.error(
                Response.Status.CONFLICT,
                "Equipment unit " + unit.assetCode + " is under maintenance."
            );
        }
        long overlaps = excludeReservationId == null
            ? ReservationItem.count(
                "equipment.id = ?1 and reservation.status in ('APPROVED', 'CHECKED_OUT') "
                    + "and reservation.startDate < ?3 and reservation.endDate > ?2",
                unit.id, startDate, endDate
            )
            : ReservationItem.count(
                "equipment.id = ?1 and reservation.id <> ?4 and reservation.status in ('APPROVED', 'CHECKED_OUT') "
                    + "and reservation.startDate < ?3 and reservation.endDate > ?2",
                unit.id, startDate, endDate, excludeReservationId
            );
        if (overlaps > 0) {
            throw AccessContextResolver.error(
                Response.Status.CONFLICT,
                "Equipment unit " + unit.assetCode + " already has an overlapping approved/active reservation."
            );
        }
    }
}
