package id.ac.upnvj.gearrent.service;

import id.ac.upnvj.gearrent.entity.EquipmentUnit;
import id.ac.upnvj.gearrent.entity.Tenant;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

/** Seeds baseline sample tenants and equipment once on an empty database without wiping existing data. */
@ApplicationScoped
public class SeedDataInitializer {

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (Tenant.count() > 0) {
            return;
        }

        createTenant("tenant-a", "Jakarta Camera Rental", "ops@jakartarental.local");
        createTenant("tenant-b", "Bandung Visual Gear", "contact@bandungvisual.local");

        createUnit("tenant-a", "CAM-001", "Sony Cinema Line FX3", "Camera", 650000.0, "Sensor bersih, firmware v5.0");
        createUnit("tenant-a", "LNS-001", "Sony FE 24-70mm f/2.8 GM II", "Lens", 350000.0, "Optik mulus, include filter UV");
        createUnit("tenant-a", "LGT-001", "Aputure Light Storm 300d II", "Lighting", 300000.0, "Include standard reflector & softbox");
        createUnit("tenant-b", "CAM-002", "Canon EOS R5 Mirrorless", "Camera", 600000.0, "Kondisi 98%, include 2 baterai LP-E6NH");
        createUnit("tenant-b", "STB-001", "DJI RS 3 Pro Gimbal Stabilizer", "Stabilizer", 250000.0, "Motor normal, include briefcase handle");
    }

    private static void createTenant(String id, String name, String email) {
        Tenant t = new Tenant();
        t.id = id;
        t.name = name;
        t.contactEmail = email;
        t.active = true;
        t.persist();
    }

    private static void createUnit(
        String tenantId, String assetCode, String name, String category, Double rate, String notes
    ) {
        EquipmentUnit u = new EquipmentUnit();
        u.tenantId = tenantId;
        u.assetCode = assetCode;
        u.name = name;
        u.category = category;
        u.dailyRate = rate;
        u.conditionNotes = notes;
        u.status = "AVAILABLE";
        u.persist();
    }
}
