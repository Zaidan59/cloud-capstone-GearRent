package id.ac.upnvj.gearrent.resource;

import id.ac.upnvj.gearrent.entity.InspectionRecord;
import id.ac.upnvj.gearrent.security.AccessContextResolver;
import id.ac.upnvj.gearrent.security.CallerContext;
import id.ac.upnvj.gearrent.service.PhotoStorageService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/inspections")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Condition Inspections & Photos", description = "Handover/return condition inspections and private object-storage photo access")
public class InspectionResource {

    @Inject
    AccessContextResolver access;

    @Inject
    PhotoStorageService photoStorage;

    @GET
    @Operation(summary = "List inspection records for the caller's tenant (optionally filtered by reservationId)")
    public List<InspectionRecord> list(@Context HttpHeaders headers, @QueryParam("reservationId") Long reservationId) {
        CallerContext ctx = access.resolve(headers);
        if (reservationId != null) {
            List<InspectionRecord> list = InspectionRecord.list(
                "tenantId = ?1 and reservation.id = ?2", ctx.tenantId(), reservationId
            );
            if (ctx.isUser()) {
                for (InspectionRecord rec : list) {
                    access.requireReservationAccess(ctx, rec.reservation);
                }
            }
            return list;
        }
        if (ctx.isUser()) {
            return InspectionRecord.list(
                "tenantId = ?1 and reservation.createdBy = ?2", ctx.tenantId(), ctx.username()
            );
        }
        return InspectionRecord.list("tenantId", ctx.tenantId());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get inspection record by ID within caller's tenant")
    public Response getById(@Context HttpHeaders headers, @PathParam("id") Long id) {
        CallerContext ctx = access.resolve(headers);
        InspectionRecord rec = requireInspectionAccess(ctx, id);
        return Response.ok(rec).build();
    }

    @POST
    @Path("/{id}/photo")
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, "image/jpeg", "image/png", "image/webp"})
    @Transactional
    @Operation(summary = "Upload private condition inspection photo (operator/admin only)")
    public Response uploadPhoto(
        @Context HttpHeaders headers,
        @PathParam("id") Long id,
        @QueryParam("filename") String filename,
        byte[] content
    ) {
        CallerContext ctx = access.resolve(headers);
        access.requireOperatorOrAdmin(ctx);

        InspectionRecord rec = InspectionRecord.findById(id);
        if (rec == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        access.requireTenantMatch(ctx, rec.tenantId);

        String contentType = headers.getHeaderString(HttpHeaders.CONTENT_TYPE);
        PhotoStorageService.StoredPhoto stored = photoStorage.store(ctx.tenantId(), rec.id, filename, contentType, content);
        rec.photoBucket = stored.bucket();
        rec.photoObjectKey = stored.objectKey();
        rec.photoContentType = stored.contentType();
        rec.photoSizeBytes = stored.sizeBytes();
        return Response.ok(rec).build();
    }

    @GET
    @Path("/{id}/photo")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, "image/jpeg", "image/png", "image/webp"})
    @Operation(summary = "Download private condition inspection photo after backend tenant/ownership authorization")
    public Response downloadPhoto(@Context HttpHeaders headers, @PathParam("id") Long id) {
        CallerContext ctx = access.resolve(headers);
        InspectionRecord rec = requireInspectionAccess(ctx, id);
        byte[] data = photoStorage.load(rec.photoObjectKey);
        String mediaType = rec.photoContentType != null ? rec.photoContentType : MediaType.APPLICATION_OCTET_STREAM;
        return Response.ok(data, mediaType).build();
    }

    private InspectionRecord requireInspectionAccess(CallerContext ctx, Long id) {
        InspectionRecord rec = InspectionRecord.findById(id);
        if (rec == null) {
            throw AccessContextResolver.error(Response.Status.NOT_FOUND, "Inspection record not found: " + id);
        }
        access.requireReservationAccess(ctx, rec.reservation);
        return rec;
    }
}
