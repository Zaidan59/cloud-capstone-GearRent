package id.ac.upnvj.gearrent.resource;

import id.ac.upnvj.gearrent.entity.EquipmentUnit;
import id.ac.upnvj.gearrent.security.AccessContextResolver;
import id.ac.upnvj.gearrent.security.CallerContext;
import id.ac.upnvj.gearrent.service.ReservationService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/equipments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Equipment Management", description = "Operations and availability lookup for multimedia rental assets per tenant")
public class EquipmentResource {

    @Inject
    AccessContextResolver access;

    @Inject
    ReservationService reservationService;

    @GET
    @Operation(summary = "List equipment units belonging to the caller's tenant")
    public List<EquipmentUnit> list(@Context HttpHeaders headers) {
        CallerContext ctx = access.resolve(headers);
        return EquipmentUnit.list("tenantId", ctx.tenantId());
    }

    @GET
    @Path("/available")
    @Operation(summary = "Search available equipment units for a requested rental period (inclusive boundary allowed)")
    public List<EquipmentUnit> findAvailable(
        @Context HttpHeaders headers,
        @QueryParam("startDate") LocalDate startDate,
        @QueryParam("endDate") LocalDate endDate
    ) {
        CallerContext ctx = access.resolve(headers);
        return reservationService.findAvailableEquipments(ctx.tenantId(), startDate, endDate);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get equipment unit details by ID within the caller's tenant")
    public Response getById(@Context HttpHeaders headers, @PathParam("id") Long id) {
        CallerContext ctx = access.resolve(headers);
        EquipmentUnit unit = EquipmentUnit.findById(id);
        if (unit == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        access.requireTenantMatch(ctx, unit.tenantId);
        return Response.ok(unit).build();
    }

    @POST
    @Transactional
    @Operation(summary = "Register a new equipment asset in the caller's tenant (operator/admin only)")
    public Response create(@Context HttpHeaders headers, EquipmentUnit unit) {
        CallerContext ctx = access.resolve(headers);
        access.requireOperatorOrAdmin(ctx);

        if (unit == null || isBlank(unit.assetCode) || isBlank(unit.name) || isBlank(unit.category) || unit.dailyRate == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "assetCode, name, category, and dailyRate are required."))
                .build();
        }
        if (EquipmentUnit.count("tenantId = ?1 and assetCode = ?2", ctx.tenantId(), unit.assetCode.trim()) > 0) {
            return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", "Asset code already exists in tenant: " + unit.assetCode))
                .build();
        }

        unit.id = null;
        unit.tenantId = ctx.tenantId();
        unit.assetCode = unit.assetCode.trim();
        unit.name = unit.name.trim();
        unit.category = unit.category.trim();
        if (isBlank(unit.status)) {
            unit.status = "AVAILABLE";
        }
        unit.persist();
        return Response.status(Response.Status.CREATED).entity(unit).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Operation(summary = "Update equipment asset information within the caller's tenant (operator/admin only)")
    public Response update(@Context HttpHeaders headers, @PathParam("id") Long id, EquipmentUnit updated) {
        CallerContext ctx = access.resolve(headers);
        access.requireOperatorOrAdmin(ctx);

        EquipmentUnit unit = EquipmentUnit.findById(id);
        if (unit == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        access.requireTenantMatch(ctx, unit.tenantId);

        if (updated == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Request body is required."))
                .build();
        }
        if (!isBlank(updated.name)) unit.name = updated.name.trim();
        if (!isBlank(updated.category)) unit.category = updated.category.trim();
        if (updated.dailyRate != null) unit.dailyRate = updated.dailyRate;
        if (updated.conditionNotes != null) unit.conditionNotes = updated.conditionNotes;
        if (!isBlank(updated.status)) unit.status = updated.status;
        return Response.ok(unit).build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
