# 02 — Architecture Baseline, Boundaries & Quality Scenarios (M1)

## Evidence Metadata

| Field | Detail |
| :--- | :--- |
| **Scope** | Arsitektur *provider-neutral*, alur delivery, *trust & failure boundaries*, serta skenario kualitas (*quality scenarios*) sistem GearRent |
| **Owner** | Kelompok 2 (Kevin, Zaidan, Raihan, Justin) |
| **Source / Version** | [Kelompok 2_Capstone_104_043_129_133.pdf](../../proposal/Kelompok%202_Capstone_104_043_129_133.pdf) (Bab 7–10 & Lampiran), [docs/decisions/](../../decisions/README.md) (`ADR-001` s.d. `ADR-007`) |
| **Timestamp** | Baseline awal Oktober 2026; diperbarui sesuai implementasi OIDC & M8 pada 27 November 2026 |
| **Method** | Penyusunan diagram 3 lapis (*Layered Overview*, *Application & Data*, *Technology & Delivery*), pemetaan *trust boundaries* (klien -> API, tenant A -> B, app -> DB, app -> foto, repo/CI -> runtime), analisis *failure boundaries*, dan perumusan target kualitas terukur |
| **Observation** | Arsitektur dipisah ke 3 lapis logis: *Application* (Quarkus Modular Monolith + Keycloak OIDC), *Data & Storage* (PostgreSQL + abstraksi `PhotoStorageService`), dan *Platform & Operations* (k3d/k3s, Gitea CI, Registry, Argo CD, Prometheus, Grafana). Delivery mengikuti alur: `Source -> Gitea CI (verify/build/Trivy/push) -> capstone-gitops -> Argo CD -> k3d` |
| **Interpretation** | Pemisahan fungsi menjaga desain tetap *provider-neutral* (siap dipetakan dari laptop/k3d ke CloudStack VM + k3s tanpa menulis ulang aplikasi). Kontrak `PhotoStorageService` memisahkan endpoint HTTP dari detail penyimpanan fisik foto |
| **Limitation** | Pada implementasi lab saat ini, `LocalPhotoStorageService` masih memakai penyimpanan lokal container (`/tmp/gearrent-photos`, lihat `ADR-003`) dan belum memakai MinIO/S3 atau PVC. Diagram lampiran di PDF proposal belum diekspor ulang sebagai gambar terpisah yang mencerminkan integrasi OIDC M6 |
| **Decision Supported** | Menetapkan pola **Modular Monolith Quarkus** (`ADR-001`), **shared DB dengan `tenant_id` + `PESSIMISTIC_WRITE`** (`ADR-002`), dan **CI terpisah dari GitOps** (`ADR-004`) |

## Artefak yang Perlu Dilengkapi
- [ ] Ekspor/simpan gambar diagram arsitektur terbaru (`layered-overview.png`, `application-data.png`, `technology-delivery.png`) ke folder `02-architecture/` dengan catatan status aktual OIDC JWT & penyimpanan foto interim.
