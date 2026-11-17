package id.ac.upnvj.gearrent.security;

/** Resolved authenticated actor, role, and tenant boundary for the current request. */
public record CallerContext(String username, String role, String tenantId) {

    public boolean isOperatorOrAdmin() {
        return "operator".equals(role) || "admin".equals(role);
    }

    public boolean isUser() {
        return "user".equals(role);
    }
}
