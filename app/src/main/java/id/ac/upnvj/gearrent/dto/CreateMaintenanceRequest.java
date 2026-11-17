package id.ac.upnvj.gearrent.dto;

/** Request payload to create a manual maintenance work order. */
public class CreateMaintenanceRequest {
    public Long equipmentId;
    public String issueDescription;
}
