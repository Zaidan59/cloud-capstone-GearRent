package id.ac.upnvj.gearrent.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "reservations")
public class Reservation extends PanacheEntity {

    @Column(name = "tenant_id", nullable = false, length = 50)
    public String tenantId;

    @Column(name = "customer_name", nullable = false)
    public String customerName;

    @Column(name = "customer_email")
    public String customerEmail;

    @ManyToOne
    public EquipmentUnit equipment;

    @Column(name = "start_date", nullable = false)
    public LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    public LocalDate endDate;

    @Column(name = "total_cost")
    public Double totalCost;

    @Column(name = "status", nullable = false, length = 30)
    public String status = "PENDING"; // PENDING, APPROVED, RETURNED, CANCELLED

    @Column(name = "return_notes")
    public String returnNotes;
}
