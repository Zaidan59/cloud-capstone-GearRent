# 07 — Operations, Observability & SLI/SLO (M5)

## Evidence Metadata

| Field | Detail |
| :--- | :--- |
| **Scope** | Endpoint kesehatan SmallRye (`/q/health/live`, `/q/health/ready`), telemetry Micrometer Prometheus (`/q/metrics` + `MetricsConfig` histograms + metrik Agroal), target scrape Prometheus, dan dashboard Grafana `GearRent - Service Overview & SLO` |
| **Owner** | Kelompok 2 (Kevin, Zaidan, Raihan, Justin) |
| **Source / Version** | Ringkasan visual [`07_evidence.jpg`](07_evidence.jpg); konfigurasi aktual di `infrastructure/prometheus/prometheus.yml`, `infrastructure/grafana/provisioning/dashboards/json/gearrent-service-dashboard.json`, `MetricsConfig.java`, dan `ObservabilityMetricsTest.java` |
| **Timestamp** | Gambar baseline: 12 November 2026; verifikasi `ObservabilityMetricsTest` terbaru: 27 November 2026 |
| **Method** | Prometheus `3.15.1` men-scrape `k3d-capstone-serverlb:80/q/metrics` setiap `10s`; Grafana `13.2.4` memvisualisasikan 13 panel metrik HTTP, latency p50/p95, Agroal DB pool, JVM Heap, CPU, dan Vert.x worker pool |
| **Observation** | `ObservabilityMetricsTest` (1/1 lulus) memverifikasi metrik `http_server_requests_seconds_bucket` dan `agroal_*` tersedia di `/q/metrics`. Dashboard Grafana `GearRent - Service Overview & SLO` mendefinisikan dua SLO utama secara eksplisit: **5xx Error Rate < 1%** dan **p95 Latency <= 2s**, disertai pemecahan outcome (`SUCCESS` vs `CLIENT_ERROR` 4xx) dan status target `up{job="capstone-app"}` |
| **Interpretation** | Telemetry memungkinkan operator membedakan error aplikasi (`5xx`), penolakan otorisasi/input klien (`4xx`), kejenuhan connection pool PostgreSQL (`agroal_awaiting_count`), dan lonjakan latency sebelum keluhan pengguna membesar |
| **Limitation** | 1) Kartu ringkasan lama [`07_evidence.jpg`](07_evidence.jpg) menulis target latency `<= 200 ms`, sedangkan kesepakatan proposal & panel dashboard Grafana aktual memakai **p95 <= 2s** (hasil uji kapasitas M8 lokal mencatat p95 aktual 47–78 ms); 2) Scraping Prometheus hanya mencatat trafik lewat Ingress `:8088`, bukan mode dev lokal `:8080`; 3) Distributed tracing (OpenTelemetry) dan centralized log aggregation belum diaktifkan |
| **Decision Supported** | Menetapkan `5xx < 1%` dan `p95 <= 2s` sebagai *service objective* operasional serta mewajibkan pengujian trafik melalui `:8088` untuk validasi SLO |

## Berkas & Tindak Lanjut
- Tersedia: [`07_evidence.jpg`](07_evidence.jpg)
- Disarankan tambah: screenshot langsung dashboard Grafana `http://127.0.0.1:3000/d/gearrent-overview/gearrent-service-overview-and-slo` dan target Prometheus `UP` pasca trafik campuran (`200` + `403`) di `:8088`.
