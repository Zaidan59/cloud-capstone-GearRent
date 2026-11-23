package id.ac.upnvj.gearrent.security;

import id.ac.upnvj.gearrent.entity.Reservation;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Resolves and enforces caller role and tenant identity.
 * In M6 runtime mode (trust-headers=false), identity, role, and tenant group
 * are resolved from verified Keycloak OIDC JWT claims.
 * Header-trust mode remains available only for the M2 %test profile.
 */
@ApplicationScoped
public class AccessContextResolver {

    private static final List<String> ROLE_PRECEDENCE = List.of("admin", "operator", "user");
    private static final Set<String> ALLOWED_ROLES = Set.copyOf(ROLE_PRECEDENCE);

    @ConfigProperty(name = "gearrent.auth.trust-headers", defaultValue = "false")
    boolean trustHeaders;

    @Inject
    Instance<SecurityIdentity> identityInstance;

    @Inject
    Instance<JsonWebToken> jwtInstance;

    public CallerContext resolve(HttpHeaders headers) {
        if (trustHeaders) {
            return resolveFromHeaders(headers);
        }
        return resolveFromOidc(headers);
    }

    private CallerContext resolveFromOidc(HttpHeaders headers) {
        SecurityIdentity identity = identityInstance.isResolvable() ? identityInstance.get() : null;
        if (identity == null || identity.isAnonymous()) {
            throw error(Response.Status.UNAUTHORIZED, "Bearer token authentication required.");
        }

        JsonWebToken jwt = jwtInstance.isResolvable() ? jwtInstance.get() : null;
        String username = resolveUsername(identity, jwt);
        String role = resolveRole(identity, jwt);
        String tenantId = resolveTenant(headers, jwt);
        return new CallerContext(username, role, tenantId);
    }

    private static String resolveUsername(SecurityIdentity identity, JsonWebToken jwt) {
        if (jwt != null) {
            String preferred = jwt.getClaim("preferred_username");
            if (preferred != null && !preferred.isBlank()) {
                return preferred.trim();
            }
        }
        String principalName = identity.getPrincipal() != null ? identity.getPrincipal().getName() : null;
        if (principalName != null && !principalName.isBlank()) {
            return principalName.trim();
        }
        throw error(Response.Status.UNAUTHORIZED, "Authenticated token is missing a subject or username.");
    }

    private static String resolveRole(SecurityIdentity identity, JsonWebToken jwt) {
        Set<String> candidateRoles = new LinkedHashSet<>();
        if (identity.getRoles() != null) {
            for (String role : identity.getRoles()) {
                if (role != null) {
                    candidateRoles.add(role.toLowerCase(Locale.ROOT));
                }
            }
        }
        if (jwt != null) {
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof JsonObject obj && obj.containsKey("roles")) {
                for (JsonValue val : obj.getJsonArray("roles")) {
                    if (val instanceof JsonString str) {
                        candidateRoles.add(str.getString().toLowerCase(Locale.ROOT));
                    }
                }
            } else if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles) {
                for (Object role : roles) {
                    if (role != null) {
                        candidateRoles.add(role.toString().toLowerCase(Locale.ROOT));
                    }
                }
            }
        }

        for (String preferred : ROLE_PRECEDENCE) {
            if (candidateRoles.contains(preferred)) {
                return preferred;
            }
        }
        throw error(Response.Status.FORBIDDEN, "Token does not include an authorized GearRent role.");
    }

    private static String resolveTenant(HttpHeaders headers, JsonWebToken jwt) {
        Set<String> allowedTenants = extractTenantGroups(jwt);
        if (allowedTenants.isEmpty()) {
            throw error(Response.Status.FORBIDDEN, "Token does not include a tenant group.");
        }

        String requestedTenant = headerValue(headers, "X-Tenant-Id");
        if (requestedTenant != null) {
            if (!allowedTenants.contains(requestedTenant)) {
                throw error(Response.Status.FORBIDDEN, "Access denied for requested tenant boundary.");
            }
            return requestedTenant;
        }
        return allowedTenants.iterator().next();
    }

    private static Set<String> extractTenantGroups(JsonWebToken jwt) {
        Set<String> tenants = new LinkedHashSet<>();
        if (jwt == null) {
            return tenants;
        }
        Object rawGroups = jwt.getClaim("groups");
        if (rawGroups instanceof JsonArray array) {
            for (JsonValue item : array) {
                if (item instanceof JsonString str) {
                    addNormalizedGroup(tenants, str.getString());
                }
            }
        } else if (rawGroups instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    addNormalizedGroup(tenants, item.toString());
                }
            }
        } else if (jwt.getGroups() != null) {
            for (String group : jwt.getGroups()) {
                addNormalizedGroup(tenants, group);
            }
        }
        return tenants;
    }

    private static void addNormalizedGroup(Set<String> tenants, String rawGroup) {
        if (rawGroup == null) {
            return;
        }
        String normalized = rawGroup.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.isBlank()) {
            tenants.add(normalized);
        }
    }

    private static CallerContext resolveFromHeaders(HttpHeaders headers) {
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
