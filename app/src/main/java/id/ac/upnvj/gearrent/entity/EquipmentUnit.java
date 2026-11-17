package id.ac.upnvj.gearrent.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/** Physical multimedia asset owned by a rental tenant. */
@Entity
@Table(
    name = "equipment_units",
    uniqueConstraints = @UniqueConstraint(name = "uk_equipment_tenant_asset", columnNames = {"tenant_id", "asset_code"})
)
public class EquipmentUnit extends PanacheEntity {

    @Column(name = "tenant_id", nullable = false, length = 50)
    public String tenantId;

    @Column(name = "asset_code", nullable = false, length = 50)
    public String assetCode;

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "category", nullable = false, length = 50)
    public String category;

    @Column(name = "daily_rate", nullable = false)
    public Double dailyRate;

    @Column(name = "condition_notes", length = 500)
    public String conditionNotes;

    @Column(name = "status", nullable = false, length = 30)
    public String status = "AVAILABLE"; // AVAILABLE, RENTED, MAINTENANCE

    @Version
    public Long version = 0L;
}
