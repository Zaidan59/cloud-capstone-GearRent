# 10 — Architecture Decision Records (ADR)

Seluruh dokumen ADR (`ADR-001` s.d. `ADR-007`) dan bukti visualnya dipusatkan di **[docs/decisions/](../../decisions/README.md)** agar tidak terduplikasi.

## Evidence Metadata

| Field | Detail |
| :--- | :--- |
| **Scope** | Dokumentasi 7 Architecture Decision Records (`ADR-001` s.d. `ADR-007`) mencakup struktur aplikasi, multi-tenancy & locking, penyimpanan foto, CI/GitOps, OIDC JWT, Trivy scan gate, serta capacity/CloudStack |
| **Owner** | Kelompok 2 (Kevin, Zaidan, Raihan, Justin) |
| **Source / Version** | [`docs/decisions/README.md`](../../decisions/README.md), [`10_evidence.jpg`](../../decisions/10_evidence.jpg), [`ADR-001`](../../decisions/ADR-001-modular-monolith-quarkus.md), [`ADR-002`](../../decisions/ADR-002-multi-tenancy-and-locking.md), [`ADR-003`](../../decisions/ADR-003-photo-storage-abstraction.md), [`ADR-004`](../../decisions/ADR-004-ci-gitops-separation.md), [`ADR-005`](../../decisions/ADR-005-keycloak-oidc-jwt.md), [`ADR-006`](../../decisions/ADR-006-trivy-reporting-gate-and-cache.md), [`ADR-007`](../../decisions/ADR-007-resource-rightsizing-and-cloudstack-roadmap.md) |
| **Timestamp** | Disusun 26 November 2026; disatukan ke `docs/` pada 27 November 2026 |
| **Method** | Penulisan ADR terstruktur (*Context, Options Considered, Decision, Consequences & Limitations, Traceability*) yang ditautkan langsung ke kode sumber, manifest GitOps, dan hasil uji |
| **Observation** | Seluruh keputusan arsitektur utama M1–M8 memiliki alasan pemilihan opsi, konsekuensi positif, serta limitasi jujur yang terdokumentasi dan dapat ditelusuri ke berkas implementasinya |
| **Interpretation** | Memenuhi persyaratan Architecture Pack (Guideline Section 7 & 12.B) dan memudahkan setiap anggota menjawab alasan *trade-off* saat technical defense |
| **Limitation** | `ADR-003` (penyimpanan foto lokal) dan `ADR-007` (usulan `320Mi` & CloudStack) berstatus *Interim* / *Proposed* hingga migrasi volume/object storage dan uji ulang JWT dijalankan |
| **Decision Supported** | Menjadi rujukan tunggal keputusan desain lintas milestone untuk laporan akhir dan sidang tim |
