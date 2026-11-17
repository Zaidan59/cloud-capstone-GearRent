# GearRent — Cloud Computing Capstone Project (Kelompok 2)

**GearRent** adalah sistem informasi *cloud-native* untuk pengelolaan penyewaan (*rental*), alokasi unit, serah-terima, serta pemeliharaan (*maintenance*) peralatan multimedia (foto & video).

Repository ini menggabungkan **source code aplikasi (`app/`)**, **konfigurasi GitOps Kubernetes (`gitops/`)**, serta **infrastruktur simulasi Cloud-Native lokal (`infrastructure/` & `scripts/`)**.

### Anggota Tim (Kelompok 2)
- **Muhammad Kevin Rasendriya** (2210512104)
- **Muhammad Zaidan** (2410512043)
- **Dhaifulloh Raihan Zayyan** (2410512129)
- **Justin Joshia Banjarnahor** (2410512133)

---

## Struktur Repository

```text
.
├── app/                  # Backend Quarkus Multi-Tenant GearRent (API, tests, Dockerfile, & .gitea/ CI workflow)
├── gitops/               # Manifest Kubernetes (Kustomize base & overlays) untuk Argo CD
├── infrastructure/       # Docker Compose (Gitea, Runner, PostgreSQL, Keycloak, Registry, Prometheus, & Grafana provisioning)
├── scripts/              # Skrip otomasi operasional lab (Bash & PowerShell)
├── smoke-test/           # Utilitas uji coba koneksi registry & kluster k3d
├── templates/            # Template referensi bawaan infrastruktur
└── guides/               # Dokumen panduan infrastruktur & proposal capstone
```

---

## Cara 1: Menjalankan Aplikasi Saja (Mode Coding / Live Reload)

Gunakan cara ini jika Anda baru melakukan `git clone` dan hanya ingin mengembangkan fitur di folder `app/` dengan *live reload* tanpa menyalakan kluster Kubernetes penuh.

### Prasyarat
- **JDK 21+** (disarankan **JDK 25 LTS** sesuai `VERSION_MATRIX.md`).
- **Docker** menyala (opsional jika ingin menjalankan PostgreSQL/Keycloak lokal melalui `up-core`).

> **Catatan Penting Pengguna Windows (`JAVA_HOME`):**
> Jika di laptop Anda juga ter-install Java 8, `.\mvnw.cmd` bisa keliru memakai Java 8 JRE dan gagal compile. Pastikan `JAVA_HOME` mengarah ke folder JDK 21/25 Anda sebelum menjalankan Maven:
> ```powershell
> # Cek versi Java yang terbaca Maven (dari dalam folder app\)
> .\mvnw.cmd --version
>
> # Jika masih terbaca Java 1.8, set JAVA_HOME ke folder JDK Anda (contoh Eclipse Adoptium JDK 25):
> [Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot", "User")
> $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
> $env:Path = "$env:JAVA_HOME\bin;$env:Path"
> ```

### Menjalankan Aplikasi
*(Jika aplikasi membutuhkan PostgreSQL container, nyalakan dulu layanan inti dari root repo dengan `./scripts/up-core.sh` atau `.\scripts\up-core.ps1`).*

```bash
cd app

# Linux / macOS (Bash)
./mvnw quarkus:dev
```

```powershell
cd app

# Windows (PowerShell — wajib pakai .\mvnw.cmd, bukan ./mvnw)
.\mvnw.cmd quarkus:dev
```

- **Endpoint API:** `http://localhost:8080/` dan `http://localhost:8080/hello`
- **Swagger UI (Dokumentasi & Test API):** `http://localhost:8080/q/swagger-ui`
- **OpenAPI Schema:** `http://localhost:8080/q/openapi`
- **Health Check:** `http://localhost:8080/q/health`
- **Quarkus Dev UI:** `http://localhost:8080/q/dev/`

*(Setiap perubahan kode di `app/src/` akan langsung ter-reload otomatis saat disimpan. Tekan `Ctrl+C` untuk berhenti.)*

---

## Cara 2: Setup Ekosistem Cloud-Native Penuh (Pertama Kali Clone)

Gunakan panduan ini jika Anda ingin menjalankan simulasi lengkap (**Gitea + CI Runner + Local Registry + PostgreSQL + Keycloak + Kubernetes k3d + Argo CD**) di laptop Anda.

