package id.ac.upnvj.gearrent;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class AvailabilityAndConcurrencyTest {

    @Test
    void sequentialBackToBackPeriodAllowedWhileOverlappingPeriodRejected() {
        long unitId = createUnit("tenant-a", "BND-CAM-1", "Blackmagic 6K", "Camera", 450000.0);

        // Approve 2027-03-01 -> 2027-03-05
        long res1 = createReservation("tenant-a", "usera", "Tim A", List.of(unitId), "2027-03-01", "2027-03-05");
        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .when().put("/api/reservations/{id}/approve", res1)
            .then().statusCode(200);

        // Overlapping search excludes BND-CAM-1
        given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .queryParam("startDate", "2027-03-03")
            .queryParam("endDate", "2027-03-07")
            .when().get("/api/equipments/available")
            .then().statusCode(200)
            .body("assetCode", not(hasItem("BND-CAM-1")));

        // Overlapping booking rejected (409)
        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .header("X-User-Id", "usera")
            .body(Map.of(
                "customerName", "Tim Bentrok",
                "equipmentIds", List.of(unitId),
                "startDate", "2027-03-03",
                "endDate", "2027-03-07"
            ))
            .when().post("/api/reservations")
            .then().statusCode(409);

        // Consecutive period starting right at previous endDate (2027-03-05 -> 2027-03-08) IS allowed
        given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .queryParam("startDate", "2027-03-05")
            .queryParam("endDate", "2027-03-08")
            .when().get("/api/equipments/available")
            .then().statusCode(200)
            .body("assetCode", hasItem("BND-CAM-1"));

        long resConsecutive = createReservation("tenant-a", "usera", "Tim B", List.of(unitId), "2027-03-05", "2027-03-08");
        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .when().put("/api/reservations/{id}/approve", resConsecutive)
            .then().statusCode(200);
    }

    @Test
    void concurrentApprovalsForSameUnitsAndDatesAllowOnlyOneWinner() throws Exception {
        long unitA = createUnit("tenant-a", "CON-UNIT-1", "Aputure 600d", "Lighting", 400000.0);
        long unitB = createUnit("tenant-a", "CON-UNIT-2", "Sennheiser MKH416", "Audio", 200000.0);

        int competitors = 8;
        List<Long> reservationIds = new ArrayList<>();
        for (int i = 0; i < competitors; i++) {
            reservationIds.add(createReservation(
                "tenant-a", "user" + i, "Crew " + i, List.of(unitA, unitB), "2027-04-10", "2027-04-12"
            ));
        }

        ExecutorService pool = Executors.newFixedThreadPool(competitors);
        CountDownLatch ready = new CountDownLatch(competitors);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();

        for (Long resId : reservationIds) {
            Callable<Integer> task = () -> {
                ready.countDown();
                go.await();
                return given()
                    .contentType(ContentType.JSON)
                    .header("X-Tenant-Id", "tenant-a")
                    .header("X-User-Role", "operator")
                    .header("X-User-Id", "operator1")
                    .when().put("/api/reservations/{id}/approve", resId)
                    .then().extract().statusCode();
            };
            results.add(pool.submit(task));
        }

        ready.await();
        go.countDown();

        int successes = 0;
        int conflicts = 0;
        for (Future<Integer> future : results) {
            int code = future.get();
            if (code == 200) successes++;
            else if (code == 409) conflicts++;
        }
        pool.shutdown();

        assertEquals(1, successes, "Exactly one concurrent approval must succeed");
        assertEquals(competitors - 1, conflicts, "All losing concurrent approvals must return 409 Conflict");
    }

    @Test
    void multiUnitApprovalIsAtomicWhenOneUnitAlreadyBooked() {
        long freeUnit = createUnit("tenant-a", "ATM-FREE-1", "Tripod Manfrotto", "Accessory", 100000.0);
        long busyUnit = createUnit("tenant-a", "ATM-BUSY-1", "Sony A7S III", "Camera", 550000.0);

        // Create two pending reservations before approval: one single-unit on busyUnit, one multi-unit on both
        long singleRes = createReservation("tenant-a", "usera", "Solo", List.of(busyUnit), "2027-05-01", "2027-05-03");
        long multiRes = createReservation("tenant-a", "usera", "Multi", List.of(freeUnit, busyUnit), "2027-05-01", "2027-05-03");

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .when().put("/api/reservations/{id}/approve", singleRes)
            .then().statusCode(200);

        // Multi-unit approval must fail atomically (409) and NOT block freeUnit
        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .when().put("/api/reservations/{id}/approve", multiRes)
            .then().statusCode(409);

        given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "user")
            .queryParam("startDate", "2027-05-01")
            .queryParam("endDate", "2027-05-03")
            .when().get("/api/equipments/available")
            .then().statusCode(200)
            .body("assetCode", hasItem("ATM-FREE-1"))
            .body("assetCode", not(hasItem("ATM-BUSY-1")));
    }

    private static long createUnit(String tenant, String code, String name, String category, double rate) {
        return given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", tenant)
            .header("X-User-Role", "operator")
            .body(Map.of("assetCode", code, "name", name, "category", category, "dailyRate", rate))
            .when().post("/api/equipments")
            .then().statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    private static long createReservation(
        String tenant, String userId, String customer, List<Long> unitIds, String start, String end
    ) {
        return given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", tenant)
            .header("X-User-Role", "user")
            .header("X-User-Id", userId)
            .body(Map.of(
                "customerName", customer,
                "equipmentIds", unitIds,
                "startDate", start,
                "endDate", end
            ))
            .when().post("/api/reservations")
            .then().statusCode(201)
            .extract().jsonPath().getLong("id");
    }
}
