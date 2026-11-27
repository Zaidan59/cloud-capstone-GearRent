# 03 — Application MVP, Persistence & Multi-Tenancy (M2)

## Evidence Metadata

| Field | Detail |
| :--- | :--- |
| **Scope** | API inti GearRent (katalog alat, cek ketersediaan, reservasi multi-unit, serah-terima/inspeksi, pengembalian, work order maintenance, laporan), persistensi PostgreSQL, dan tes otomatis |
| **Owner** | Kelompok 2 (Kevin, Zaidan, Raihan, Justin) |
| **Source / Version** | Ringkasan visual [`03_evidence.png`](03_evidence.png) (baseline `0e552aa`); kode sumber terbaru `app/` pada commit `d66bdb6` (Quarkus `3.40.1.1`) |
| **Timestamp** | Gambar baseline: 12 November 2026; verifikasi `./mvnw -B verify` terbaru: 27 November 2026 |
| **Method** | Pengujian end-to-end REST API + PostgreSQL (`SeedDataInitializer` 2 tenant / 5 unit awal) serta eksekusi suite JUnit 5 / RestAssured (`cd app && ./mvnw -B verify`) |
| **Observation** | Suite otomatis terbaru lulus **11/11 test** (`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`), termasuk `ReservationWorkflowTest` (1), `AvailabilityAndConcurrencyTest` (3 — validasi tanggal, anti *double booking*, dan ascending `PESSIMISTIC_WRITE`), `TenantIsolationAndRoleTest` (3), `ObservabilityMetricsTest` (1), dan `OidcSecurityTest` (2). Skema memakai `quarkus.hibernate-orm.database.generation=update` sehingga data bertahan lintas restart pod |
| **Interpretation** | Alur bisnis utama dan kontrol konsistensi alokasi multi-unit bekerja deterministik dan tidak bergantung pada memori aplikasi. Isolasi tenant dan batas peran terverifikasi di level layanan |
| **Limitation** | 1) Suite `./mvnw -B verify` memakai H2 in-memory (`%test`) dan `OidcWiremockTestResource` agar cepat dan independen; 2) Gambar [`03_evidence.png`](03_evidence.png) merupakan kartu ringkasan HTML dari baseline awal (`0e552aa`) sebelum migrasi penuh ke Keycloak OIDC M6 |
| **Decision Supported** | Membuktikan kelayakan MVP M2 sebelum kompleksitas Kubernetes/CI/Security ditambahkan, serta mengonfirmasi `PESSIMISTIC_WRITE` terurut aman dari *deadlock* (`ADR-002`) |

## Berkas & Tindak Lanjut
- Tersedia: [`03_evidence.png`](03_evidence.png)
- Disarankan tambah: screenshot aktual Swagger UI (`/q/swagger-ui/`) dan output `curl` / `psql` terbaru dari cluster pasca M6 OIDC agar menggantikan/melengkapi kartu visual awal.
