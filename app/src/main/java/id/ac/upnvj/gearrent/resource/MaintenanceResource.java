package id.ac.upnvj.gearrent.resource;

import id.ac.upnvj.gearrent.entity.EquipmentUnit;
import id.ac.upnvj.gearrent.entity.MaintenanceRecord;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.util.List;
import java.util.Map;

@Path("/api/maintenance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Maintenance Orders", description = "Track and resolve equipment maintenance and inspection records")
public class MaintenanceResource {

    @GET
    @Operation(summary = "List all maintenance work orders, optionally filtered by tenant")
    public List<MaintenanceRecord> list(@QueryParam("tenantId") String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            return MaintenanceRecord.list("tenantId", tenantId);
        }
        return MaintenanceRecord.listAll();
    }

    @PUT
    @Path("/{id}/resolve")
    @Transactional
    @Operation(summary = "Resolve maintenance order and restore equipment to AVAILABLE")
    public Response resolve(@PathParam("id") Long id, Map<String, String> payload) {
        MaintenanceRecord record = MaintenanceRecord.findById(id);
        if (record == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String notes = payload != null ? payload.getOrDefault("notes", "Repairs completed successfully") : "Repairs completed";
        record.status = "RESOLVED";
        record.resolutionNotes = notes;

        if (record.equipment != null) {
            EquipmentUnit unit = EquipmentUnit.findById(record.equipment.id);
            if (unit != null) {
                unit.status = "AVAILABLE";
                unit.conditionNotes = "Maintenance completed: " + notes;
            }
        }

        return Response.ok(record).build();
    }
}
