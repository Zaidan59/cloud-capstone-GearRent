# ADR-003: Abstraksi `PhotoStorageService` dengan `LocalPhotoStorageService` Sementara

- **Status:** Accepted (Interim untuk Lab Lokal)
- **Tanggal:** 2026-11-26
- **Owner:** Tim Kelompok 2 (A02) — GearRent
- **Milestone:** M1 (Architecture Baseline), M2 (Application MVP), M7 (Reliability & Recovery)

## Context
Alur inspeksi serah-terima dan pengembalian pada GearRent mendukung unggah dan unduh foto kondisi alat (`POST/GET /api/inspections/{id}/photo`). Foto tidak boleh dibuka publik tanpa otorisasi tenant/kepemilikan, dan infrastruktur lab dasar belum menyertakan layanan object storage (MinIO/S3) secara default.

## Options Considered
1. **Menyimpan byte foto langsung sebagai `BLOB` di PostgreSQL:**
   - *Pro:* Otomatis ikut backup `pg_dump`.
   - *Kontra:* Membebani ukuran tabel, memori JVM, dan waktu backup/restore database relasional.
2. **Langsung mewajibkan container MinIO sejak M2:**
   - *Pro:* Persistensi object storage penuh.
   - *Kontra:* Menambah komponen infrastruktur dan konfigurasi credential tambahan di luar baseline common package.
3. **Mendefinisikan interface `PhotoStorageService` dengan implementasi awal `LocalPhotoStorageService` (Dipilih):**
   - *Pro:* Memisahkan kontrak penyimpanan dari `InspectionResource`; foto tetap dilayani di balik otorisasi backend (`AccessContextResolver`) dengan path-traversal guard, serta metadata `photoBucket`, `photoObjectKey`, `photoContentType`, dan `photoSizeBytes` tetap disimpan di PostgreSQL.
   - *Kontra:* Direktori default `${java.io.tmpdir}/gearrent-photos` bersifat *ephemeral* di dalam container pod.

## Decision
Menerapkan interface `PhotoStorageService` dan implementasi `LocalPhotoStorageService` yang menyimpan file pada `${java.io.tmpdir}/gearrent-photos` (dengan normalisasi path untuk mencegah *path traversal*), sementara metadata bucket/key disimpan di entitas `InspectionRecord`.

## Consequences & Limitations
- **Positif:** Upload/download foto berjalan mulus di `ReservationWorkflowTest` dan `TenantIsolationAndRoleTest` (terbukti tenant lain ditolak `403` saat mengakses foto inspeksi), tanpa menambah dependency/container berat.
- **Limitasi (Diketahui & Dicatat untuk M7/M8):** Ketika pod `capstone-app` terhapus/recreated (`kubectl delete pod`) atau di-scale > 1 replika, file fisik foto di `/tmp` container akan hilang meskipun metadata di PostgreSQL selamat.
- **Tindak Lanjut:** Membuat implementasi `S3PhotoStorageService` (MinIO/S3) atau me-mount Kubernetes PVC pada deployment produksi/CloudStack tanpa perlu mengubah controller `InspectionResource`.

## Traceability / Evidence
- `app/src/main/java/id/ac/upnvj/gearrent/service/PhotoStorageService.java`
- `app/src/main/java/id/ac/upnvj/gearrent/service/LocalPhotoStorageService.java`
- `app/src/main/java/id/ac/upnvj/gearrent/resource/InspectionResource.java`
- `app/src/main/resources/application.properties` (`gearrent.storage.*`)
