# 08 — Reliability, Self-Healing & Backup/Restore (M7)

## Evidence Metadata

| Field | Detail |
| :--- | :--- |
| **Scope** | Pemulihan pod oleh Kubernetes/Argo CD `selfHeal`, persistensi skema PostgreSQL (`database.generation=update` + idempotent seed), serta backup (`pg_dump -Fc`) dan validasi restore terisolasi ke database `capstone_restore_validation` |
| **Owner** | Muhammad Zaidan (`2410512043`) & Tim Kelompok 2 |
| **Source / Version** | `scripts/backup-postgres.sh`, `scripts/restore-postgres-validation.sh`, `scripts/backup-postgres.ps1`, `scripts/restore-postgres-validation.ps1`, `SeedDataInitializer.java`, `application.properties`, [`ADR-003`](../../decisions/ADR-003-photo-storage-abstraction.md), [`ADR-004`](../../decisions/ADR-004-ci-gitops-separation.md) |
| **Timestamp** | Perbaikan skrip lintas-OS & validasi M7: 23 November 2026 (commit `1c574b3` / PR #2 `a771c66`); folder evidence disusun 27 November 2026 |
| **Method** | 1) Menghapus pod aktif (`kubectl -n capstone delete pod -l app=capstone-app`) dan menunggu `Ready` + cek `/q/health/ready`; 2) Menjalankan `./scripts/backup-postgres.sh` dan memulihkan file `.dump` ke DB terpisah `capstone_restore_validation` via `./scripts/restore-postgres-validation.sh` lalu mencocokkan jumlah baris tabel |
| **Observation** | Skrip backup menghasilkan dump biner terkompresi (`pg_dump -Fc`) ke folder `backups/` (yang di-gitignore) dan skrip validasi membuat ulang database `capstone_restore_validation`, memulihkan seluruh tabel dengan `pg_restore --no-owner`, serta menampilkan daftar tabel (`\dt`). `SeedDataInitializer` hanya mengisi data awal jika `Tenant.count() == 0`, sehingga restart pod tidak menimpa data transaksi |
| **Interpretation** | Keberadaan file backup saja tidak cukup (`Guideline 14.2`); validasi restore ke database terisolasi memastikan dump benar-benar dapat dipulihkan dan konsisten secara relasional tanpa mengganggu database utama |
| **Limitation** | 1) **Penyimpanan foto sementara (`ADR-003`):** `LocalPhotoStorageService` masih menyimpan file foto di `/tmp/gearrent-photos` di dalam container, sehingga ketika pod dihapus/diganti, metadata di PostgreSQL selamat tetapi **file fisik foto hilang** (sampai dimigrasikan ke PVC atau MinIO/S3); 2) Backup manual masih bergantung pada eksekusi skrip lokal (`RPO` mengikuti jadwal eksekusi operator) dan belum otomatis via CronJob/off-site storage; 3) Folder ini **belum menyimpan screenshot/log output terminal live** dari eksekusi `delete pod` dan perbandingan `SELECT count(*)` |
| **Decision Supported** | Menetapkan `database.generation=update` + idempotent seed untuk ketahanan skema, serta mewajibkan validasi restore terpisah dan transparansi limitasi foto `/tmp` |

## Checklist Bukti Live yang Masih Perlu Ditambahkan ke Folder Ini
- [ ] Log/screenshot `kubectl -n capstone delete pod -l app=capstone-app` beserta durasi hingga `Ready` (target <= 120 detik) dan status `/q/health/ready`.
- [ ] Log/screenshot output `./scripts/backup-postgres.sh` dan `./scripts/restore-postgres-validation.sh`.
- [ ] Output perbandingan `SELECT count(*)` seluruh tabel inti (`tenants`, `equipment_units`, `reservations`, `reservation_items`, `inspection_records`, `maintenance_records`) antara database `capstone` dan `capstone_restore_validation`.
