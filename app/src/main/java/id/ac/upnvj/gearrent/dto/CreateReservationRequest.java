package id.ac.upnvj.gearrent.dto;

import java.time.LocalDate;
import java.util.List;

/** Request payload to create a multi-unit rental reservation. */
public class CreateReservationRequest {
    public String customerName;
    public String customerEmail;
    public List<Long> equipmentIds;
    public LocalDate startDate;
    public LocalDate endDate;
    public String notes;
}