### Prasyarat Tools
Pastikan sudah ter-install dan siap dipakai:
- **Docker** & **Docker Compose** (kondisi menyala/running; di Windows gunakan Docker Desktop dengan backend WSL2)
- **Git**, **curl**, **kubectl**, **k3d**
- **jq** *(khusus pengguna Linux/macOS)*

> **Catatan sebelum memulai (jika ganti laptop / clone baru):**
> - Seluruh perintah di bawah dijalankan dari **folder root repository**.
> - Jika di dalam repo sudah ada `infrastructure/.runtime/runner-token` sisa perangkat lain, tetap jalankan langkah `bootstrap-gitea` agar token baru untuk Gitea di laptop Anda di-generate ulang.

---

### A. Pengguna Linux / macOS (Bash)

```bash
# 1. Buat file environment lokal (tidak menimpa jika sudah ada)
cp -n infrastructure/.env.example infrastructure/.env

# 2. Cek kesiapan tools & port
./scripts/doctor.sh

# 3. Nyalakan layanan inti (Gitea, Postgres, Keycloak, Registry)
./scripts/up-core.sh

# 4. Inisialisasi akun Gitea & generate token runner lokal
./scripts/bootstrap-gitea.sh

# 5. Bangun & jalankan Gitea CI Runner
# (Jika sebelumnya pernah nyala dengan token lama, hapus dulu: docker rm -f capstone-gitea-runner)
./scripts/start-runner.sh

# 6. Buat kluster Kubernetes k3d (tunggu sampai kedua node berstatus Ready)
./scripts/create-cluster.sh

# 7. Buat Kubernetes Secret untuk password database di namespace capstone
kubectl create namespace capstone --dry-run=client -o yaml | kubectl apply -f -
kubectl -n capstone create secret generic capstone-app-secret \
  --from-literal=DB_PASSWORD=capstone \
  --dry-run=client -o yaml | kubectl apply -f -

# 8. Bersihkan artifact build lokal jika ada, lalu push app/ dan gitops/ ke Gitea lokal
rm -rf app/target
./scripts/bootstrap-project.sh

# 9. Install Argo CD & hubungkan ke repositori GitOps lokal
./scripts/install-argocd.sh

# 10. Picu CI Run baru jika Run #1 gagal di langkah clone GitOps (lihat catatan di bawah)
./scripts/deploy-lab.sh
```

---

### B. Pengguna Windows (PowerShell)

