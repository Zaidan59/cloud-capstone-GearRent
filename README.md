# GearRent — Cloud Computing Capstone Project (Kelompok 2)

Sistem informasi *cloud-native* multi-tenant untuk pengelolaan penyewaan (*rental*), alokasi unit, serah-terima/inspeksi, dan pemeliharaan (*maintenance*) peralatan multimedia.

**Tim Kelompok 2:**
- Muhammad Kevin Rasendriya (`2210512104`)
- Muhammad Zaidan (`2410512043`)
- Dhaifulloh Raihan Zayyan (`2410512129`)
- Justin Joshia Banjarnahor (`2410512133`)

> **Panduan Pengujian:** Seluruh skenario pengujian aplikasi, API OIDC, observability, self-healing, backup/restore, dan benchmark kapasitas dipusatkan di **[TESTING.md](TESTING.md)**.

## Struktur Repository

```text
├── app/              # Backend Quarkus GearRent (REST API, tests, Dockerfile, .gitea CI)
├── gitops/           # Manifest Kubernetes Kustomize (base & overlays/dev) untuk Argo CD
├── infrastructure/   # Docker Compose (Gitea, Runner, Postgres, Keycloak, Registry, Prometheus, Grafana)
├── scripts/          # Skrip otomasi setup, deploy, backup/restore, & kapasitas (.sh & .ps1/.py)
├── docs/             # Proposal (proposal/), Kapasitas M8 (capacity/), ADR (decisions/), & Portfolio (evidence/)
├── tests/            # Unit test skrip kapasitas Python (test_capacity.py)
├── smoke-test/       # Uji koneksi registry & kluster k3d
├── templates/        # Template referensi infrastruktur
├── guides/           # Dokumen panduan capstone dari dosen
└── TESTING.md        # Panduan lengkap pengujian end-to-end M2–M8
```

---

## 1. Mode Pengerjaan Cepat (Aplikasi Saja — `:8080`)

Untuk mengembangkan kode di `app/` dengan *live reload* tanpa kluster Kubernetes penuh (butuh **JDK 21+**; Docker opsional jika butuh Postgres via `up-core`).

```bash
# Linux / macOS
cd app && ./mvnw quarkus:dev
```

```powershell
# Windows PowerShell (pastikan JAVA_HOME mengarah ke JDK 21+, bukan Java 8)
cd app; .\mvnw.cmd quarkus:dev
```

- **App / Swagger UI:** `http://localhost:8080/` | `http://localhost:8080/q/swagger-ui/`
- **Health / OpenAPI:** `http://localhost:8080/q/health` | `http://localhost:8080/q/openapi`

> **Catatan:** Mode `quarkus:dev` berjalan di `:8080` dan tidak di-scrape Prometheus. Untuk verifikasi lengkap di kluster Kubernetes, gunakan Ingress `:8088` sesuai panduan di [TESTING.md](TESTING.md).

---

## 2. Setup Awal Infrastruktur, Cluster, & Deploy Pertama

Prasyarat: **Docker & Docker Compose**, **Git**, **curl**, **kubectl**, **k3d**, **Python 3.10+** *(plus **jq** di Linux/macOS)*. Jalankan semua perintah dari **root repository**.

### A. Setup Core Services, Cluster `k3d`, & Argo CD

**Linux / macOS (Bash):**
```bash
cp -n infrastructure/.env.example infrastructure/.env
./scripts/doctor.sh
./scripts/up-core.sh
./scripts/bootstrap-gitea.sh
./scripts/start-runner.sh
./scripts/create-cluster.sh

kubectl create namespace capstone --dry-run=client -o yaml | kubectl apply -f -
kubectl -n capstone create secret generic capstone-app-secret \
  --from-literal=DB_PASSWORD=capstone --dry-run=client -o yaml | kubectl apply -f -

rm -rf app/target
./scripts/bootstrap-project.sh
./scripts/install-argocd.sh
./scripts/deploy-lab.sh
```

**Windows (PowerShell):**
```powershell
if (!(Test-Path infrastructure\.env)) { Copy-Item infrastructure\.env.example infrastructure\.env }
.\scripts\doctor.ps1
.\scripts\up-core.ps1
.\scripts\bootstrap-gitea.ps1
.\scripts\start-runner.ps1
.\scripts\create-cluster.ps1

kubectl create namespace capstone --dry-run=client -o yaml | kubectl apply -f -
kubectl -n capstone create secret generic capstone-app-secret `
  --from-literal=DB_PASSWORD=capstone --dry-run=client -o yaml | kubectl apply -f -

