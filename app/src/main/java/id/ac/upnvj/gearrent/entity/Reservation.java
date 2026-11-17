package id.ac.upnvj.gearrent.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Multi-unit rental reservation with atomic allocation and return tracking. */
@Entity
@Table(name = "reservations")
public class Reservation extends PanacheEntity {

    @Column(name = "tenant_id", nullable = false, length = 50)
    public String tenantId;

    @Column(name = "customer_name", nullable = false)
    public String customerName;

    @Column(name = "customer_email")
    public String customerEmail;

    @Column(name = "created_by", length = 100)
    public String createdBy;

    @Column(name = "start_date", nullable = false)
    public LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    public LocalDate endDate;

    @Column(name = "actual_return_date")
    public LocalDate actualReturnDate;

    @Column(name = "late_return")
    public Boolean lateReturn = Boolean.FALSE;

    @Column(name = "total_cost")
    public Double totalCost;

    @Column(name = "status", nullable = false, length = 30)
    public String status = "PENDING"; // PENDING, APPROVED, CHECKED_OUT, RETURNED, REJECTED, CANCELLED

    @Column(name = "notes", length = 500)
    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    public List<ReservationItem> items = new ArrayList<>();
}
