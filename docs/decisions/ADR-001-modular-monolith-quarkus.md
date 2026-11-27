# ADR-001: Modular Monolith Quarkus vs Microservices

- **Status:** Accepted
- **Tanggal:** 2026-11-26
- **Owner:** Tim Kelompok 2 (A02) — GearRent
- **Milestone:** M1 (Architecture Baseline), M2 (Application MVP)

## Context
GearRent membutuhkan layanan backend untuk mengelola katalog peralatan multimedia, reservasi multi-unit, inspeksi serah-terima/pengembalian beserta foto, work order maintenance, dan ringkasan laporan per tenant. Proyek dikerjakan oleh tim 4 mahasiswa dan dijalankan pada lingkungan lab laptop lokal (`k3d` dengan batas resource terbatas, minimal 8–16 GB RAM) sebelum dipetakan ke VM CloudStack.

## Options Considered
1. **Microservices terpisah per domain** (`equipment-service`, `reservation-service`, `inspection-service`, `maintenance-service` dengan database masing-masing atau message broker):
   - *Pro:* Deployment independen per domain.
   - *Kontra:* Menambah overhead jaringan antar-service, distributed transaction/saga untuk alokasi multi-unit, duplikasi konfigurasi CI/CD, dan konsumsi RAM/CPU berlebih di `k3d`.
2. **Modular Monolith dengan Quarkus `3.40.1.1` (Dipilih):**
   - *Pro:* Transaksi ACID lokal saat mengunci banyak unit sekaligus (`ReservationService`), satu pipeline CI/GitOps yang terstruktur, startup cepat, dan konsumsi memori terkendali (`~242–255 MiB` saat beban 8 worker).
   - *Kontra:* Seluruh modul di-deploy dalam satu artifact/pod, sehingga scaling hanya bisa dilakukan per replika aplikasi secara keseluruhan.

## Decision
Mengimplementasikan aplikasi GearRent sebagai **Modular Monolith** menggunakan **Quarkus `3.40.1.1`** (Java `21` compiler release, berjalan pada runtime JVM) dengan pemisahan package yang tegas di `app/src/main/java/id/ac/upnvj/gearrent/` (`entity/`, `dto/`, `resource/`, `service/`, `security/`, `observability/`).

## Consequences & Limitations
- **Positif:** Alur bisnis lengkap (21 endpoint `/api/*` dan 6 entitas) dapat diuji secara deterministik dalam satu suite test (`./mvnw -B verify`), dan footprint container sesuai dengan target resource lab (`100m/192Mi` requests, `500m/512Mi` limits).
- **Limitasi:** Belum mendukung scaling independen khusus modul upload/inspeksi secara terpisah dari modul reservasi; pemecahan ke service terpisah hanya dilakukan bila metrik bottleneck menuntutnya di masa depan.

## Traceability / Evidence
- `app/pom.xml`, `app/Dockerfile`, `app/src/main/java/id/ac/upnvj/gearrent/`
- Panduan arsitektur capstone (`guides/Cloud_Computing_Final_Capstone_Project_Guideline.docx`, Bagian 6: *Avoid over-engineering; modular monolith is acceptable and often preferred*).
