package id.ac.upnvj.gearrent;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class TenantIsolationAndRoleTest {

    @Test
    void missingContextHeadersRejectedWithUnauthorized() {
        given()
            .when().get("/api/equipments")
            .then().statusCode(401);

        given()
            .header("X-Tenant-Id", "tenant-a")
            .when().get("/api/reservations")
            .then().statusCode(401);
    }

    @Test
    void crossTenantAccessToUnitsReservationsInspectionsPhotosAndMaintenanceIsDenied() {
        long unitA = given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .body(Map.of("assetCode", "ISO-A-1", "name", "Tenant A Camera", "category", "Camera", "dailyRate", 500000.0))
            .when().post("/api/equipments")
            .then().statusCode(201)
            .extract().jsonPath().getLong("id");

        long resA = given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .header("X-User-Id", "usera")
            .body(Map.of(
                "customerName", "Customer A",
                "equipmentIds", List.of(unitA),
                "startDate", "2027-06-01",
                "endDate", "2027-06-03"
            ))
            .when().post("/api/reservations")
            .then().statusCode(201)
            .extract().jsonPath().getLong("id");

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .when().put("/api/reservations/{id}/approve", resA)
            .then().statusCode(200);

        long inspectionA = given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .body(Map.of("conditionNotes", "Check A"))
            .when().post("/api/reservations/{id}/handover", resA)
            .then().statusCode(200)
            .extract().jsonPath().getLong("inspections[0].id");

        given()
            .contentType("image/png")
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .queryParam("filename", "secret-a.png")
            .body(new byte[] {1, 2, 3})
            .when().post("/api/inspections/{id}/photo", inspectionA)
            .then().statusCode(200);

        // Tenant B operator cannot see or mutate Tenant A resources
        given()
            .header("X-Tenant-Id", "tenant-b")
            .header("X-User-Role", "operator")
            .when().get("/api/equipments")
            .then().statusCode(200)
            .body("assetCode", not(hasItem("ISO-A-1")));

        given()
            .header("X-Tenant-Id", "tenant-b")
            .header("X-User-Role", "operator")
            .when().get("/api/equipments/{id}", unitA)
            .then().statusCode(403);

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-b")
            .header("X-User-Role", "operator")
            .body(Map.of("name", "Hacked"))
            .when().put("/api/equipments/{id}", unitA)
            .then().statusCode(403);

        given()
            .header("X-Tenant-Id", "tenant-b")
            .header("X-User-Role", "operator")
            .when().get("/api/reservations/{id}", resA)
            .then().statusCode(403);

        given()
            .header("X-Tenant-Id", "tenant-b")
            .header("X-User-Role", "operator")
            .when().get("/api/inspections/{id}", inspectionA)
            .then().statusCode(403);

        given()
            .header("X-Tenant-Id", "tenant-b")
            .header("X-User-Role", "operator")
            .when().get("/api/inspections/{id}/photo", inspectionA)
            .then().statusCode(403);
    }

    @Test
    void customerRoleCannotRunOperatorActionsOrReadAnotherUsersReservation() {
        long unitA = given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .body(Map.of("assetCode", "ROLE-A-1", "name", "Role Test Lens", "category", "Lens", "dailyRate", 200000.0))
            .when().post("/api/equipments")
            .then().statusCode(201)
            .extract().jsonPath().getLong("id");

        // Customer cannot register equipment
        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .header("X-User-Id", "usera")
            .body(Map.of("assetCode", "UNAUTH-1", "name", "Fake", "category", "Camera", "dailyRate", 100000.0))
            .when().post("/api/equipments")
            .then().statusCode(403);

        // Customer creates own reservation
        long resId = given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .header("X-User-Id", "usera")
            .body(Map.of(
                "customerName", "User A",
                "equipmentIds", List.of(unitA),
                "startDate", "2027-07-01",
                "endDate", "2027-07-02"
            ))
            .when().post("/api/reservations")
            .then().statusCode(201)
            .extract().jsonPath().getLong("id");

        // Customer cannot approve, handover, return, create maintenance, or read reports
        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .header("X-User-Id", "usera")
            .when().put("/api/reservations/{id}/approve", resId)
            .then().statusCode(403);

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .header("X-User-Id", "usera")
            .body(Map.of("equipmentId", unitA, "issueDescription", "Unauthorized"))
            .when().post("/api/maintenance")
            .then().statusCode(403);

        given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .header("X-User-Id", "usera")
            .when().get("/api/reports/summary")
            .then().statusCode(403);

        // Another user in the same tenant cannot read usera's reservation
        given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .header("X-User-Id", "other-user")
            .when().get("/api/reservations/{id}", resId)
            .then().statusCode(403);
    }
}
