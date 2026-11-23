# GearRent — Cloud Computing Capstone Project (Kelompok 2)

Sistem informasi *cloud-native* multi-tenant untuk pengelolaan penyewaan (*rental*), alokasi unit, serah-terima/inspeksi, dan pemeliharaan (*maintenance*) peralatan multimedia.

**Tim Kelompok 2:**
- Muhammad Kevin Rasendriya (2210512104)
- Muhammad Zaidan (2410512043)
- Dhaifulloh Raihan Zayyan (2410512129)
- Justin Joshia Banjarnahor (2410512133)

## Struktur Repository

```text
├── app/              # Backend Quarkus GearRent (REST API, tests, Dockerfile, .gitea CI)
├── gitops/           # Manifest Kubernetes Kustomize (base & overlays/dev) untuk Argo CD
├── infrastructure/   # Docker Compose (Gitea, Runner, Postgres, Keycloak, Registry, Prometheus, Grafana)
├── scripts/          # Skrip otomasi lab (Bash .sh & PowerShell .ps1)
├── smoke-test/       # Uji koneksi registry & kluster k3d
├── templates/        # Template referensi infrastruktur
└── guides/           # Dokumen panduan capstone
```

---

## 1. Mode Coding Cepat (Aplikasi Saja — `:8080`)

Untuk mengembangkan kode di `app/` dengan *live reload* tanpa kluster Kubernetes penuh (butuh **JDK 21+**; Docker opsional jika perlu Postgres via `up-core`).

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

---

## 2. Panduan End-to-End Cloud-Native Lab (Setup ➔ Deploy ➔ Test ➔ M5 ➔ M7)

Prasyarat: **Docker & Docker Compose**, **Git**, **curl**, **kubectl**, **k3d** *(plus **jq** di Linux/macOS)*. Jalankan semua perintah dari **root repository**.

### Langkah 1 — Setup Infrastruktur, Cluster, & Deploy Pertama

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

> **Catatan:**
> - `deploy-lab` di akhir memicu CI Run #2 setelah secret `GITOPS_PASSWORD` tersimpan di Gitea (Run #1 saat `bootstrap-project` sering gagal `401`). Tunggu hingga Argo CD berstatus `Synced` & `Healthy`.
> - Jika runner error `runner registration token not found`, jalankan ulang `bootstrap-gitea`, hapus `docker rm -f capstone-gitea-runner`, lalu jalankan `start-runner`.
> - Jika `k3d` tidak stabil (`EOF`), buat ulang kluster dengan `--image rancher/k3s:v1.35.10-k3s1` (flag lengkap ada di `scripts/create-cluster.*`).

---

### Langkah 2 — Automated Tests & Uji API di Kubernetes (`:8088`)

> **Penting:** Traffic ke Kubernetes menggunakan **`http://capstone.localhost:8088`** (tercatat di Prometheus/Grafana), berbeda dengan `quarkus:dev` (`:8080`).

1. **Jalankan 9 automated tests (H2 in-memory):**
   ```bash
   cd app && ./mvnw -B verify && cd ..    # Windows: cd app; .\mvnw.cmd -B verify; cd ..
   ```
2. **Uji endpoint `/api/*` di Kubernetes** (header wajib sementara sebelum Keycloak OIDC penuh: `X-Tenant-Id` = `tenant-a` / `tenant-b`, `X-User-Role` = `user` / `operator` / `admin`):
   ```bash
   # Cek health, akses positif (200), dan uji negatif (401 tanpa header & 403 lintas role)
   curl -sf http://capstone.localhost:8088/q/health/ready
   curl -s -H "X-Tenant-Id: tenant-a" -H "X-User-Role: operator" http://capstone.localhost:8088/api/equipments
   curl -s -o /dev/null -w "no-header: %{http_code}\n" http://capstone.localhost:8088/api/equipments
   curl -s -o /dev/null -w "user-report: %{http_code}\n" -H "X-Tenant-Id: tenant-a" -H "X-User-Role: user" http://capstone.localhost:8088/api/reports/summary
   ```

---

### Langkah 3 — Observability & Dashboard SLO (M5)

1. **Nyalakan Prometheus & Grafana:**
   ```bash
   ./scripts/up-observability.sh   # Windows: .\scripts\up-observability.ps1
   ```
