package id.ac.upnvj.gearrent;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class ObservabilityMetricsTest {

    @Test
    void prometheusMetricsIncludeHttpHistogramBucketsAndDatasourcePool() {
        given()
            .header("X-Tenant-Id", "tenant-a")
            .header("X-User-Role", "operator")
            .when().get("/api/equipments")
            .then().statusCode(200);

        given()
            .when().get("/q/metrics")
            .then()
            .statusCode(200)
            .body(containsString("http_server_requests_seconds_bucket"))
            .body(containsString("agroal_active_count"))
            .body(containsString("agroal_available_count"));
    }
}
