package id.ac.upnvj.gearrent.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Condition & accessory inspection record captured at handover or return, with object-storage metadata. */
@Entity
@Table(name = "inspection_records")
public class InspectionRecord extends PanacheEntity {

    @Column(name = "tenant_id", nullable = false, length = 50)
    public String tenantId;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    public Reservation reservation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    public EquipmentUnit equipment;

    @Column(name = "stage", nullable = false, length = 20)
    public String stage; // HANDOVER, RETURN

    @Column(name = "accessories_complete", nullable = false)
    public Boolean accessoriesComplete = Boolean.TRUE;

    @Column(name = "accessories_checklist", length = 500)
    public String accessoriesChecklist;

    @Column(name = "condition_notes", length = 500)
    public String conditionNotes;

    @Column(name = "has_damage", nullable = false)
    public Boolean hasDamage = Boolean.FALSE;

    @Column(name = "photo_object_key", length = 255)
    public String photoObjectKey;

    @Column(name = "photo_bucket", length = 100)
    public String photoBucket;

    @Column(name = "photo_content_type", length = 100)
    public String photoContentType;

    @Column(name = "photo_size_bytes")
    public Long photoSizeBytes;

    @Column(name = "inspected_by", length = 100)
    public String inspectedBy;

    @Column(name = "inspected_at", nullable = false)
    public LocalDateTime inspectedAt = LocalDateTime.now();

    @JsonProperty("reservationId")
    public Long getReservationId() {
        return reservation != null ? reservation.id : null;
    }
}
