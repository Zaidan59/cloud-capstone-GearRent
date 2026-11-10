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
├── app/                  # Source code backend Quarkus (Java), Dockerfile, & CI workflow (.gitea/)
├── gitops/               # Manifest Kubernetes (Kustomize base & overlays) untuk Argo CD
├── infrastructure/       # Docker Compose (Gitea, Runner, PostgreSQL, Keycloak, Registry, Prometheus, Grafana)
├── scripts/              # Skrip otomasi operasional lab (Bash & PowerShell)
├── smoke-test/           # Utilitas uji coba koneksi registry & kluster k3d
├── templates/            # Template referensi bawaan infrastruktur
└── guides/               # Dokumen panduan infrastruktur & proposal capstone
```

---

## Cara 1: Menjalankan Aplikasi Saja (Mode Coding / Live Reload)

Jika Anda baru melakukan `git clone` dan hanya ingin menulis kode fitur di folder `app/` secara cepat tanpa perlu menyalakan kluster Kubernetes penuh:

**Prasyarat:** JDK 21+ (atau sesuai konfigurasi `pom.xml`).

```bash
cd app

# Linux / macOS
./mvnw quarkus:dev

# Windows (PowerShell)
.\mvnw.cmd quarkus:dev
```

- **Endpoint API:** `http://localhost:8080/` dan `http://localhost:8080/hello`
- **Health Check:** `http://localhost:8080/q/health`
- **Quarkus Dev UI:** `http://localhost:8080/q/dev/`

*(Setiap perubahan kode di `app/src/` akan langsung ter-reload otomatis saat disimpan. Tekan `Ctrl+C` untuk berhenti).*

---

## Cara 2: Setup Ekosistem Cloud-Native Penuh (Pertama Kali Clone)

Gunakan panduan ini jika Anda ingin menjalankan simulasi lengkap (**Gitea + CI Runner + Local Registry + PostgreSQL + Keycloak + Kubernetes k3d + Argo CD**) di laptop Anda.

### Prasyarat Tools
Pastikan sudah ter-install:
- **Docker** & **Docker Compose** (kondisi menyala/running)
- **Git**, **curl**, **kubectl**, **k3d**
- **jq** *(khusus pengguna Linux/macOS)*

### Langkah Setup (Urut dari 1 s.d. 9)

#### A. Pengguna Linux / macOS (Bash)
```bash
# 1. Buat file environment lokal
cp -n infrastructure/.env.example infrastructure/.env

# 2. Cek kesiapan tools & port
./scripts/doctor.sh

# 3. Nyalakan layanan inti (Gitea, Postgres, Keycloak, Registry)
./scripts/up-core.sh

# 4. Inisialisasi akun Gitea & token runner
./scripts/bootstrap-gitea.sh

# 5. Bangun & jalankan Gitea CI Runner
./scripts/start-runner.sh

# 6. Buat kluster Kubernetes k3d
./scripts/create-cluster.sh

# 7. Buat Kubernetes Secret untuk password database di namespace capstone
kubectl create namespace capstone --dry-run=client -o yaml | kubectl apply -f -
kubectl -n capstone create secret generic capstone-app-secret \
  --from-literal=DB_PASSWORD=capstone \
  --dry-run=client -o yaml | kubectl apply -f -

# 8. Push kode app/ dan gitops/ ke Gitea lokal (otomatis memicu pipeline CI pertama)
./scripts/bootstrap-project.sh

# 9. Install Argo CD & hubungkan ke repositori GitOps lokal
./scripts/install-argocd.sh
```

