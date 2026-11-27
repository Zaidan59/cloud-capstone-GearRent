# ADR-007: Baseline Resource (`100m/192Mi`–`500m/512Mi`), Usulan Rightsizing `320Mi`, & Roadmap CloudStack

- **Status:** Accepted (Baseline & Model) / Proposed (Rightsizing `320Mi`)
- **Tanggal:** 2026-11-26
- **Owner:** Muhammad Kevin Rasendriya (2210512104) & Tim Kelompok 2 (A02)
- **Milestone:** M3 (Cloud-Native Platform), M8 (Capacity & FinOps)

## Context
Manifest awal `gitops/base/deployment.yaml` menetapkan resource pod `requests: 100m CPU / 192Mi RAM` dan `limits: 500m CPU / 512Mi RAM`. Tim perlu membuktikan apakah konfigurasi tersebut mencukupi di bawah beban terukur, menentukan rekomendasi *rightsizing* tanpa mengorbankan kestabilan, serta menyiapkan pemetaan kapasitas dan biaya menuju VM CloudStack.

## Options Considered
1. **Langsung mengubah manifest produksi ke angka tebakan tanpa benchmark:**
   - *Pro:* Cepat diubah.
   - *Kontra:* Berisiko OOMKilled jika terlalu kecil atau pemborosan kapasitas node jika terlalu besar, dan menyulitkan perbandingan baseline.
2. **Mempertahankan baseline `100m/192Mi`–`500m/512Mi`, mengukur aktual, dan mengusulkan `320Mi` berbasis formula terukur (Dipilih):**
   - *Pro:* Pemakaian aktual terukur melalui `scripts/capacity.py` dan Metrics API setiap 15 detik; keputusan usulan transparan (`peak working set * 1.25` dibulatkan ke kelipatan 64 MiB).
   - *Kontra:* Usulan `320Mi` baru tervalidasi pada beban baca (`75% GET /api/equipments`, `25% GET /api/equipments/available`) di runtime sebelum OIDC (`4987995`), belum diuji ulang dengan beban tulis & JWT.

## Decision
1. Menjaga baseline aktif `100m/192Mi` – `500m/512Mi` sebagai acuan tetap pada dokumentasi M3/M8 saat ini.
2. Mengajukan status **`Proposed`** untuk menaikkan `requests.memory` menjadi **`320Mi`** (dengan `requests.cpu: 100m` dan `limits: 500m/512Mi` tetap) sebelum penerapan ke manifest setelah uji ulang beban JWT dan transaksi tulis selesai.
3. Menetapkan model biaya ilustratif satu VM kandidat (`2 vCPU / 4 GiB RAM / 20 GiB disk`, 720 jam/bulan) sebagai dasar estimasi awal (`Rp1.738.000/bulan` atau `Rp17.380/1.000 request` pada asumsi 100.000 request sukses/bulan) serta roadmap migrasi 6 langkah ke **Apache CloudStack + k3s**.

## Consequences & Limitations
- **Data Terukur (`docs/capacity/REPORT.md`):**
  - Baseline 8 worker (`120s`, `949/949 HTTP 200`, `7.92 RPS`): `p95 = 47 ms`, CPU max `214.31m` (headroom `57.14%`), RAM max `242.55MiB` (headroom `52.63%`).
  - Live demo 8 worker (`60s`, `480/480 HTTP 200`, `8.0146 RPS`): `p95 = 78 ms`, CPU max `359.26m` (headroom `28.15%`), RAM max `254.98MiB` (headroom `50.20%`).
  - Terlihat bahwa request memory `192Mi` lebih rendah daripada puncak working set `254.98MiB`, sehingga usulan `320Mi` penting untuk stabilitas scheduling Kubernetes.
- **Limitasi:** Model biaya belum memasukkan pajak, egress, backup storage, HA multi-node, dan biaya tooling CI/observability terpisah; migrasi CloudStack masih berupa roadmap konseptual yang belum dieksekusi di VM nyata.

## Traceability / Evidence
- `gitops/base/deployment.yaml`
- `docs/capacity/REPORT.md`, `docs/capacity/README.md`, `docs/capacity/cost-assumptions.json`
- `docs/capacity/09_evidence.jpg` (dirujuk dari `docs/evidence/09-capacity-finops/README.md`)
- `scripts/capacity.py`, `scripts/capacity_report.py`, `tests/test_capacity.py` (`4/4 OK`)
