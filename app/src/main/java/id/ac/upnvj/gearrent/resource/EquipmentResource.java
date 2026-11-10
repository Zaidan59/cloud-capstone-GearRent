package id.ac.upnvj.gearrent.resource;

import id.ac.upnvj.gearrent.entity.EquipmentUnit;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.util.List;

@Path("/api/equipments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Equipment Management", description = "Operations for multimedia rental assets per tenant")
public class EquipmentResource {

    @GET
    @Operation(summary = "List all equipment units, optionally filtered by tenant")
    public List<EquipmentUnit> list(@QueryParam("tenantId") String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            return EquipmentUnit.list("tenantId", tenantId);
        }
        return EquipmentUnit.listAll();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get equipment unit details by ID")
    public Response getById(@PathParam("id") Long id) {
        EquipmentUnit unit = EquipmentUnit.findById(id);
        if (unit == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(unit).build();
    }

    @POST
    @Transactional
    @Operation(summary = "Register a new equipment asset")
    public Response create(EquipmentUnit unit) {
        if (unit.tenantId == null || unit.tenantId.isBlank()) {
            unit.tenantId = "tenant-a"; // Default tenant
        }
        unit.persist();
        return Response.status(Response.Status.CREATED).entity(unit).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Operation(summary = "Update equipment asset information")
    public Response update(@PathParam("id") Long id, EquipmentUnit updated) {
        EquipmentUnit unit = EquipmentUnit.findById(id);
        if (unit == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        unit.name = updated.name;
        unit.category = updated.category;
        unit.dailyRate = updated.dailyRate;
        unit.conditionNotes = updated.conditionNotes;
        if (updated.status != null) {
            unit.status = updated.status;
        }
        return Response.ok(unit).build();
    }
}