Remove-Item -Recurse -Force app\target -ErrorAction SilentlyContinue
.\scripts\bootstrap-project.ps1
.\scripts\install-argocd.ps1
.\scripts\deploy-lab.ps1
```

### B. Nyalakan Stack Observability (Prometheus & Grafana)

```bash
./scripts/up-observability.sh   # Windows: .\scripts\up-observability.ps1
```

> **Catatan Setup:**
> - `deploy-lab` di akhir memicu CI Run #2 setelah secret `GITOPS_PASSWORD` tersimpan di Gitea (Run #1 saat `bootstrap-project` sering gagal `401`). Tunggu hingga Argo CD berstatus `Synced` & `Healthy`.
> - Jika runner error `runner registration token not found`, jalankan ulang `bootstrap-gitea`, hapus `docker rm -f capstone-gitea-runner`, lalu jalankan `start-runner`.
> - Jika `k3d` tidak stabil (`EOF`), buat ulang kluster dengan `--image rancher/k3s:v1.35.10-k3s1` (flag lengkap ada di `scripts/create-cluster.*`).

---

## 3. Alur Pengerjaan Harian & Start/Stop Lab

### A. Commit & Deploy Perubahan Kode (`app/`)
```bash
# 1. Commit perubahan (jangan commit infrastructure/.env, .runtime/, atau backups/*.dump)
git status && git add <file> && git commit -m "feat: ..." && git push origin main

# 2. Deploy commit HEAD folder app/ ke Gitea CI -> Registry -> Argo CD -> k3d
./scripts/deploy-lab.sh         # Windows: .\scripts\deploy-lab.ps1
```

> **Catatan `gitops/`:** `deploy-lab` hanya mendorong folder `app/` ke repo Gitea `student/capstone-app`. Jika Anda mengubah manifest di `gitops/` (misalnya resource `deployment.yaml` atau `configmap.yaml`), dorong juga perubahannya ke repo Gitea `student/capstone-gitops` agar Argo CD melakukan sinkronisasi ke kluster.

### B. Matikan & Nyalakan Kembali Lab Setelah Restart Laptop
```bash
# Stop sementara (data & kluster tetap tersimpan)
k3d cluster stop capstone
docker compose --env-file infrastructure/.env -f infrastructure/docker-compose.yml --profile ci --profile observability stop

# Start lagi
./scripts/up-core.sh && k3d cluster start capstone && ./scripts/start-runner.sh && ./scripts/status.sh
```

*(Jika menggunakan Prometheus/Grafana, jalankan kembali `./scripts/up-observability.sh`.)*

---

## 4. Daftar URL & Kredensial Default Lab Lokal

| Layanan | URL | Kredensial / Keterangan |
| :--- | :--- | :--- |
| **GearRent App (`k3d` Ingress)** | `http://capstone.localhost:8088` | Swagger: `/q/swagger-ui/`, Health: `/q/health`, Metrics: `/q/metrics` |
| **Gitea (Git & CI)** | `http://127.0.0.1:3001` | `student` / `student` *(Admin: `capstone-admin` / `capstone-admin`)* |
| **Keycloak (IAM/OIDC)** | `http://127.0.0.1:8081` | `admin` / `admin` *(Realm `capstone`)* |
| **PostgreSQL** | `127.0.0.1:5432` | `capstone` / `capstone` *(DB: `capstone`)* |
| **OCI Registry** | `http://127.0.0.1:5001/v2/_catalog` | Registry lokal |
| **Argo CD UI** | `https://127.0.0.1:8082` | Jalankan `./scripts/argocd-ui.sh`, user `admin` |
| **Prometheus / Grafana** | `http://127.0.0.1:9090` / `:3000` | Grafana: `admin` / `admin` *(nyalakan via `up-observability`)* |

Cek password admin Argo CD:
```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
```
```powershell
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String((kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}")))
```

---

## 5. Dokumentasi Pendukung & Pengujian

- **Panduan Pengujian Lengkap (M2–M8):** [TESTING.md](TESTING.md)
- **Proposal Awal (M0/M1):** [Kelompok 2_Capstone_104_043_129_133.pdf](docs/proposal/Kelompok%202_Capstone_104_043_129_133.pdf)
- **Kapasitas & FinOps (M8):** [docs/capacity/README.md](docs/capacity/README.md) & [docs/capacity/REPORT.md](docs/capacity/REPORT.md)
- **Architecture Decision Records (ADR-001 s.d. ADR-007):** [docs/decisions/README.md](docs/decisions/README.md)
- **Portfolio Evidence Terpadu (01–11):** [docs/evidence/README.md](docs/evidence/README.md)
- **Catatan Keamanan Lab Lokal:** [SECURITY_NOTES.md](SECURITY_NOTES.md) & [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
