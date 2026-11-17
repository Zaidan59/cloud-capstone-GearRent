package id.ac.upnvj.gearrent.security;

import id.ac.upnvj.gearrent.entity.Reservation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Resolves and enforces caller role and tenant identity.
 * In M2, trusted context headers enable isolation & negative testing;
 * M6 replaces header trust (trust-headers=false) with verified Keycloak OIDC JWT claims.
 */
@ApplicationScoped
public class AccessContextResolver {

    private static final Set<String> ALLOWED_ROLES = Set.of("user", "operator", "admin");

    @ConfigProperty(name = "gearrent.auth.trust-headers", defaultValue = "true")
    boolean trustHeaders;

    public CallerContext resolve(HttpHeaders headers) {
        if (!trustHeaders) {
            throw error(Response.Status.UNAUTHORIZED, "Bearer token authentication required.");
        }

        String tenantId = headerValue(headers, "X-Tenant-Id");
        String role = headerValue(headers, "X-User-Role");
        String userId = headerValue(headers, "X-User-Id");

        if (tenantId == null || role == null) {
            throw error(
                Response.Status.UNAUTHORIZED,
                "Missing tenant or role context. Provide X-Tenant-Id and X-User-Role headers."
            );
        }

        String normalizedRole = role.toLowerCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw error(Response.Status.FORBIDDEN, "Unsupported role: " + role);
        }

        String username = userId != null ? userId : normalizedRole + "@" + tenantId;
        return new CallerContext(username, normalizedRole, tenantId);
    }

    public void requireOperatorOrAdmin(CallerContext ctx) {
        if (!ctx.isOperatorOrAdmin()) {
            throw error(Response.Status.FORBIDDEN, "Operation requires operator or admin role.");
        }
    }

    public void requireTenantMatch(CallerContext ctx, String resourceTenantId) {
        if (resourceTenantId == null || !ctx.tenantId().equals(resourceTenantId)) {
            throw error(Response.Status.FORBIDDEN, "Access denied for tenant boundary.");
        }
    }

    public void requireReservationAccess(CallerContext ctx, Reservation reservation) {
        requireTenantMatch(ctx, reservation.tenantId);
        if (ctx.isUser() && reservation.createdBy != null && !ctx.username().equals(reservation.createdBy)) {
            throw error(Response.Status.FORBIDDEN, "Customers can only access their own reservations.");
        }
    }

    public static WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(
            Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", message))
                .build()
        );
    }

    private static String headerValue(HttpHeaders headers, String name) {
        if (headers == null) {
            return null;
        }
        String raw = headers.getHeaderString(name);
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