#### B. Pengguna Windows (PowerShell)
```powershell
# 1. Buat file environment lokal
Copy-Item infrastructure\.env.example infrastructure\.env

# 2. Cek kesiapan tools & port
.\scripts\doctor.ps1

# 3. Nyalakan layanan inti
.\scripts\up-core.ps1

# 4. Inisialisasi akun Gitea & token runner
.\scripts\bootstrap-gitea.ps1

# 5. Bangun & jalankan Gitea CI Runner
.\scripts\start-runner.ps1

# 6. Buat kluster Kubernetes k3d
.\scripts\create-cluster.ps1

# 7. Buat Kubernetes Secret untuk password database
kubectl create namespace capstone --dry-run=client -o yaml | kubectl apply -f -
kubectl -n capstone create secret generic capstone-app-secret `
  --from-literal=DB_PASSWORD=capstone `
  --dry-run=client -o yaml | kubectl apply -f -

# 8. Push kode app/ dan gitops/ ke Gitea lokal
.\scripts\bootstrap-project.ps1

# 9. Install Argo CD & hubungkan ke repositori GitOps lokal
.\scripts\install-argocd.ps1
```

---

## Alur Kerja Harian (Kolaborasi Tim & Deploy ke Lab)

1. **Coding Fitur Baru:**
   Edit atau tambahkan kode aplikasi di dalam folder `app/`.
2. **Simpan & Bagikan ke Tim (GitHub):**
   ```bash
   git add .
   git commit -m "feat: deskripsi perubahan"
   git push origin main
   ```
3. **Deploy ke Lab Lokal (Gitea CI/CD ➔ Argo CD ➔ Kubernetes):**
   Setelah melakukan `git commit`, jalankan skrip berikut untuk mengirim isi `app/` ke Gitea lokal:
   ```bash
   # Linux / macOS
   ./scripts/deploy-lab.sh

   # Windows (PowerShell)
   .\scripts\deploy-lab.ps1
   ```
   Gitea Runner akan otomatis mem-build image, melakukan scan keamanan dengan Trivy, mem-push image ke registry lokal, meng-update tag di repo GitOps, dan Argo CD akan men-deploy pod baru ke Kubernetes.

---

## Menyalakan & Mematikan Lab (Setelah Laptop Restart)

Jika laptop habis dimatikan/restart dan Anda ingin melanjutkan pengujian lab tanpa setup dari awal:

### Menyalakan Kembali
```bash
# 1. Nyalakan container utama
./scripts/up-core.sh          # atau .\scripts\up-core.ps1

# 2. Nyalakan kluster Kubernetes k3d
k3d cluster start capstone

# 3. Nyalakan Gitea Runner
./scripts/start-runner.sh     # atau .\scripts\start-runner.ps1

# 4. Cek status keseluruhan
./scripts/status.sh           # atau .\scripts\status.ps1
```

### Mematikan Sementara (Hemat RAM / Baterai)
```bash
k3d cluster stop capstone
docker compose --env-file infrastructure/.env -f infrastructure/docker-compose.yml stop
```
*(Data database, commit Gitea, dan konfigurasi kluster tetap tersimpan aman).*

---

## Daftar URL Layanan & Kredensial Default

| Layanan | URL Akses | Username | Password / Keterangan |
| :--- | :--- | :--- | :--- |
| **Aplikasi GearRent (Ingress k3d)** | `http://capstone.localhost:8088/` | - | Endpoint utama di Kubernetes |
| **Gitea (Git & Actions CI)** | `http://127.0.0.1:3001` | `student` | `student` *(Admin: `capstone-admin` / `capstone-admin`)* |
| **Keycloak (OIDC / IAM)** | `http://127.0.0.1:8081` | `admin` | `admin` |
| **PostgreSQL** | `127.0.0.1:5432` | `capstone` | `capstone` *(Database: `capstone`)* |
| **OCI Docker Registry** | `http://127.0.0.1:5001/v2/_catalog` | - | Registry container lokal |
| **Argo CD UI** | `https://127.0.0.1:8082` | `admin` | Jalankan `./scripts/argocd-ui.sh` dulu.<br>Cek password dengan perintah di bawah. |
| **Prometheus** *(Opsional M5)* | `http://127.0.0.1:9090` | - | Nyalakan dengan `./scripts/up-observability.sh` |
| **Grafana** *(Opsional M5)* | `http://127.0.0.1:3000` | `admin` | `admin` |

**Perintah untuk melihat password admin Argo CD:**
```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
```