> Jalankan di **PowerShell** dari folder root repository. Pastikan tidak ada karakter backtick (`` ` ``) nyasar di akhir nama skrip saat copy-paste.

```powershell
# 1. Buat file environment lokal (hanya jika belum ada)
if (!(Test-Path infrastructure\.env)) { Copy-Item infrastructure\.env.example infrastructure\.env }

# 2. Cek kesiapan tools & Docker engine
.\scripts\doctor.ps1

# 3. Nyalakan layanan inti (Gitea, Postgres, Keycloak, Registry)
.\scripts\up-core.ps1

# 4. Inisialisasi akun Gitea (capstone-admin & student) & generate token runner baru
.\scripts\bootstrap-gitea.ps1

# 5. Bangun & jalankan Gitea CI Runner
# (Jika sebelumnya pernah nyala dengan token lama, hapus dulu: docker rm -f capstone-gitea-runner)
.\scripts\start-runner.ps1

# 6. Buat kluster Kubernetes k3d (pastikan node Ready & tidak Restarting)
.\scripts\create-cluster.ps1

# 7. Buat namespace & Kubernetes Secret untuk password database
kubectl create namespace capstone --dry-run=client -o yaml | kubectl apply -f -
kubectl -n capstone create secret generic capstone-app-secret --from-literal=DB_PASSWORD=capstone --dry-run=client -o yaml | kubectl apply -f -

# 8. Hapus app\target jika ada (mencegah error file kosong 0-byte), lalu push ke Gitea lokal
Remove-Item -Recurse -Force app\target -ErrorAction SilentlyContinue
.\scripts\bootstrap-project.ps1

# 9. Install Argo CD & hubungkan ke repositori GitOps lokal
.\scripts\install-argocd.ps1

# 10. Picu CI Run baru jika Run #1 gagal di langkah clone GitOps (lihat catatan di bawah)
.\scripts\deploy-lab.ps1
```

---

### Catatan Penting Saat Setup Pertama (Windows & Linux)

1. **Kenapa langkah #10 (`deploy-lab`) disarankan setelah `bootstrap-project`?**
   Skrip `bootstrap-project` mem-push `capstone-app` (yang langsung memicu CI Run #1) sesaat sebelum secret `GITOPS_PASSWORD` selesai disimpan di Gitea. Akibatnya CI Run #1 sering gagal pada langkah clone `capstone-gitops` (`401 Unauthorized`). Setelah `bootstrap-project` selesai dan secret tersimpan, menjalankan `deploy-lab` sekali (atau klik *Re-run* di Gitea UI) akan memicu CI Run #2 yang sukses mem-push image ke `localhost:5001` dan meng-update tag di `capstone-gitops`.
2. **Argo CD sync setelah CI selesai:**
   Selama CI pertama belum sukses, pod `capstone-app` wajar berstatus `ErrImagePull` / `ImagePullBackOff` karena tag awal masih `:initial`. Setelah CI sukses, Argo CD otomatis menarik commit baru dalam ~3 menit (atau klik **Refresh** di Argo CD UI) hingga pod berubah menjadi `1/1 Running`.
3. **Jika kluster `k3d` gagal stabil (`EOF` / `the server is currently unable to handle the request`):**
   Beberapa host mengalami *startup race condition* pada `rancher/k3s:v1.35.5-k3s1` (`cloud-controller-manager exited`). Hapus kluster lalu buat ulang dengan tag `k3s` yang lebih stabil:
   ```bash
   k3d cluster delete capstone
   k3d cluster create capstone --image rancher/k3s:v1.35.10-k3s1 --servers 1 --agents 1 --network capstone-net --api-port 6550 -p "8088:80@loadbalancer" -p "8443:443@loadbalancer" --registry-config infrastructure/k3d/registries.yaml --wait
   ```
   *(Di Windows gunakan `infrastructure\k3d\registries.yaml`. Jika masih bermasalah, coba `rancher/k3s:v1.34.13-k3s1`.)*
4. **Cek log runner jika tidak kunjung jalan:**
   Jalankan `docker logs --tail 25 capstone-gitea-runner`. Jika muncul `runner registration token not found`, jalankan ulang `bootstrap-gitea`, hapus container runner (`docker rm -f capstone-gitea-runner`), lalu jalankan `start-runner`.

---

## Tahap Lanjutan (Setelah Setup Awal Berhasil): Testing, Monitoring (M5), & Uji Recovery (M7)

Bagian ini adalah **kelanjutan langsung** setelah Langkah 1–10 pada Cara 2 selesai (`capstone-app` berstatus `Synced` & `Healthy` di Argo CD). Jika ingin mengecek ulang kondisi seluruh stack kapan saja, jalankan:

- **Linux / macOS (Bash):** `./scripts/doctor.sh && ./scripts/status.sh`
- **Windows (PowerShell):** `.\scripts\doctor.ps1; .\scripts\status.ps1`

---

### 1. Automated Testing & Uji API GearRent (M2)

#### A. Menjalankan Automated Tests (`mvnw verify`)
Suite test menjalankan 9 skenario JUnit 5 / RestAssured (`GreetingResourceTest`, `ReservationWorkflowTest`, `AvailabilityAndConcurrencyTest`, `TenantIsolationAndRoleTest`, `ObservabilityMetricsTest`) menggunakan database H2 in-memory dan random HTTP test port (`%test.quarkus.http.test-port=0`, agar tidak bentrok dengan Keycloak di port `8081`). Membutuhkan **JDK 21+**.

- **Linux / macOS (Bash):**
  ```bash
  cd app
  ./mvnw -B verify
  ```
- **Windows (PowerShell):**
  ```powershell
  cd app
  .\mvnw.cmd -B verify
  ```
  Pastikan hasil akhir menunjukkan `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0` dan `BUILD SUCCESS`.

#### B. Uji API & Multi-Tenancy di Kubernetes (`http://capstone.localhost:8088`)

> **⚠️ Penting (Jangan tertukar port):**
> - Aplikasi di **Kubernetes (`k3d`)** berjalan di **`http://capstone.localhost:8088`** (`Swagger UI`: `http://capstone.localhost:8088/q/swagger-ui`). **Hanya traffic ke port `8088` yang terekam Prometheus & Grafana.**
> - Aplikasi mode lokal (`quarkus:dev` pada Cara 1) berjalan di **`http://localhost:8080`** dan tidak di-scrape Prometheus.

**Header Wajib untuk Endpoint `/api/*`:**
Saat ini aplikasi menggunakan mode header trust (`gearrent.auth.trust-headers=true`) untuk validasi tenant isolation & RBAC sebelum integrasi Keycloak OIDC penuh:
- `X-Tenant-Id`: `tenant-a` *(Jakarta Camera Rental: `CAM-001`, `LNS-001`, `LGT-001`)* atau `tenant-b` *(Bandung Visual Gear: `CAM-002`, `STB-001`)*
- `X-User-Role`: `user` | `operator` | `admin`
- `X-User-Id` *(opsional)*: mis. `budi` *(default: `<role>@<tenant>`)*

> Jika Anda mengetes lewat **Swagger UI**, klik endpoint lalu isi kolom header `X-Tenant-Id` dan `X-User-Role` sebelum menekan **Execute** (atau isi lewat tombol `Authorize` jika tersedia). Tanpa header tersebut, endpoint `/api/*` mengembalikan `401 Unauthorized`.

**Contoh Uji Cepat via Terminal:**

- **Linux / macOS (Bash):**
  ```bash
  # 1. Cek Health & daftar aset tenant-a (200 OK)
  curl -sf http://capstone.localhost:8088/q/health/ready
  curl -s -H "X-Tenant-Id: tenant-a" -H "X-User-Role: operator" http://capstone.localhost:8088/api/equipments
  curl -s -H "X-Tenant-Id: tenant-a" -H "X-User-Role: operator" http://capstone.localhost:8088/api/reports/summary

  # 2. Uji negatif: tanpa header (harus 401) & role user mengakses report operator (harus 403)
  curl -s -o /dev/null -w "no-header: %{http_code}\n" http://capstone.localhost:8088/api/equipments
  curl -s -o /dev/null -w "user-report: %{http_code}\n" -H "X-Tenant-Id: tenant-a" -H "X-User-Role: user" http://capstone.localhost:8088/api/reports/summary

  # 3. (Opsional) Buat reservasi baru multi-unit untuk tenant-a
  curl -i -X POST http://capstone.localhost:8088/api/reservations \
    -H "Content-Type: application/json" \
    -H "X-Tenant-Id: tenant-a" -H "X-User-Role: user" -H "X-User-Id: budi" \
    -d '{"customerName":"Budi","customerEmail":"budi@example.com","equipmentIds":[1],"startDate":"2026-12-01","endDate":"2026-12-03","notes":"Liputan event"}'
  ```

- **Windows (PowerShell):**
  ```powershell
  # 1. Cek Health & daftar aset + laporan tenant-a (200 OK)
  curl.exe -sf http://capstone.localhost:8088/q/health/ready
  $hOp = @{ "X-Tenant-Id" = "tenant-a"; "X-User-Role" = "operator" }
  Invoke-RestMethod -Uri "http://capstone.localhost:8088/api/equipments" -Headers $hOp | Format-Table id, assetCode, name, status, dailyRate
  Invoke-RestMethod -Uri "http://capstone.localhost:8088/api/reports/summary" -Headers $hOp

  # 2. Uji negatif: tanpa header (harus 401) & role user mengakses report operator (harus 403)
  curl.exe -s -o NUL -w "no-header: %{http_code}`n" http://capstone.localhost:8088/api/equipments
  curl.exe -s -o NUL -w "user-report: %{http_code}`n" -H "X-Tenant-Id: tenant-a" -H "X-User-Role: user" http://capstone.localhost:8088/api/reports/summary

  # 3. (Opsional) Buat reservasi baru multi-unit untuk tenant-a
  $body = '{"customerName":"Budi","customerEmail":"budi@example.com","equipmentIds":[1],"startDate":"2026-12-01","endDate":"2026-12-03","notes":"Liputan event"}'
  Invoke-RestMethod -Method Post -Uri "http://capstone.localhost:8088/api/reservations" `
    -Headers @{ "X-Tenant-Id" = "tenant-a"; "X-User-Role" = "user"; "X-User-Id" = "budi" } `
    -ContentType "application/json" -Body $body
  ```

---

### 2. Observability & Dashboard SLO (M5 — Prometheus & Grafana)

1. **Nyalakan Stack Observability:**
   - **Linux / macOS (Bash):** `./scripts/up-observability.sh`
   - **Windows (PowerShell):** `.\scripts\up-observability.ps1`

2. **Verifikasi Target Prometheus:**
   Buka `http://127.0.0.1:9090/targets` di browser dan pastikan target `capstone-app` (`http://k3d-capstone-serverlb:80/q/metrics`, interval `10s`) berstatus **`UP`**.
   Anda juga dapat memeriksa endpoint metrik mentah secara langsung di `http://capstone.localhost:8088/q/metrics`.

3. **Buka Dashboard Grafana (Auto-Provisioned):**
   - Buka `http://127.0.0.1:3000` (login: `admin` / `admin`).
   - Dashboard **`GearRent - Service Overview & SLO`** otomatis terpasang dari `infrastructure/grafana/provisioning/dashboards/` dan dapat dibuka langsung di:
     `http://127.0.0.1:3000/d/gearrent-overview/gearrent-service-overview-and-slo`
   - Panel utama yang dipantau: `Target Health (up)`, `HTTP Request Rate`, `5xx Error Rate (SLO < 1%)`, `p95 Latency (SLO <= 2s)`, `HTTP Request Rate by Route & Status`, `Request Latency Percentiles (p50 & p95 vs 2s SLO)`, `PostgreSQL Agroal Connection Pool`, `JVM Heap Memory`, `CPU Usage (%)`, dan `Vert.x Worker Pool`.

4. **Memicu Traffic Agar Kurva `rate()` & `p95 Latency` Muncul:**
   > **Catatan:** Micrometer mencatat metrik route HTTP secara *lazy* sejak request pertama masuk, dan fungsi `rate()` Prometheus butuh minimal **2 kali scrape** (`>= 20 detik` pada interval `10s`) untuk menghitung kemiringan kurva. Setelah mengirim traffic ke port **`8088`**, tunggu ~15–20 detik dan atur time range Grafana ke **`Last 5 minutes`** atau **`Last 15 minutes`** (refresh `5s`).

   - **Linux / macOS (Bash):**
     ```bash
     for i in $(seq 1 15); do
       curl -sf -H "X-Tenant-Id: tenant-a" -H "X-User-Role: operator" http://capstone.localhost:8088/api/equipments > /dev/null
       sleep 2
     done
     ```
   - **Windows (PowerShell):**
     ```powershell
     1..15 | ForEach-Object {
       Invoke-RestMethod -Uri "http://capstone.localhost:8088/api/equipments" -Headers @{ "X-Tenant-Id" = "tenant-a"; "X-User-Role" = "operator" } | Out-Null
       Start-Sleep -Seconds 2
     }
     ```

---

### 3. Uji Recovery & Backup/Restore Validation (M7)

#### A. Uji Pod Self-Healing & Persistensi Data (Kubernetes)
Simulasikan pod aplikasi terhapus/crash lalu pastikan Kubernetes membuat pod pengganti berstatus `Ready` dalam <= 120 detik dan data PostgreSQL tetap utuh (`quarkus.hibernate-orm.database.generation=update` + `SeedDataInitializer` idempoten):

- **Linux / macOS (Bash):**
  ```bash
  kubectl -n capstone get pods -l app=capstone-app
  kubectl -n capstone delete pod -l app=capstone-app
  kubectl -n capstone wait --for=condition=Ready pod -l app=capstone-app --timeout=120s
  curl -sf http://capstone.localhost:8088/q/health/ready
  curl -s -H "X-Tenant-Id: tenant-a" -H "X-User-Role: operator" http://capstone.localhost:8088/api/equipments
  ```
- **Windows (PowerShell):**
  ```powershell
  kubectl -n capstone get pods -l app=capstone-app
  kubectl -n capstone delete pod -l app=capstone-app
  kubectl -n capstone wait --for=condition=Ready pod -l app=capstone-app --timeout=120s
  curl.exe -sf http://capstone.localhost:8088/q/health/ready
  Invoke-RestMethod -Uri "http://capstone.localhost:8088/api/equipments" -Headers @{ "X-Tenant-Id" = "tenant-a"; "X-User-Role" = "operator" } | Format-Table id, assetCode, name, status
  ```

#### B. Backup PostgreSQL & Validasi Restore

> ⚠️ **Catatan Skrip PowerShell di Windows PowerShell 5.1:**
> Skrip bawaan `scripts\backup-postgres.ps1` membutuhkan **PowerShell 7+** (`ArgumentList` pada .NET 5+), dan `scripts\restore-postgres-validation.ps1` memiliki kendala urutan `param(...)`. Pada **Windows PowerShell 5.1** (bawaan Windows 10/11), gunakan perintah `docker exec` lewat `cmd /c` di bawah ini agar file `.dump` tidak bernilai 0 byte.

- **Linux / macOS (Bash):**
  ```bash
  # 1. Buat backup ke backups/capstone-<timestamp>.dump
  ./scripts/backup-postgres.sh

  # 2. Validasi restore ke database isolasi capstone_restore_validation
  LATEST_DUMP=$(ls -t backups/*.dump | head -n 1)
  ./scripts/restore-postgres-validation.sh "$LATEST_DUMP"
  ```

- **Windows (PowerShell — aman untuk PowerShell 5.1 maupun 7+):**
  ```powershell
  # 1. Buat backup (format custom -Fc) ke folder backups\
  New-Item -ItemType Directory -Force -Path backups | Out-Null
  $dump = "backups\capstone-$(Get-Date -Format 'yyyyMMdd-HHmmss').dump"
  cmd /c "docker exec capstone-postgres pg_dump -U capstone -d capstone -Fc > $dump"
  Get-Item $dump | Select-Object Name, Length   # pastikan Length > 0

  # 2. Validasi restore ke database terpisah capstone_restore_validation
  docker exec capstone-postgres psql -U capstone -d postgres -c "DROP DATABASE IF EXISTS capstone_restore_validation;"
  docker exec capstone-postgres psql -U capstone -d postgres -c "CREATE DATABASE capstone_restore_validation;"
  cmd /c "docker exec -i capstone-postgres pg_restore -U capstone -d capstone_restore_validation --no-owner < $dump"
  docker exec capstone-postgres psql -U capstone -d capstone_restore_validation -c "\dt"
  docker exec capstone-postgres psql -U capstone -d capstone_restore_validation -c "SELECT count(*) AS equipment_count FROM equipment_units;"

  # 3. (Opsional) Bersihkan database uji restore setelah verifikasi selesai
  docker exec capstone-postgres psql -U capstone -d postgres -c "DROP DATABASE IF EXISTS capstone_restore_validation;"
  ```
  Pastikan daftar tabel (`equipment_units`, `inspection_records`, `maintenance_records`, `reservation_items`, `reservations`, `tenants`) muncul dengan `exit code 0` di `capstone_restore_validation`.

> ⚠️ **Jangan commit file `backups/*.dump` ke Git.** Folder `backups/` hanya untuk penyimpanan uji lokal.

---

## Alur Kerja Harian (Kolaborasi Tim & Deploy ke Lab)

1. **Coding Fitur Baru:**
   Edit atau tambahkan kode aplikasi di dalam folder `app/`.
2. **Verifikasi Sebelum Commit:**
   Jalankan `./mvnw -B verify` *(Bash)* atau `.\mvnw.cmd -B verify` *(PowerShell)* di folder `app/`, lalu periksa `git status`.
   > ⚠️ **Kebersihan Git:** Pastikan **tidak** mem-commit perubahan lokal pada `infrastructure/.env`, folder `infrastructure/.runtime/`, maupun file dump di `backups/`. Jika `infrastructure/.env` terlanjur termodifikasi secara lokal, hindari men-stage file tersebut saat `git add`.
   ```bash
   git status
   git add app/ README.md   # atau file spesifik yang memang ingin di-commit
   git commit -m "feat: deskripsi perubahan"
   git push origin main
   ```
3. **Deploy ke Lab Lokal (Gitea CI/CD ➔ Argo CD ➔ Kubernetes):**
   `deploy-lab` mengambil isi folder `app/` dari **commit Git terbaru (`HEAD`)**, jadi pastikan Anda sudah melakukan `git commit` terlebih dahulu:
   ```bash
   # Linux / macOS (Bash)
   ./scripts/deploy-lab.sh
   ```
   ```powershell
   # Windows (PowerShell)
   .\scripts\deploy-lab.ps1
   ```
   Gitea Runner akan otomatis menjalankan test/verify Maven, mem-build image, melakukan scan keamanan dengan Trivy, mem-push image ke registry lokal, meng-update tag di repo `capstone-gitops`, dan Argo CD akan men-deploy pod baru ke Kubernetes.

---

## Menyalakan & Mematikan Lab (Setelah Laptop Restart)

Jika laptop habis dimatikan/restart dan Anda ingin melanjutkan pengujian lab tanpa setup dari awal:

### Menyalakan Kembali
**Linux / macOS (Bash):**
```bash
./scripts/up-core.sh
k3d cluster start capstone
./scripts/start-runner.sh
./scripts/status.sh
```

**Windows (PowerShell):**
```powershell
.\scripts\up-core.ps1
k3d cluster start capstone
.\scripts\start-runner.ps1
.\scripts\status.ps1
```

### Mematikan Sementara (Hemat RAM / Baterai)
*(Berlaku di Bash maupun PowerShell)*
```bash
k3d cluster stop capstone
docker compose --env-file infrastructure/.env -f infrastructure/docker-compose.yml stop
```
*(Data database, commit Gitea, dan konfigurasi kluster tetap tersimpan aman.)*

---

## Troubleshooting Cepat (Testing, Grafana, & Recovery)

- **Endpoint `/api/*` mengembalikan `401 Unauthorized` atau `403 Forbidden`:**
  Pastikan header `X-Tenant-Id` (`tenant-a` / `tenant-b`) dan `X-User-Role` (`user` / `operator` / `admin`) diisi pada Swagger UI maupun request API. Role `user` akan ditolak (`403`) jika memanggil endpoint operasional seperti `/api/reports/summary` atau `/api/reservations/{id}/approve`.
- **Grafik Grafana (`HTTP Request Rate` / `p95 Latency`) tidak naik setelah mengetes API:**
  1. Pastikan Anda menembak Kubernetes Ingress di **`http://capstone.localhost:8088`** (bukan `quarkus:dev` port `8080`).
  2. Cek target `capstone-app` di `http://127.0.0.1:9090/targets` berstatus `UP`.
  3. Kirim lebih dari 1 request ke endpoint yang sama dan tunggu **15–20 detik** (minimal 2 siklus scrape Prometheus `10s`), lalu set Grafana ke `Last 5 minutes`.
- **Skrip `backup-postgres.ps1` menghasilkan file 0 byte / `restore-postgres-validation.ps1` error di Windows PowerShell 5.1:**
  Gunakan blok perintah `cmd /c "docker exec ..."` manual pada Bagian **3.B Uji Recovery & Backup/Restore Validation** di atas yang sudah diuji langsung di Windows PowerShell 5.1.

---

## Daftar URL Layanan & Kredensial Default

| Layanan | URL Akses | Username | Password / Keterangan |
| :--- | :--- | :--- | :--- |
| **Aplikasi GearRent (Ingress k3d)** | `http://capstone.localhost:8088/` | - | Endpoint utama Kubernetes (`Swagger UI`: `/q/swagger-ui`, `Health`: `/q/health`, `Metrics`: `/q/metrics`) |
| **Gitea (Git & Actions CI)** | `http://127.0.0.1:3001` | `student` | `student` *(Admin: `capstone-admin` / `capstone-admin`)* |
| **Keycloak (OIDC / IAM)** | `http://127.0.0.1:8081` | `admin` | `admin` |
| **PostgreSQL** | `127.0.0.1:5432` | `capstone` | `capstone` *(Database: `capstone`)* |
| **OCI Docker Registry** | `http://127.0.0.1:5001/v2/_catalog` | - | Registry container lokal |
| **Argo CD UI** | `https://127.0.0.1:8082` | `admin` | Jalankan `./scripts/argocd-ui.sh` (Linux/macOS) atau `.\scripts\argocd-ui.ps1` (Windows) dulu.<br>Cek password dengan perintah di bawah. |
| **Prometheus** *(M5 Observability)* | `http://127.0.0.1:9090` | - | Nyalakan dengan `./scripts/up-observability.sh` / `.ps1` (`Targets`: `/targets`) |
| **Grafana** *(M5 Observability)* | `http://127.0.0.1:3000` | `admin` | `admin` *(Dashboard SLO: `/d/gearrent-overview/gearrent-service-overview-and-slo`)* |

**Perintah untuk melihat password admin Argo CD:**

- **Linux / macOS (Bash):**
  ```bash
  kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
  ```
- **Windows (PowerShell):**
  ```powershell
  [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String((kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}")))
  ```
