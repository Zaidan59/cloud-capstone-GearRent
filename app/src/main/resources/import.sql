-- Initial Tenants
INSERT INTO tenants (id, name, contact_email, active) VALUES ('tenant-a', 'Jakarta Camera Rental', 'ops@jakartarental.local', true) ON CONFLICT DO NOTHING;
INSERT INTO tenants (id, name, contact_email, active) VALUES ('tenant-b', 'Bandung Visual Gear', 'contact@bandungvisual.local', true) ON CONFLICT DO NOTHING;

-- Equipment Units for Tenant A (Jakarta)
INSERT INTO equipment_units (id, tenant_id, asset_code, name, category, daily_rate, condition_notes, status) 
VALUES (nextval('equipment_units_SEQ'), 'tenant-a', 'CAM-001', 'Sony Cinema Line FX3', 'Camera', 650000.0, 'Sensor bersih, firmware v5.0', 'AVAILABLE');

INSERT INTO equipment_units (id, tenant_id, asset_code, name, category, daily_rate, condition_notes, status) 
VALUES (nextval('equipment_units_SEQ'), 'tenant-a', 'LNS-001', 'Sony FE 24-70mm f/2.8 GM II', 'Lens', 350000.0, 'Optik mulus, include filter UV', 'AVAILABLE');

INSERT INTO equipment_units (id, tenant_id, asset_code, name, category, daily_rate, condition_notes, status) 
VALUES (nextval('equipment_units_SEQ'), 'tenant-a', 'LGT-001', 'Aputure Light Storm 300d II', 'Lighting', 300000.0, 'Include standard reflector & softbox', 'AVAILABLE');

-- Equipment Units for Tenant B (Bandung)
INSERT INTO equipment_units (id, tenant_id, asset_code, name, category, daily_rate, condition_notes, status) 
VALUES (nextval('equipment_units_SEQ'), 'tenant-b', 'CAM-002', 'Canon EOS R5 Mirrorless', 'Camera', 600000.0, 'Kondisi 98%, include 2 baterai LP-E6NH', 'AVAILABLE');

INSERT INTO equipment_units (id, tenant_id, asset_code, name, category, daily_rate, condition_notes, status) 
VALUES (nextval('equipment_units_SEQ'), 'tenant-b', 'STB-001', 'DJI RS 3 Pro Gimbal Stabilizer', 'Stabilizer', 250000.0, 'Motor normal, include briefcase handle', 'AVAILABLE');
