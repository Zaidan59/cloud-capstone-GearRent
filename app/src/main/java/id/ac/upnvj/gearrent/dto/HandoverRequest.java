package id.ac.upnvj.gearrent.dto;

import java.util.List;

/** Request payload to record handover inspection for all items in a reservation. */
public class HandoverRequest {
    public Boolean accessoriesComplete = Boolean.TRUE;
    public String accessoriesChecklist;
    public String conditionNotes;
    public String photoObjectKey;
    public List<ItemInspectionInput> items;

    public static class ItemInspectionInput {
        public Long equipmentId;
        public Boolean accessoriesComplete;
        public String accessoriesChecklist;
        public String conditionNotes;
        public String photoObjectKey;
    }
}
