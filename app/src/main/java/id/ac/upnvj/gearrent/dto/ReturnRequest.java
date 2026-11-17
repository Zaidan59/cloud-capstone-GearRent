package id.ac.upnvj.gearrent.dto;

import java.time.LocalDate;
import java.util.List;

/** Request payload to record equipment return, condition inspection, and optional damage flags. */
public class ReturnRequest {
    public LocalDate actualReturnDate;
    public Boolean hasDamage = Boolean.FALSE;
    public Boolean accessoriesComplete = Boolean.TRUE;
    public String accessoriesChecklist;
    public String conditionNotes;
    public String notes;
    public String photoObjectKey;
    public List<ItemReturnInput> items;

    public static class ItemReturnInput {
        public Long equipmentId;
        public Boolean hasDamage;
        public Boolean accessoriesComplete;
        public String accessoriesChecklist;
        public String conditionNotes;
        public String photoObjectKey;
    }
}
