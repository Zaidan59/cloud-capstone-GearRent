package id.ac.upnvj.gearrent.resource;

import id.ac.upnvj.gearrent.entity.EquipmentUnit;
import id.ac.upnvj.gearrent.entity.MaintenanceRecord;
import id.ac.upnvj.gearrent.entity.Reservation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;

@Path("/api/reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Business & Operations Reports", description = "Operational summaries and rental utilization statistics")
public class ReportResource {

    @GET
    @Path("/summary")
    @Operation(summary = "Get high-level business & operational summary per tenant")
    public Map<String, Object> getSummary(@QueryParam("tenantId") String tenantId) {
        Map<String, Object> report = new HashMap<>();

        if (tenantId != null && !tenantId.isBlank()) {
            report.put("tenantId", tenantId);
            report.put("totalEquipments", EquipmentUnit.count("tenantId", tenantId));
            report.put("availableEquipments", EquipmentUnit.count("tenantId = ?1 and status = 'AVAILABLE'", tenantId));
            report.put("rentedEquipments", EquipmentUnit.count("tenantId = ?1 and status = 'RENTED'", tenantId));
            report.put("underMaintenance", EquipmentUnit.count("tenantId = ?1 and status = 'MAINTENANCE'", tenantId));
            report.put("totalReservations", Reservation.count("tenantId", tenantId));
            report.put("openMaintenanceOrders", MaintenanceRecord.count("tenantId = ?1 and status = 'OPEN'", tenantId));
        } else {
            report.put("totalEquipments", EquipmentUnit.count());
            report.put("availableEquipments", EquipmentUnit.count("status", "AVAILABLE"));
            report.put("rentedEquipments", EquipmentUnit.count("status", "RENTED"));
            report.put("underMaintenance", EquipmentUnit.count("status", "MAINTENANCE"));
            report.put("totalReservations", Reservation.count());
            report.put("openMaintenanceOrders", MaintenanceRecord.count("status", "OPEN"));
        }

        return report;
    }
}
