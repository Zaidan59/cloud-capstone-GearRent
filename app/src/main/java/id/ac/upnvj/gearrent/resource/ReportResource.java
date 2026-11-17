package id.ac.upnvj.gearrent.resource;

import id.ac.upnvj.gearrent.entity.EquipmentUnit;
import id.ac.upnvj.gearrent.entity.MaintenanceRecord;
import id.ac.upnvj.gearrent.entity.Reservation;
import id.ac.upnvj.gearrent.security.AccessContextResolver;
import id.ac.upnvj.gearrent.security.CallerContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Business & Operations Reports", description = "Operational summaries and rental utilization statistics per tenant")
public class ReportResource {

    @Inject
    AccessContextResolver access;

    @GET
    @Path("/summary")
    @Operation(summary = "Get business & operational summary for the caller's tenant (operator/admin only)")
    public Map<String, Object> getSummary(@Context HttpHeaders headers) {
        CallerContext ctx = access.resolve(headers);
        access.requireOperatorOrAdmin(ctx);

        String tenantId = ctx.tenantId();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("tenantId", tenantId);
        report.put("totalEquipments", EquipmentUnit.count("tenantId", tenantId));
        report.put("availableEquipments", EquipmentUnit.count("tenantId = ?1 and status = 'AVAILABLE'", tenantId));
        report.put("rentedEquipments", EquipmentUnit.count("tenantId = ?1 and status = 'RENTED'", tenantId));
        report.put("underMaintenance", EquipmentUnit.count("tenantId = ?1 and status = 'MAINTENANCE'", tenantId));
        report.put("totalReservations", Reservation.count("tenantId", tenantId));
        report.put("activeReservations", Reservation.count("tenantId = ?1 and status in ('APPROVED', 'CHECKED_OUT')", tenantId));
        report.put("lateReturns", Reservation.count("tenantId = ?1 and lateReturn = true", tenantId));
        report.put("openMaintenanceOrders", MaintenanceRecord.count("tenantId = ?1 and status = 'OPEN'", tenantId));
        return report;
    }
}
