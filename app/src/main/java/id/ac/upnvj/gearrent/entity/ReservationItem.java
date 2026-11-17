package id.ac.upnvj.gearrent.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Individual equipment unit line item belonging to a multi-unit reservation. */
@Entity
@Table(name = "reservation_items")
public class ReservationItem extends PanacheEntity {

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    public Reservation reservation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    public EquipmentUnit equipment;

    @Column(name = "daily_rate_snapshot", nullable = false)
    public Double dailyRateSnapshot;

    @Column(name = "item_status", nullable = false, length = 30)
    public String itemStatus = "REQUESTED"; // REQUESTED, ALLOCATED, CHECKED_OUT, RETURNED, CANCELLED
}
