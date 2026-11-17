package id.ac.upnvj.gearrent.resource;

import id.ac.upnvj.gearrent.dto.CreateMaintenanceRequest;
import id.ac.upnvj.gearrent.dto.ResolveMaintenanceRequest;
import id.ac.upnvj.gearrent.entity.EquipmentUnit;
import id.ac.upnvj.gearrent.entity.MaintenanceRecord;
import id.ac.upnvj.gearrent.security.AccessContextResolver;
import id.ac.upnvj.gearrent.security.CallerContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/maintenance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Maintenance Orders", description = "Track and resolve equipment maintenance work orders per tenant")
public class MaintenanceResource {

    @Inject
    AccessContextResolver access;

    @GET
    @Operation(summary = "List maintenance work orders for the caller's tenant (operator/admin only)")
    public List<MaintenanceRecord> list(@Context HttpHeaders headers) {
        CallerContext ctx = access.resolve(headers);
        access.requireOperatorOrAdmin(ctx);
        return MaintenanceRecord.list("tenantId", ctx.tenantId());
    }

    @POST
    @Transactional
    @Operation(summary = "Create a manual maintenance work order and mark unit MAINTENANCE (operator/admin only)")
    public Response create(@Context HttpHeaders headers, CreateMaintenanceRequest req) {
        CallerContext ctx = access.resolve(headers);
        access.requireOperatorOrAdmin(ctx);

        if (req == null || req.equipmentId == null || req.issueDescription == null || req.issueDescription.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "equipmentId and issueDescription are required."))
                .build();
        }
        EquipmentUnit unit = EquipmentUnit.findById(req.equipmentId);
        if (unit == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        access.requireTenantMatch(ctx, unit.tenantId);

        if ("RENTED".equalsIgnoreCase(unit.status)) {
            return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", "Cannot place a currently RENTED unit into maintenance before return."))
                .build();
        }

        unit.status = "MAINTENANCE";
        unit.conditionNotes = req.issueDescription.trim();

        MaintenanceRecord record = new MaintenanceRecord();
        record.tenantId = ctx.tenantId();
        record.equipment = unit;
        record.issueDescription = req.issueDescription.trim();
        record.status = "OPEN";
        record.reportedAt = LocalDateTime.now();
        record.persist();

        return Response.status(Response.Status.CREATED).entity(record).build();
    }

    @PUT
    @Path("/{id}/resolve")
    @Transactional
    @Operation(summary = "Resolve maintenance order and restore unit to AVAILABLE (operator/admin only)")
    public Response resolve(@Context HttpHeaders headers, @PathParam("id") Long id, ResolveMaintenanceRequest payload) {
        CallerContext ctx = access.resolve(headers);
        access.requireOperatorOrAdmin(ctx);

        MaintenanceRecord record = MaintenanceRecord.findById(id);
        if (record == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        access.requireTenantMatch(ctx, record.tenantId);
        if ("RESOLVED".equals(record.status)) {
            return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", "Maintenance record is already RESOLVED."))
                .build();
        }

        String notes = payload != null && payload.notes != null && !payload.notes.isBlank()
            ? payload.notes.trim()
            : "Repairs completed successfully";
        record.status = "RESOLVED";
        record.resolutionNotes = notes;
        record.resolvedAt = LocalDateTime.now();

        if (record.equipment != null) {
            EquipmentUnit unit = EquipmentUnit.findById(record.equipment.id);
            if (unit != null && MaintenanceRecord.count("equipment.id = ?1 and status = 'OPEN' and id <> ?2", unit.id, record.id) == 0) {
                unit.status = "AVAILABLE";
                unit.conditionNotes = "Maintenance completed: " + notes;
            }
        }

        return Response.ok(record).build();
    }
}
