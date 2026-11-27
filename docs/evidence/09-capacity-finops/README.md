# 09 — Capacity, Headroom & FinOps (M8)

## Evidence Metadata

| Field | Detail |
| :--- | :--- |
| **Scope** | Pengukuran beban baca (`GET /api/equipments` 75% & `GET /api/equipments/available` 25%), sampling CPU/working-set RAM via Kubernetes Metrics API, analisis headroom terhadap limits, usulan rightsizing `320Mi`, model biaya ilustratif, dan roadmap CloudStack |
| **Owner** | Muhammad Kevin Rasendriya (`2210512104`) & Tim Kelompok 2 |
| **Source / Version** | [`REPORT.md`](../../capacity/REPORT.md), [`09_evidence.jpg`](../../capacity/09_evidence.jpg), [`baseline-summary.jpg`](../../capacity/results/2026-11-23/baseline-summary.jpg), [`cost-assumptions.json`](../../capacity/cost-assumptions.json), `scripts/capacity.py`, `scripts/capacity_report.py`, `tests/test_capacity.py` (commit baseline `4987995` & skrip `d9545bf`) |
| **Timestamp** | Pengukuran baseline & live demo Kevin: 23 November 2026 (20:28–21:05 WIB); verifikasi unit test Python `test_capacity.py`: 27 November 2026 |
| **Method** | Closed-loop load test Python (`Idle` 60s, `2 worker` 120s, `8 worker` 120s, serta `live-demo-60s` 8 worker 60s) dengan sampling Metrics API setiap 15 detik, ditambah 4 unit test matematika kapasitas & unit-cost (`python3 -m unittest discover -s tests -p "test_*.py" -v`) |
| **Observation** | - **Baseline 8 worker (120s):** `949/949` HTTP 200 (`7.92 RPS`), p95 client `47.0 ms` (maks `3875.0 ms`), CPU max `214.31m` (headroom `57.14%`), RAM max `242.55MiB` (headroom `52.63%`).\n- **Live demo Kevin 8 worker (60s):** `480/480` HTTP 200 (`8.0146 RPS`), p95 client `78 ms` (maks `266 ms`), CPU max `359.26m` (headroom `28.15%`), RAM max `254.98MiB` (headroom `50.20%`).\n- **Model biaya ilustratif (`cost-assumptions.json`):** 1 VM `2 vCPU / 4 GiB / 20 GiB` (720 jam/bln) = `Rp1.738.000/bulan` atau `Rp17.380 per 1.000 request` pada asumsi `100.000` request/bulan.\n- **Unit tests:** `test_capacity.py` lulus **4/4 OK** |
| **Interpretation** | Pemakaian RAM aktual (`242–255 MiB`) sudah melampaui `requests.memory` aktif (`192Mi`), sehingga scheduler berisiko menempatkan pod di node yang sempit; CPU dan RAM masih memiliki headroom >28% dan >50% terhadap limits `500m/512Mi` |
| **Limitation** | 1) Pengukuran ini dijalankan pada baseline pra-OIDC (`trust-headers=true`, `4987995`) dan hanya menguji 2 endpoint baca dengan dataset demo kecil pada 1 pod di 1 laptop; 2) Sampling Metrics API 15s memakai window averaging sehingga lonjakan CPU singkat dapat terlewat; 3) Tarif VM & volume bulanan bersifat asumsi ilustratif (belum termasuk pajak, egress, backup, HA, dan tooling CI/observability) |
| **Decision Supported** | Mengusulkan kenaikan `requests.memory` menjadi **`320Mi`** (`peak * 1.25` dibulatkan ke kelipatan 64 MiB) pada `ADR-007` setelah uji ulang dengan token JWT aktif dan beban tulis |

## Tindak Lanjut
- [ ] Jalankan ulang 3 fase menggunakan `--token-file` (JWT Keycloak aktif) sesuai `TESTING.md` Bagian 8.B dan perbarui `REPORT.md`.
- [ ] Terapkan `requests.memory: 320Mi` ke `gitops/base/deployment.yaml` setelah validasi JWT selesai.
