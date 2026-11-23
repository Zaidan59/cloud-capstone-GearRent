package id.ac.upnvj.gearrent;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.http.ContentType;
import io.smallrye.jwt.build.Jwt;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(OidcSecurityTest.OidcStrictProfile.class)
@QuarkusTestResource(OidcWiremockTestResource.class)
class OidcSecurityTest {

    public static class OidcStrictProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "quarkus.oidc.enabled", "true",
                "quarkus.keycloak.devservices.enabled", "false",
                "gearrent.auth.trust-headers", "false",
                "quarkus.oidc.auth-server-url", "${keycloak.url}/realms/quarkus",
                "smallrye.jwt.sign.key.location", "privateKey.jwk"
            );
        }
    }

    private static String jwtFor(String username, List<String> roles, List<String> groups) {
        return Jwt.preferredUserName(username)
            .issuer("https://server.example.com")
            .audience("https://server.example.com")
            .claim("realm_access", Map.of("roles", roles))
            .claim("groups", groups)
            .jws()
            .keyId("1")
            .sign("privateKey.jwk");
    }

    @Test
    void unauthenticatedAndSpoofedHeaderRequestsAreRejectedWithUnauthorized() {
        given()
            .when().get("/api/equipments")
            .then().statusCode(401);

        // Even with M2 headers attached, runtime OIDC mode rejects unauthenticated callers
        given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "admin")
            .when().get("/api/equipments")
            .then().statusCode(401);

        given()
            .header("Authorization", "Bearer invalid.jwt.token")
            .when().get("/api/equipments")
            .then().statusCode(401);
    }

    @Test
    void jwtEnforcesRoleTenantIsolationAndAntiSpoofing() {
        String userAToken = jwtFor("usera", List.of("user"), List.of("tenant-a"));
        String operatorAToken = jwtFor("operator", List.of("operator"), List.of("tenant-a"));
        String operatorBToken = jwtFor("operatorb", List.of("operator"), List.of("tenant-b"));
        String adminToken = jwtFor("appadmin", List.of("admin"), List.of("tenant-a", "tenant-b"));

        // Customer in tenant-a can list tenant-a equipments
        given()
            .auth().oauth2(userAToken)
            .when().get("/api/equipments")
            .then().statusCode(200)
            .body("tenantId", everyItem(is("tenant-a")));

        // Customer cannot spoof X-Tenant-Id header to cross tenant boundary
        given()
            .auth().oauth2(userAToken)
            .header("X-Tenant-Id", "tenant-b")
            .when().get("/api/equipments")
            .then().statusCode(403);

        // Customer cannot call operator/admin endpoints
        given()
            .auth().oauth2(userAToken)
            .when().get("/api/reports/summary")
            .then().statusCode(403);

        // Operator in tenant-a creates unit and verifies tenant-b operator cannot access it
        long unitA = given()
            .auth().oauth2(operatorAToken)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "assetCode", "OIDC-A-1",
                "name", "OIDC Verified Camera",
                "category", "Camera",
                "dailyRate", 450000.0
            ))
            .when().post("/api/equipments")
            .then().statusCode(201)
            .extract().jsonPath().getLong("id");

        given()
            .auth().oauth2(operatorBToken)
            .when().get("/api/equipments/{id}", unitA)
            .then().statusCode(403);

        // Multi-tenant admin can query both allowed tenants and is blocked on unlisted tenant-c
        given()
            .auth().oauth2(adminToken)
            .header("X-Tenant-Id", "tenant-b")
            .when().get("/api/equipments")
            .then().statusCode(200)
            .body("tenantId", everyItem(is("tenant-b")));

        given()
            .auth().oauth2(adminToken)
            .header("X-Tenant-Id", "tenant-c")
            .when().get("/api/equipments")
            .then().statusCode(403);
    }
}
