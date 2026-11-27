# 10 Decisions & Architecture Decision Records (ADR) — GearRent A02

Direktori ini mendokumentasikan keputusan arsitektur dan operasional utama pada proyek **GearRent** (Kelompok 2 / A02), lengkap dengan konteks, alternatif yang dipertimbangkan, keputusan, konsekuensi/limitasi, serta jejak verifikasi di repositori.

Ringkasan visual tersedia di [10_evidence.jpg](10_evidence.jpg).

## Daftar ADR

| ID | Keputusan | Status | Milestone | Fokus Utama |
| :--- | :--- | :--- | :--- | :--- |
| [ADR-001](ADR-001-modular-monolith-quarkus.md) | Modular Monolith Quarkus vs Microservices | Accepted | M1, M2 | Kesederhanaan operasional, batas domain jelas, waktu respons rendah |
| [ADR-002](ADR-002-multi-tenancy-and-locking.md) | Multi-Tenancy `tenant_id` + Ascending `PESSIMISTIC_WRITE` | Accepted | M1, M2 | Isolasi tenant dalam 1 PostgreSQL & pencegahan *double booking* / *deadlock* |
| [ADR-003](ADR-003-photo-storage-abstraction.md) | Abstraksi `PhotoStorageService` (`LocalPhotoStorageService` sementara) | Accepted *(Interim)* | M1, M2, M7 | Kontrak penyimpanan terpisah agar siap swap ke MinIO/S3 atau PVC |
| [ADR-004](ADR-004-ci-gitops-separation.md) | Pemisahan App CI (Gitea) & GitOps CD (Argo CD `selfHeal`) | Accepted | M3, M4 | CI tidak pernah `kubectl apply`; perubahan cluster deklaratif via tag SHA |
| [ADR-005](ADR-005-keycloak-oidc-jwt.md) | Keycloak OIDC Bearer JWT (`trust-headers=false`) & Split-Horizon Issuer | Accepted | M6 | Eliminasi *header spoofing*, RBAC & tenant group dari JWT terverifikasi |
| [ADR-006](ADR-006-trivy-reporting-gate-and-cache.md) | Trivy Scan Reporting Gate (`--exit-code 0`) + Persistent Cache Volume | Accepted | M4, M6 | Interpretasi CVE berbasis risiko dan stabilitas pipeline dengan `trivy-cache` |
| [ADR-007](ADR-007-resource-rightsizing-and-cloudstack-roadmap.md) | Baseline Resource (`100m/192Mi`–`500m/512Mi`), Usulan `320Mi`, & CloudStack | Accepted / Proposed | M3, M8 | Penyesuaian kapasitas berbasis pengukuran aktual & pemetaan IaaS |

## Keterkaitan & Batas Sistem Saat Ini
- **Keamanan & Isolasi:** `ADR-002` (isolasi query `tenant_id`) diperkuat oleh `ADR-005` (identitas tenant diambil langsung dari claim `groups` JWT Keycloak, bukan header bebas).
- **Reliabilitas & Storage:** `ADR-004` (`selfHeal` Argo CD) dan backup/restore PostgreSQL menjaga konsistensi workload dan data relasional, sedangkan `ADR-003` secara eksplisit mencatat limitasi penyimpanan foto lokal yang masih bersifat *ephemeral* hingga dimigrasikan ke MinIO/PVC.
- **Kapasitas & Evolusi Platform:** `ADR-007` memisahkan baseline aktif dari usulan *rightsizing* `320Mi` yang menunggu uji ulang beban JWT dan transaksi tulis sebelum diterapkan ke manifest GitOps.
