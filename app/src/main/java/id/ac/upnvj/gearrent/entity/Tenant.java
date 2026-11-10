package id.ac.upnvj.gearrent.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenants")
public class Tenant extends PanacheEntityBase {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    public String id;

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "contact_email")
    public String contactEmail;

    @Column(name = "active", nullable = false)
    public boolean active = true;
}
