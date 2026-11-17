package id.ac.upnvj.gearrent.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Maintenance work order triggered manually or from damaged equipment return. */
@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord extends PanacheEntity {

    @Column(name = "tenant_id", nullable = false, length = 50)
    public String tenantId;

    @ManyToOne
    public EquipmentUnit equipment;

    @Column(name = "source_reservation_id")
    public Long sourceReservationId;

    @Column(name = "issue_description", nullable = false, length = 500)
    public String issueDescription;

    @Column(name = "reported_at", nullable = false)
    public LocalDateTime reportedAt = LocalDateTime.now();

    @Column(name = "status", nullable = false, length = 30)
    public String status = "OPEN"; // OPEN, RESOLVED

    @Column(name = "resolution_notes", length = 500)
    public String resolutionNotes;

    @Column(name = "resolved_at")
    public LocalDateTime resolvedAt;
}
