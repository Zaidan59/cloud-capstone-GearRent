# ADR-002: Multi-Tenancy `tenant_id` Discriminator + Ascending `PESSIMISTIC_WRITE` Locking

- **Status:** Accepted
- **Tanggal:** 2026-11-26
- **Owner:** Tim Kelompok 2 (A02) — GearRent
- **Milestone:** M1 (Architecture Baseline), M2 (Application MVP)

## Context
GearRent melayani lebih dari satu penyewa (`tenant-a` Jakarta Camera Rental dan `tenant-b` Bandung Visual Gear). Sistem harus menjamin dua hal kritis:
1. Data unit, reservasi, inspeksi, foto, dan maintenance milik satu tenant tidak boleh bocor atau diubah oleh tenant lain.
2. Persetujuan (*approve*) reservasi multi-unit yang dilakukan bersamaan tidak boleh menyebabkan *double booking* atau *deadlock* di database.

## Options Considered
1. **Database terpisah atau Schema terpisah per tenant:**
   - *Pro:* Isolasi fisik lebih kuat di level database.
   - *Kontra:* Memperumit migrasi skema Hibernate (`database.generation=update`), connection pool Agroal per tenant, dan operasional backup/restore untuk skala lab.
2. **Shared PostgreSQL dengan kolom diskriminator `tenant_id` + aplikasi enforcement (Dipilih):**
   - *Pro:* Satu skema PostgreSQL `17-alpine`, satu connection pool terpantau di Grafana (`agroal_*`), dan mudah diaudit bersama isolasi JWT (`ADR-005`).
   - *Kontra:* Keamanan isolasi bergantung pada disiplin query `tenantId` dan validasi di layer `AccessContextResolver` + `ReservationService`.
3. **Optimistic locking saja (`@Version`) tanpa pessimistic lock saat approval:**
   - *Pro:* Tidak menahan row lock di DB.
   - *Kontra:* Rentan memicu retry/conflict yang membingungkan saat dua operator menyetujui reservasi berbeda dengan kombinasi unit yang beririsan.

## Decision
1. Menggunakan **shared PostgreSQL** dengan kolom `tenant_id` pada seluruh entitas bisnis dan mewajibkan `AccessContextResolver.requireTenantMatch` / `requireReservationAccess` di setiap endpoint.
2. Pada `ReservationService.approveReservation`, seluruh `equipmentId` dalam reservasi diurutkan **ascending (`sorted()`)** sebelum mengambil `LockModeType.PESSIMISTIC_WRITE`, lalu dicek overlap tanggalnya terhadap status `APPROVED` dan `CHECKED_OUT` (sambil mempertahankan `@Version` pada `EquipmentUnit` dengan `columnDefinition = "bigint default 0"`).

## Consequences & Limitations
- **Positif:** Mencegah *double booking* dan *deadlock* antar-transaksi multi-unit yang saling bersilangan; seluruh operasi CRUD tenant dan isolasi teruji otomatis pada `AvailabilityAndConcurrencyTest` (3 test) dan `TenantIsolationAndRoleTest` (3 test).
- **Limitasi:** Isolasi belum memakai PostgreSQL *Row-Level Security (RLS)* di level mesin DB; jika tenant berkembang jauh lebih banyak atau ada regulasi pemisahan fisik, migrasi ke RLS atau schema-per-tenant diperlukan.

## Traceability / Evidence
- `app/src/main/java/id/ac/upnvj/gearrent/service/ReservationService.java`
- `app/src/main/java/id/ac/upnvj/gearrent/entity/EquipmentUnit.java`
- `app/src/main/java/id/ac/upnvj/gearrent/security/AccessContextResolver.java`
- `app/src/test/java/id/ac/upnvj/gearrent/AvailabilityAndConcurrencyTest.java`
- `app/src/test/java/id/ac/upnvj/gearrent/TenantIsolationAndRoleTest.java`
