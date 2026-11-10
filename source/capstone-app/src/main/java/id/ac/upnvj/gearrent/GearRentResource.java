package id.ac.upnvj.gearrent;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/")
public class GearRentResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> index() {
        return Map.of(
            "app", "GearRent",
            "status", "RUNNING",
            "message", "GearRent Cloud-Native Multimedia Equipment Rental API"
        );
    }
}
