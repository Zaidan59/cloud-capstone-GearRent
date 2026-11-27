# Evidence Portfolio: Cloud Computing Final Capstone Project
**Kelompok 2 — GearRent: Cloud-Native Multi-Tenant Multimedia Rental & Maintenance System**  
**Program Studi Sistem Informasi, UPN "Veteran" Jakarta — 2026/2027**  
**Dosen Pengampu:** I Wayan Widi Pradnyana, S.Kom., M.TI.

---

## Tim Pengembang (Kelompok 2)
1. Muhammad Kevin Rasendriya (`2210512104`)
2. Muhammad Zaidan (`2410512043`)
3. Dhaifulloh Raihan Zayyan (`2410512129`)
4. Justin Joshia Banjarnahor (`2410512133`)

> **Catatan Tim 4 Orang (Guideline Section 4):** Anggota tim bertanggung jawab lintas area (*Application & Data*, *Platform & Delivery*, *Security & Operations*), dengan cakupan implementasi dan bukti yang diperluas (OIDC JWT + anti-spoofing, locking transaksi multi-unit, Trivy cache + analisis CVE, dashboard SLO kustom, skrip benchmark M8 + unit test FinOps, serta 7 ADR).

---

## Struktur & Status Berkas Portfolio

| Milestone / Area | Folder Portfolio | Berkas Utama | Status Kelengkapan |
| :--- | :--- | :--- | :--- |
| **M0 — Case Proposal & Business** | [`01-business/`](01-business/README.md) | `README.md`, [`Kelompok 2_Capstone_104_043_129_133.pdf`](../proposal/Kelompok%202_Capstone_104_043_129_133.pdf) | Metadata terisi; proposal tersimpan di `docs/proposal/` |
| **M1 — Architecture Baseline** | [`02-architecture/`](02-architecture/README.md) | `README.md` | Metadata & boundaries terisi; perlu salin/ekspor gambar diagram terbaru |
| **M2 — Application MVP & Data** | [`03-application-data/`](03-application-data/README.md) | [`03_evidence.png`](03-application-data/03_evidence.png), `README.md` | Gambar baseline (`0e552aa`) + metadata terisi; perlu screenshot Swagger/API mode OIDC terbaru |
| **M3 — Cloud-Native Platform** | [`04-platform-kubernetes/`](04-platform-kubernetes/README.md) | [`04_evidence.png`](04-platform-kubernetes/04_evidence.png), `README.md` | Gambar baseline + metadata & koreksi manifest aktual terisi |
| **M4 — Delivery Automation** | [`05-ci-cd-gitops/`](05-ci-cd-gitops/README.md) | [`05_evidence.png`](05-ci-cd-gitops/05_evidence.png), `README.md` | Gambar baseline + metadata & koreksi Trivy aktual terisi |
| **M6 — Security (IAM & Scan)** | [`06-security/`](06-security/README.md) | `README.md` | Metadata & hasil tes otomatis (`11/11`) terisi; **menunggu capture live JWT/Secret/Trivy** |
| **M5 — Operations & SLO** | [`07-observability-slo/`](07-observability-slo/README.md) | [`07_evidence.jpg`](07-observability-slo/07_evidence.jpg), `README.md` | Gambar baseline + metadata & koreksi SLO aktual terisi; disarankan capture Grafana aktual |
| **M7 — Reliability & Recovery** | [`08-reliability-recovery/`](08-reliability-recovery/README.md) | `README.md` | Metadata desain & limitasi terisi; **menunggu capture live delete pod & pg_restore** |
| **M8 — Capacity & FinOps** | [`09-capacity-finops/`](09-capacity-finops/README.md) | [`09_evidence.jpg`](../capacity/09_evidence.jpg), [`baseline-summary.jpg`](../capacity/results/2026-11-23/baseline-summary.jpg), [`REPORT.md`](../capacity/REPORT.md), [`cost-assumptions.json`](../capacity/cost-assumptions.json), `README.md` | **Lengkap untuk baseline pra-OIDC** (`docs/capacity/`); menunggu uji ulang dengan token JWT |
| **ADR — Architecture Decisions** | [`10-decisions-adr/`](10-decisions-adr/README.md) | [`10_evidence.jpg`](../decisions/10_evidence.jpg), [`docs/decisions/`](../decisions/README.md) (`ADR-001` s.d. `ADR-007`) | **Lengkap** (`docs/decisions/`) |
| **FINAL — Demo & Defense** | [`11-demo-defense/`](11-demo-defense/README.md) | `README.md` | Kerangka demo 13 langkah & bank soal siap; **menunggu final report & individual reflections** |

Setiap sub-folder mengikuti standar metadata 9 kolom (**Scope, Owner, Source/Version, Timestamp, Method, Observation, Interpretation, Limitation, Decision Supported**) sesuai *Final Capstone Project Guideline* Section 9.
