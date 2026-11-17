package id.ac.upnvj.gearrent;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@QuarkusTest
class ReservationWorkflowTest {

    @Test
    void fullMultiUnitRentalHandoverPhotoReturnAndMaintenanceWorkflow() {
        long unit1Id = createUnit("tenant-a", "WF-CAM-1", "Sony FX6", "Camera", 500000.0);
        long unit2Id = createUnit("tenant-a", "WF-LNS-1", "Sony 70-200mm GM", "Lens", 250000.0);

        // User checks available units for 2027-01-10 to 2027-01-13 (3 days)
        given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .header("X-User-Id", "usera")
            .queryParam("startDate", "2027-01-10")
            .queryParam("endDate", "2027-01-13")
            .when().get("/api/equipments/available")
            .then()
            .statusCode(200)
            .body("assetCode", hasItem("WF-CAM-1"))
            .body("assetCode", hasItem("WF-LNS-1"));

        // User creates multi-unit reservation: (500k + 250k) * 3 days = 2,250,000
        long reservationId = given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .header("X-User-Id", "usera")
            .body(Map.of(
                "customerName", "Budi Studio",
                "customerEmail", "budi@example.local",
                "equipmentIds", List.of(unit1Id, unit2Id),
                "startDate", "2027-01-10",
                "endDate", "2027-01-13",
                "notes", "Produksi dokumenter"
            ))
            .when().post("/api/reservations")
            .then()
            .statusCode(201)
            .body("status", equalTo("PENDING"))
            .body("totalCost", equalTo(2250000.0f))
            .body("items.size()", equalTo(2))
            .extract().jsonPath().getLong("id");

        // Operator approves reservation
        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .header("X-User-Id", "operator1")
            .when().put("/api/reservations/{id}/approve", reservationId)
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"));

        // Operator performs handover and inspects accessories
        long handoverInspectionId = given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .header("X-User-Id", "operator1")
            .body(Map.of(
                "accessoriesComplete", true,
                "accessoriesChecklist", "Body, 2 batteries, charger, lens cap",
                "conditionNotes", "Semua mulus"
            ))
            .when().post("/api/reservations/{id}/handover", reservationId)
            .then()
            .statusCode(200)
            .body("reservation.status", equalTo("CHECKED_OUT"))
            .body("inspections.size()", equalTo(2))
            .extract().jsonPath().getLong("inspections[0].id");

        // Upload & download private inspection photo
        byte[] samplePng = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};
        given()
            .contentType("image/png")
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .header("X-User-Id", "operator1")
            .queryParam("filename", "handover-front.png")
            .body(samplePng)
            .when().post("/api/inspections/{id}/photo", handoverInspectionId)
            .then()
            .statusCode(200)
            .body("photoObjectKey", notNullValue())
            .body("photoSizeBytes", equalTo(samplePng.length));

        byte[] downloaded = given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .header("X-User-Id", "usera")
            .when().get("/api/inspections/{id}/photo", handoverInspectionId)
            .then()
            .statusCode(200)
            .extract().asByteArray();
        assertArrayEquals(samplePng, downloaded);

        // Operator records late return (2027-01-14 > 2027-01-13) with damage ONLY on unit1
        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .header("X-User-Id", "operator1")
            .body(Map.of(
                "actualReturnDate", "2027-01-14",
                "items", List.of(
                    Map.of(
                        "equipmentId", unit1Id,
                        "hasDamage", true,
                        "accessoriesComplete", false,
                        "conditionNotes", "ND filter ring retak"
                    ),
                    Map.of(
                        "equipmentId", unit2Id,
                        "hasDamage", false,
                        "accessoriesComplete", true,
                        "conditionNotes", "Lensa normal"
                    )
                )
            ))
            .when().post("/api/reservations/{id}/return", reservationId)
            .then()
            .statusCode(200)
            .body("reservation.status", equalTo("RETURNED"))
            .body("lateReturn", equalTo(true))
            .body("maintenanceOrdersCreated", equalTo(1));

        // Verify unit1 is MAINTENANCE and unit2 is AVAILABLE
        given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .when().get("/api/equipments/{id}", unit1Id)
            .then().statusCode(200).body("status", equalTo("MAINTENANCE"));

        given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .when().get("/api/equipments/{id}", unit2Id)
            .then().statusCode(200).body("status", equalTo("AVAILABLE"));

        // Find OPEN maintenance record for unit1 and resolve it
        Number maintenanceId = given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .when().get("/api/maintenance")
            .then()
            .statusCode(200)
            .extract().jsonPath().get("find { it.equipment.id == " + unit1Id + " && it.status == 'OPEN' }.id");

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .body(Map.of("notes", "Ring ND diganti dan dikalibrasi"))
            .when().put("/api/maintenance/{id}/resolve", maintenanceId.longValue())
            .then()
            .statusCode(200)
            .body("status", equalTo("RESOLVED"));

        given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .when().get("/api/equipments/{id}", unit1Id)
            .then().statusCode(200).body("status", equalTo("AVAILABLE"));

        // Report summary includes lateReturns
        given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .when().get("/api/reports/summary")
            .then()
            .statusCode(200)
            .body("tenantId", equalTo("tenant-a"))
            .body("lateReturns", greaterThanOrEqualTo(1));
    }

    private static long createUnit(String tenant, String code, String name, String category, double rate) {
        return given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", tenant)
            .header("X-User-Role", "operator")
            .body(Map.of(
                "assetCode", code,
                "name", name,
                "category", category,
                "dailyRate", rate
            ))
            .when().post("/api/equipments")
            .then().statusCode(201)
            .extract().jsonPath().getLong("id");
    }
}
