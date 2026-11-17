package id.ac.upnvj.gearrent.resource;

import id.ac.upnvj.gearrent.dto.CreateReservationRequest;
import id.ac.upnvj.gearrent.dto.HandoverRequest;
import id.ac.upnvj.gearrent.dto.ReturnRequest;
import id.ac.upnvj.gearrent.entity.Reservation;
import id.ac.upnvj.gearrent.security.AccessContextResolver;
import id.ac.upnvj.gearrent.security.CallerContext;
import id.ac.upnvj.gearrent.service.ReservationService;
import jakarta.inject.Inject;
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
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/reservations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Reservation Workflow", description = "Multi-unit rental reservation, atomic approval, handover, and return workflow")
public class ReservationResource {

    @Inject
    AccessContextResolver access;

    @Inject
    ReservationService reservationService;

    @GET
    @Operation(summary = "List reservations for caller's tenant (users only see their own reservations)")
    public List<Reservation> list(@Context HttpHeaders headers) {
        CallerContext ctx = access.resolve(headers);
        if (ctx.isUser()) {
            return Reservation.list("tenantId = ?1 and createdBy = ?2", ctx.tenantId(), ctx.username());
        }
        return Reservation.list("tenantId", ctx.tenantId());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get reservation details by ID within caller's tenant")
    public Response getById(@Context HttpHeaders headers, @PathParam("id") Long id) {
        CallerContext ctx = access.resolve(headers);
        Reservation reservation = Reservation.findById(id);
        if (reservation == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        access.requireReservationAccess(ctx, reservation);
        return Response.ok(reservation).build();
    }

    @POST
    @Operation(summary = "Create a new multi-unit rental reservation request")
    public Response create(@Context HttpHeaders headers, CreateReservationRequest request) {
        CallerContext ctx = access.resolve(headers);
        Reservation created = reservationService.createReservation(ctx, request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}/approve")
    @Operation(summary = "Atomically lock and approve all units in a reservation (operator/admin only)")
    public Response approve(@Context HttpHeaders headers, @PathParam("id") Long id) {
        CallerContext ctx = access.resolve(headers);
        return Response.ok(reservationService.approveReservation(ctx, id)).build();
    }

    @PUT
    @Path("/{id}/reject")
    @Operation(summary = "Reject a pending reservation (operator/admin only)")
    public Response reject(@Context HttpHeaders headers, @PathParam("id") Long id, Map<String, String> payload) {
        CallerContext ctx = access.resolve(headers);
        String reason = payload != null ? payload.get("reason") : null;
        return Response.ok(reservationService.rejectReservation(ctx, id, reason)).build();
    }

    @PUT
    @Path("/{id}/cancel")
    @Operation(summary = "Cancel a PENDING or APPROVED reservation")
    public Response cancel(@Context HttpHeaders headers, @PathParam("id") Long id) {
        CallerContext ctx = access.resolve(headers);
        return Response.ok(reservationService.cancelReservation(ctx, id)).build();
    }

    @POST
    @Path("/{id}/handover")
    @Operation(summary = "Record handover inspection and mark units RENTED (operator/admin only)")
    public Response handover(@Context HttpHeaders headers, @PathParam("id") Long id, HandoverRequest request) {
        CallerContext ctx = access.resolve(headers);
        return Response.ok(reservationService.handoverReservation(ctx, id, request)).build();
    }

    @POST
    @Path("/{id}/return")
    @Operation(summary = "Record return inspection, detect late return, and create maintenance orders for damaged units (operator/admin only)")
    public Response returnEquipment(@Context HttpHeaders headers, @PathParam("id") Long id, ReturnRequest request) {
        CallerContext ctx = access.resolve(headers);
        return Response.ok(reservationService.returnReservation(ctx, id, request)).build();
    }
}