2. **Kirim beberapa request ke `:8088`** selama ~20–30 detik (minimal 2 siklus scrape `10s`) agar kurva `rate()` & `p95 Latency` muncul:
   ```bash
   for i in $(seq 1 15); do curl -sf -H "X-Tenant-Id: tenant-a" -H "X-User-Role: operator" http://capstone.localhost:8088/api/equipments > /dev/null; sleep 2; done
   ```
3. **Verifikasi dashboard:**
   - **Prometheus:** `http://127.0.0.1:9090/targets` → pastikan `capstone-app` berstatus **`UP`**.
   - **Grafana:** `http://127.0.0.1:3000/d/gearrent-overview/gearrent-service-overview-and-slo` (`admin`/`admin`), atur rentang waktu ke `Last 5 minutes`.

---

### Langkah 4 — Uji Self-Healing & Backup/Restore PostgreSQL (M7)

1. **Uji pod recovery (Kubernetes + Argo CD `selfHeal`):**
   ```bash
   kubectl -n capstone delete pod -l app=capstone-app
   kubectl -n capstone wait --for=condition=Ready pod -l app=capstone-app --timeout=120s
   curl -sf http://capstone.localhost:8088/q/health/ready
   ```
2. **Backup & validasi restore ke database terpisah (`capstone_restore_validation`):**
   ```bash
   # Linux / macOS
   ./scripts/backup-postgres.sh
   ./scripts/restore-postgres-validation.sh "$(ls -t backups/*.dump | head -n 1)"
   ```
   ```powershell
   # Windows PowerShell 5.1 (pakai cmd /c agar file .dump tidak 0 byte)
   New-Item -ItemType Directory -Force -Path backups | Out-Null
   $dump = "backups\capstone-$(Get-Date -Format 'yyyyMMdd-HHmmss').dump"
   cmd /c "docker exec capstone-postgres pg_dump -U capstone -d capstone -Fc > $dump"
   docker exec capstone-postgres psql -U capstone -d postgres -c "DROP DATABASE IF EXISTS capstone_restore_validation; CREATE DATABASE capstone_restore_validation;"
   cmd /c "docker exec -i capstone-postgres pg_restore -U capstone -d capstone_restore_validation --no-owner < $dump"
   docker exec capstone-postgres psql -U capstone -d capstone_restore_validation -c "\dt"
   ```

---

## 3. Alur Kerja Harian & Start/Stop Lab

```bash
# 1. Verifikasi & commit perubahan (jangan commit infrastructure/.env, .runtime/, atau backups/*.dump)
cd app && ./mvnw -B verify && cd ..
git status && git add <file> && git commit -m "feat: ..." && git push origin main

# 2. Deploy commit HEAD folder app/ ke Gitea CI -> Registry -> Argo CD -> k3d
./scripts/deploy-lab.sh         # Windows: .\scripts\deploy-lab.ps1
```

**Matikan sementara / nyalakan kembali setelah restart laptop:**
```bash
# Stop (data & cluster tetap tersimpan)
k3d cluster stop capstone
docker compose --env-file infrastructure/.env -f infrastructure/docker-compose.yml --profile ci --profile observability stop

# Start lagi
./scripts/up-core.sh && k3d cluster start capstone && ./scripts/start-runner.sh && ./scripts/status.sh
```

---

## 4. Daftar URL & Kredensial Default

| Layanan | URL | Kredensial / Keterangan |
| :--- | :--- | :--- |
| **GearRent App (k3d Ingress)** | `http://capstone.localhost:8088` | Swagger: `/q/swagger-ui/`, Health: `/q/health`, Metrics: `/q/metrics` |
| **Gitea (Git & CI)** | `http://127.0.0.1:3001` | `student` / `student` *(Admin: `capstone-admin` / `capstone-admin`)* |
| **Keycloak (IAM/OIDC)** | `http://127.0.0.1:8081` | `admin` / `admin` |
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

## M8 Capacity dan FinOps

Pengukuran baseline, worksheet resource, model biaya, dan rencana CloudStack tersedia di [docs/capacity](docs/capacity/README.md). Bukti kontribusi 09 ada di [cloud-capstone-A02](cloud-capstone-A02/README.md).
