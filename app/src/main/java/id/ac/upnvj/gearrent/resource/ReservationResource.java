package id.ac.upnvj.gearrent.resource;

import id.ac.upnvj.gearrent.entity.EquipmentUnit;
import id.ac.upnvj.gearrent.entity.MaintenanceRecord;
import id.ac.upnvj.gearrent.entity.Reservation;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Path("/api/reservations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Reservation Workflow", description = "Rental reservation, approval, and equipment return workflow")
public class ReservationResource {

    @GET
    @Operation(summary = "List all reservations, optionally filtered by tenant")
    public List<Reservation> list(@QueryParam("tenantId") String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            return Reservation.list("tenantId", tenantId);
        }
        return Reservation.listAll();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get reservation details by ID")
    public Response getById(@PathParam("id") Long id) {
        Reservation reservation = Reservation.findById(id);
        if (reservation == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(reservation).build();
    }

    @POST
    @Transactional
    @Operation(summary = "Create a new rental reservation request")
    public Response create(Reservation request) {
        if (request.equipment == null || request.equipment.id == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Equipment ID is required"))
                    .build();
        }

        EquipmentUnit unit = EquipmentUnit.findById(request.equipment.id);
        if (unit == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Equipment unit not found"))
                    .build();
        }

        if (!"AVAILABLE".equalsIgnoreCase(unit.status)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Equipment is currently not available: " + unit.status))
                    .build();
        }

        request.equipment = unit;
        if (request.tenantId == null || request.tenantId.isBlank()) {
            request.tenantId = unit.tenantId;
        }

        if (request.startDate != null && request.endDate != null) {
            long days = ChronoUnit.DAYS.between(request.startDate, request.endDate);
            if (days <= 0) days = 1;
            request.totalCost = days * (unit.dailyRate != null ? unit.dailyRate : 0.0);
        }

        request.status = "PENDING";
        request.persist();

        return Response.status(Response.Status.CREATED).entity(request).build();
    }

    @PUT
    @Path("/{id}/approve")
    @Transactional
    @Operation(summary = "Operator approves reservation and allocates equipment (status -> RENTED)")
    public Response approve(@PathParam("id") Long id) {
        Reservation res = Reservation.findById(id);
        if (res == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        EquipmentUnit unit = EquipmentUnit.findById(res.equipment.id);
        if (unit == null || !"AVAILABLE".equalsIgnoreCase(unit.status)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Cannot approve: Equipment unit is already rented or under maintenance"))
                    .build();
        }

        res.status = "APPROVED";
        unit.status = "RENTED";

        return Response.ok(res).build();
    }

    @POST
    @Path("/{id}/return")
    @Transactional
    @Operation(summary = "Record equipment return, condition inspection, and flag maintenance if needed")
    public Response returnEquipment(@PathParam("id") Long id, Map<String, Object> payload) {
        Reservation res = Reservation.findById(id);
        if (res == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        EquipmentUnit unit = EquipmentUnit.findById(res.equipment.id);
        if (unit == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        boolean hasDamage = Boolean.TRUE.equals(payload.get("hasDamage"));
        String notes = (String) payload.getOrDefault("notes", "Normal return");

        res.status = "RETURNED";
        res.returnNotes = notes;

        if (hasDamage) {
            unit.status = "MAINTENANCE";
            unit.conditionNotes = "Damaged during rental #" + res.id + ": " + notes;

            MaintenanceRecord m = new MaintenanceRecord();
            m.tenantId = res.tenantId;
            m.equipment = unit;
            m.issueDescription = notes;
            m.status = "OPEN";
            m.persist();
        } else {
            unit.status = "AVAILABLE";
        }

        return Response.ok(Map.of(
                "reservation", res,
                "equipmentStatus", unit.status,
                "message", hasDamage ? "Returned with damage. Maintenance order created." : "Equipment returned in good condition."
        )).build();
    }
}
