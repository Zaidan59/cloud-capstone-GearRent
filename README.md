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

## Alur Kerja Harian (Kolaborasi Tim & Deploy ke Lab)

1. **Coding Fitur Baru:**
   Edit atau tambahkan kode aplikasi di dalam folder `app/`.
2. **Simpan & Bagikan ke Tim (GitHub):**
   > Pastikan **tidak** mem-commit file lokal seperti `infrastructure/.env` atau `infrastructure/.runtime/`.
   ```bash
   git add .
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

## Daftar URL Layanan & Kredensial Default

| Layanan | URL Akses | Username | Password / Keterangan |
| :--- | :--- | :--- | :--- |
| **Aplikasi GearRent (Ingress k3d)** | `http://capstone.localhost:8088/` | - | Endpoint utama di Kubernetes (`Swagger UI`: `/q/swagger-ui`) |
| **Gitea (Git & Actions CI)** | `http://127.0.0.1:3001` | `student` | `student` *(Admin: `capstone-admin` / `capstone-admin`)* |
| **Keycloak (OIDC / IAM)** | `http://127.0.0.1:8081` | `admin` | `admin` |
| **PostgreSQL** | `127.0.0.1:5432` | `capstone` | `capstone` *(Database: `capstone`)* |
| **OCI Docker Registry** | `http://127.0.0.1:5001/v2/_catalog` | - | Registry container lokal |
| **Argo CD UI** | `https://127.0.0.1:8082` | `admin` | Jalankan `./scripts/argocd-ui.sh` (Linux/macOS) atau `.\scripts\argocd-ui.ps1` (Windows) dulu.<br>Cek password dengan perintah di bawah. |
| **Prometheus** *(Opsional M5)* | `http://127.0.0.1:9090` | - | Nyalakan dengan `./scripts/up-observability.sh` atau `.\scripts\up-observability.ps1` |
| **Grafana** *(Opsional M5)* | `http://127.0.0.1:3000` | `admin` | `admin` |

**Perintah untuk melihat password admin Argo CD:**

- **Linux / macOS (Bash):**
  ```bash
  kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
  ```
- **Windows (PowerShell):**
  ```powershell
  [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String((kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}")))
  ```
