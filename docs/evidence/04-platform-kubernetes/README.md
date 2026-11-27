# 04 — Cloud-Native Platform & Kubernetes (M3)

## Evidence Metadata

| Field | Detail |
| :--- | :--- |
| **Scope** | Containerization (`app/Dockerfile`), kluster Kubernetes `k3d` (1 server, 1 agent), manifest Kustomize (`gitops/base/` & `overlays/dev/`), probes, resource requests/limits, dan Traefik Ingress (`capstone.localhost:8088`) |
| **Owner** | Kelompok 2 (Kevin, Zaidan, Raihan, Justin) |
| **Source / Version** | Ringkasan visual [`04_evidence.png`](04_evidence.png); manifest aktual di `gitops/base/` (`deployment.yaml`, `configmap.yaml`, `service.yaml`, `ingress.yaml`, `namespace.yaml`) dan `app/Dockerfile` |
| **Timestamp** | Gambar baseline: 12 November 2026; tinjauan manifest aktual: 27 November 2026 |
| **Method** | Build image non-root (`eclipse-temurin:21-jre`, `USER 1001`), penerapan manifest Kustomize ke namespace `capstone`, verifikasi `kubectl get nodes,all,ingress`, serta uji endpoint lewat port `8088` |
| **Observation** | Workload berjalan pada namespace `capstone` dengan Service ClusterIP port `80 -> 8080`, Traefik Ingress `capstone.localhost:8088`, readiness probe (`/q/health/ready`, delay 5s, period 5s), liveness probe (`/q/health/live`, delay 10s, period 10s), serta konfigurasi terpisah di `ConfigMap` (`DB_URL`, `DB_USER`, `OIDC_ISSUER`) dan runtime Secret `capstone-app-secret` (di luar Git). Pada manifest aktual `gitops/base/deployment.yaml`, batas resource adalah **requests `100m` CPU / `192Mi` RAM** dan **limits `500m` CPU / `512Mi` RAM** |
| **Interpretation** | Beban kerja terkemas secara reprodusibel, berjalan non-root, dan terpisah dari kredensial runtime. Health probes melindungi routing trafik saat startup atau kegagalan container |
| **Limitation** | 1) Kartu ringkasan awal [`04_evidence.png`](04_evidence.png) sempat mencantumkan `requests.memory: 256Mi`, sedangkan manifest aktual di repo memakai `192Mi` (dan pengukuran M8 menunjukkan working set puncak `254.98MiB`, sehingga diusulkan rightsizing ke `320Mi`); 2) Kluster `k3d` multi-node berjalan pada satu host laptop yang sama sehingga tidak membuktikan ketersediaan lintas host fisik |
| **Decision Supported** | Menetapkan pemisahan ConfigMap vs runtime Secret di kluster dan mencatat kebutuhan penyesuaian `requests.memory` dari `192Mi` ke `320Mi` (`ADR-007`) |

## Berkas & Tindak Lanjut
- Tersedia: [`04_evidence.png`](04_evidence.png)
- Disarankan tambah: screenshot terminal asli `kubectl get nodes -o wide`, `kubectl -n capstone get all,ingress`, dan `kubectl -n capstone describe deploy capstone-app` dari deployment terbaru.
