# TESTING.md — Panduan Lengkap Pengujian GearRent Cloud Capstone

Dokumen ini disusun berurutan dari pengujian lokal, verifikasi platform & GitOps, uji API & security, observability (Prometheus/Grafana), reliability/recovery, hingga pengukuran kapasitas.

> **Catatan Port & Shell:**
> - **Kubernetes (`k3d` Ingress):** `http://capstone.localhost:8088` *(dipakai untuk uji integrasi end-to-end & direkam Prometheus/Grafana)*.
> - **Mode Coding Lokal (`quarkus:dev`):** `http://localhost:8080` *(tidak di-scrape Prometheus)*.
> - **Pengguna Fish shell:** tersedia blok khusus **Fish** di bawah; atau jalankan `bash` terlebih dahulu jika ingin langsung menjalankan seluruh blok `bash`.

---

## 1. Automated Test Suite (`./mvnw -B verify`)

Suite otomatis berjalan dengan H2 in-memory (`%test`) dan random HTTP test port (`0`).

```bash
# Linux / macOS (Bash & Fish)
cd app && ./mvnw -B verify
```

```powershell
# Windows PowerShell (pastikan JAVA_HOME mengarah ke JDK 21+)
cd app; .\mvnw.cmd -B verify
```

**Cakupan 11 Test (`Tests run: 11, Failures: 0, Errors: 0`):**
- `GreetingResourceTest` (1): Endpoint dasar aplikasi.
- `ReservationWorkflowTest` (1): Siklus lengkap reservasi → approve → handover → upload/download foto inspeksi → return → late return / damage → maintenance resolve.
- `AvailabilityAndConcurrencyTest` (3): Cek ketersediaan tanggal, penolakan double booking, dan deadlock-safe multi-unit locking (`PESSIMISTIC_WRITE`).
- `TenantIsolationAndRoleTest` (3): Isolasi tenant dan RBAC pada mode header-trust (`%test`).
- `ObservabilityMetricsTest` (1): Validasi metrik `/q/metrics` (`http_server_requests_seconds_bucket` & `agroal_*`).
- `OidcSecurityTest` (2): Validasi JWT OIDC (`gearrent.auth.trust-headers=false`), penolakan spoofed headers (`401`/`403`), isolasi antar-operator lintas tenant, dan multi-tenant admin via `OidcWiremockTestResource`.

---

## 2. Verifikasi Platform Kubernetes & GitOps (M3 & M4)

Sebelum mengetes endpoint live, pastikan kluster `k3d`, pod aplikasi, dan sinkronisasi Argo CD sudah sehat:

```bash
kubectl get nodes -o wide
kubectl -n capstone get all,ingress
kubectl -n argocd get applications.argoproj.io
curl -sf http://capstone.localhost:8088/q/health/live
curl -sf http://capstone.localhost:8088/q/health/ready
```

Pastikan `capstone-app` di Argo CD berstatus **`Synced` & `Healthy`**, pod `1/1 Running`, dan `/q/health/ready` mengembalikan `"status": "UP"`.

---

## 3. Ambil Token Keycloak (Prasyarat Uji API `:8088`)

Pada runtime (`gearrent.auth.trust-headers=false`), seluruh endpoint `/api/*` wajib memakai **Bearer JWT** dari Keycloak (`http://127.0.0.1:8081`, realm `capstone`, client `capstone-app`).

| Username | Password | Role | Tenant Group |
| :--- | :--- | :--- | :--- |
| `usera` | `usera` | `user` | `tenant-a` |
| `userb` | `userb` | `user` | `tenant-b` |
| `operator` | `operator` | `operator` | `tenant-a` |
| `operatorb` | `operatorb` | `operator` | `tenant-b` |
| `appadmin` | `appadmin` | `admin` | `tenant-a`, `tenant-b` |

**Linux / macOS (Bash):**
```bash
KC="http://127.0.0.1:8081/realms/capstone/protocol/openid-connect/token"
get_token() {
  curl -fsS -X POST "$KC" -d "client_id=capstone-app" -d "grant_type=password" \
    -d "username=$1" -d "password=$1" | jq -r '.access_token'
}

TOK_USERA=$(get_token usera)
TOK_USERB=$(get_token userb)
TOK_OP_A=$(get_token operator)
TOK_OP_B=$(get_token operatorb)
TOK_ADMIN=$(get_token appadmin)

# Token disimpan ke variabel; tampilkan jika ingin disalin ke Swagger UI:
echo "$TOK_USERA"
echo "$TOK_OP_A"
```

**Linux / macOS (Fish shell):**
```fish
set KC "http://127.0.0.1:8081/realms/capstone/protocol/openid-connect/token"
function get_token
  curl -fsS -X POST "$KC" -d "client_id=capstone-app" -d "grant_type=password" \
    -d "username=$argv[1]" -d "password=$argv[1]" | jq -r '.access_token'
end

set TOK_USERA (get_token usera)
set TOK_USERB (get_token userb)
set TOK_OP_A (get_token operator)
set TOK_OP_B (get_token operatorb)
set TOK_ADMIN (get_token appadmin)

# Token disimpan ke variabel; tampilkan jika ingin disalin ke Swagger UI:
echo $TOK_USERA
echo $TOK_OP_A
```

**Windows (PowerShell):**
```powershell
function Get-KeycloakToken($User) {
  $resp = Invoke-RestMethod -Method Post `
    -Uri "http://127.0.0.1:8081/realms/capstone/protocol/openid-connect/token" `
    -Body @{ client_id="capstone-app"; grant_type="password"; username=$User; password=$User }
  return $resp.access_token
}

$TokUserA = Get-KeycloakToken "usera"
$TokUserB = Get-KeycloakToken "userb"
$TokOpA   = Get-KeycloakToken "operator"
$TokOpB   = Get-KeycloakToken "operatorb"
$TokAdmin = Get-KeycloakToken "appadmin"

# Token disimpan ke variabel; tampilkan jika ingin disalin ke Swagger UI:
$TokUserA
$TokOpA
```

---

## 4. Uji Workflow Bisnis End-to-End (M2)

Menguji alur bisnis inti: cek unit tersedia → buat reservasi → approve → handover → return → ringkasan laporan `tenant-a`.

**Langkah 1 & 2 — Cek unit & buat reservasi (`usera`):**
```bash
# Bash: gunakan RES_ID=$(...) | Fish: ganti baris set dengan `set RES_ID (...)`
curl -sf -H "Authorization: Bearer $TOK_USERA" \
  "http://capstone.localhost:8088/api/equipments/available?startDate=2026-12-10&endDate=2026-12-12"

RES_ID=$(curl -sf -X POST "http://capstone.localhost:8088/api/reservations" \
  -H "Authorization: Bearer $TOK_USERA" \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Budi","customerEmail":"budi@example.com","equipmentIds":[1],"startDate":"2026-12-10","endDate":"2026-12-12","notes":"Liputan event"}' \
  | jq -r '.id')
echo "Created reservation ID: $RES_ID"
```

```fish
# Jika memakai Fish shell, pakai baris ini untuk menyimpan RES_ID:
set RES_ID (curl -sf -X POST "http://capstone.localhost:8088/api/reservations" \
  -H "Authorization: Bearer $TOK_USERA" \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Budi","customerEmail":"budi@example.com","equipmentIds":[1],"startDate":"2026-12-10","endDate":"2026-12-12","notes":"Liputan event"}' \
  | jq -r '.id')
echo "Created reservation ID: $RES_ID"
```

**Langkah 3 & 4 — Approve, handover, return, & laporan (`operator`, kompatibel Bash & Fish):**
```bash
curl -sf -X PUT -H "Authorization: Bearer $TOK_OP_A" "http://capstone.localhost:8088/api/reservations/$RES_ID/approve"
curl -sf -X POST -H "Authorization: Bearer $TOK_OP_A" -H "Content-Type: application/json" \
  -d '{"accessoriesComplete":true,"conditionNotes":"Unit normal saat serah terima"}' \
  "http://capstone.localhost:8088/api/reservations/$RES_ID/handover"
curl -sf -X POST -H "Authorization: Bearer $TOK_OP_A" -H "Content-Type: application/json" \
  -d '{"actualReturnDate":"2026-12-12","hasDamage":false,"accessoriesComplete":true,"conditionNotes":"Kembali lengkap dan baik"}' \
  "http://capstone.localhost:8088/api/reservations/$RES_ID/return"
curl -sf -H "Authorization: Bearer $TOK_OP_A" "http://capstone.localhost:8088/api/reports/summary" | jq .
```

---

## 5. Uji Observability, Prometheus, & Dashboard Grafana SLO (M5)

### A. Nyalakan Stack & Cek Metrik Mentah
```bash
curl -sf http://capstone.localhost:8088/q/metrics | grep -E "http_server_requests_seconds|agroal_" | head -n 20
./scripts/up-observability.sh   # Windows: .\scripts\up-observability.ps1
```
Buka `http://127.0.0.1:9090/targets` → pastikan job **`capstone-app`** (`http://k3d-capstone-serverlb:80/q/metrics`, interval `10s`) berstatus **`UP`**.

### B. Generate Traffic ke `:8088` untuk Menguji Panel Grafana
Micrometer baru mencatat route HTTP setelah ada request masuk, dan query `rate()`/`increase()` Prometheus butuh minimal **2 siklus scrape (`>= 20 detik`)** agar grafik muncul. Kirim traffic campuran (`200` + `403`) ke port **`8088`**:

**Linux / macOS (Bash):**
```bash
for i in $(seq 1 15); do
  curl -sf -H "Authorization: Bearer $TOK_OP_A" http://capstone.localhost:8088/api/equipments > /dev/null
  curl -sf -H "Authorization: Bearer $TOK_OP_A" http://capstone.localhost:8088/api/reports/summary > /dev/null
  curl -s -o /dev/null -H "Authorization: Bearer $TOK_USERA" http://capstone.localhost:8088/api/reports/summary
  sleep 2
done
```

**Linux / macOS (Fish shell):**
```fish
for i in (seq 1 15)
  curl -sf -H "Authorization: Bearer $TOK_OP_A" http://capstone.localhost:8088/api/equipments > /dev/null
  curl -sf -H "Authorization: Bearer $TOK_OP_A" http://capstone.localhost:8088/api/reports/summary > /dev/null
  curl -s -o /dev/null -H "Authorization: Bearer $TOK_USERA" http://capstone.localhost:8088/api/reports/summary
  sleep 2
end
```

**Windows (PowerShell):**
```powershell
1..15 | ForEach-Object {
  Invoke-RestMethod -Uri "http://capstone.localhost:8088/api/equipments" -Headers @{ Authorization = "Bearer $TokOpA" } | Out-Null
  Invoke-RestMethod -Uri "http://capstone.localhost:8088/api/reports/summary" -Headers @{ Authorization = "Bearer $TokOpA" } | Out-Null
  curl.exe -s -o NUL -H "Authorization: Bearer $TokUserA" http://capstone.localhost:8088/api/reports/summary
  Start-Sleep -Seconds 2
}
```

### C. Verifikasi Panel Dashboard Grafana (`GearRent - Service Overview & SLO`)
1. Buka `http://127.0.0.1:3000/d/gearrent-overview/gearrent-service-overview-and-slo` (login `admin`/`admin`).
2. Atur time range ke **`Last 5 minutes`** dan auto-refresh **`5s`**.
3. Periksa seluruh panel utama:
   - **Target Health (`up`):** Bernilai **`UP` (1)**.
   - **HTTP Request Rate & HTTP Request Rate by Route & Status:** Kurva `/api/equipments` (`200`), `/api/reports/summary` (`200` & `403`) naik sesuai traffic.
   - **5xx Error Rate (`SLO < 1%`):** Bernilai `0%` (hijau).
   - **p95 Latency (`SLO <= 2s`) & Request Latency Percentiles (`p50 & p95`):** Terukur di bawah batas `2s`.
   - **HTTP Requests by Outcome (5m window):** Pemisahan `SUCCESS` (`2xx`) dan `CLIENT_ERROR` (`4xx` dari uji `403`).
   - **Runtime & DB Panels:** `DB Active Connections` & `PostgreSQL Agroal Connection Pool`, `JVM Heap Memory`, `CPU Usage (%)`, dan `Vert.x Worker Pool` menampilkan metrik aktif.

> **Jika Grafik Grafana Masih `No data` / `0`:** pastikan traffic dikirim ke port **`8088`** (bukan `8080`), target Prometheus **`UP`**, tunggu >= 20 detik, dan rentang waktu di **`Last 5 minutes`**.

---

## 6. Uji Security, IAM OIDC, & Vulnerability Scan (M6)

### A. Uji Positif & Negatif via `curl` (kompatibel Bash & Fish)
```bash
# 1. Negatif (401) — Tanpa token, header spoof tanpa token, dan token palsu
curl -s -o /dev/null -w "no-token: %{http_code}\n" http://capstone.localhost:8088/api/equipments
curl -s -o /dev/null -w "spoof-header: %{http_code}\n" -H "X-Tenant-Id: tenant-a" -H "X-User-Role: admin" http://capstone.localhost:8088/api/equipments
curl -s -o /dev/null -w "bad-jwt: %{http_code}\n" -H "Authorization: Bearer invalid.jwt.token" http://capstone.localhost:8088/api/equipments

# 2. Positif (200) — Customer tenant-a membaca daftar unit tenant-a
curl -s -H "Authorization: Bearer $TOK_USERA" http://capstone.localhost:8088/api/equipments

# 3. Negatif RBAC (403) — Role user mengakses endpoint operator/admin
curl -s -o /dev/null -w "user-report: %{http_code}\n" -H "Authorization: Bearer $TOK_USERA" http://capstone.localhost:8088/api/reports/summary

# 4. Negatif Anti-Spoofing (403) — usera memaksa header X-Tenant-Id: tenant-b
curl -s -o /dev/null -w "spoof-tenant: %{http_code}\n" -H "Authorization: Bearer $TOK_USERA" -H "X-Tenant-Id: tenant-b" http://capstone.localhost:8088/api/equipments

# 5. Negatif Lintas Tenant (403) — operatorb (tenant-b) mengakses aset ID 1 (tenant-a)
curl -s -o /dev/null -w "cross-tenant: %{http_code}\n" -H "Authorization: Bearer $TOK_OP_B" http://capstone.localhost:8088/api/equipments/1

# 6. Positif Multi-Tenant Admin (200) & Negatif Tenant Tidak Terdaftar (403)
curl -s -H "Authorization: Bearer $TOK_ADMIN" -H "X-Tenant-Id: tenant-b" http://capstone.localhost:8088/api/reports/summary
curl -s -o /dev/null -w "admin-invalid-tenant: %{http_code}\n" -H "Authorization: Bearer $TOK_ADMIN" -H "X-Tenant-Id: tenant-c" http://capstone.localhost:8088/api/reports/summary
```

### B. Uji Visual via Swagger UI
1. Buka `http://capstone.localhost:8088/q/swagger-ui/`.
2. Klik **Authorize** (`bearerAuth`), tempel token `$TOK_USERA` atau `$TOK_OP_A`, lalu **Authorize** → **Close**.
3. Jalankan `GET /api/equipments` (harus `200 OK`) dan coba `GET /api/reports/summary` memakai token `usera` (harus `403 Forbidden`).

### C. Verifikasi Secret & Vulnerability Scan (Trivy)
```bash
# Pastikan Secret DB ada di cluster (tanpa menampilkan isi password di evidence)
kubectl -n capstone get secret capstone-app-secret

# Scan image lokal secara manual (opsional; otomatis berjalan di Gitea CI)
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v trivy-cache:/root/.cache \
  aquasec/trivy:0.76.0 image --timeout 15m --severity HIGH,CRITICAL --exit-code 0 \
  localhost:5001/student/capstone-app:latest
```

---

## 7. Uji Reliability, Self-Healing, & Backup/Restore (M7)

### A. Pod Failure & Self-Healing Kubernetes
```bash
date
kubectl -n capstone delete pod -l app=capstone-app
kubectl -n capstone wait --for=condition=Ready pod -l app=capstone-app --timeout=120s
date
curl -sf http://capstone.localhost:8088/q/health/ready
```

### B. Backup & Validasi Restore PostgreSQL (Terisolasi)
```bash
# 1. Cek jumlah data sebelum backup
docker exec capstone-postgres psql -U capstone -d capstone -c "SELECT count(*) AS equipments FROM equipment_units; SELECT count(*) AS reservations FROM reservations;"

# 2. Buat backup & validasi restore ke database capstone_restore_validation
./scripts/backup-postgres.sh
# Bash: LATEST_DUMP=$(ls -t backups/*.dump | head -n 1)
# Fish: set LATEST_DUMP (ls -t backups/*.dump | head -n 1)
ls -lh "$LATEST_DUMP"
./scripts/restore-postgres-validation.sh "$LATEST_DUMP"

# 3. Bandingkan jumlah data di database hasil restore
docker exec capstone-postgres psql -U capstone -d capstone_restore_validation -c "SELECT count(*) AS equipments FROM equipment_units; SELECT count(*) AS reservations FROM reservations;"
```

```powershell
# Backup & validasi restore di Windows PowerShell (5.1 maupun 7+)
.\scripts\backup-postgres.ps1
$latestDump = (Get-ChildItem backups\*.dump | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName
.\scripts\restore-postgres-validation.ps1 $latestDump
```

---

## 8. Pengukuran Kapasitas, Load Test, & FinOps (M8)

Referensi lengkap dan baseline laporan tersedia di [docs/capacity/README.md](docs/capacity/README.md) dan [docs/capacity/REPORT.md](docs/capacity/REPORT.md).

### A. Cek Resource Pod Aktif & Unit Test Perhitungan M8
```bash
# Cek pemakaian CPU/RAM aktual pod vs requests/limits (100m/192Mi – 500m/512Mi)
kubectl -n capstone top pods
kubectl -n capstone describe deploy capstone-app | grep -A 6 "Limits:"

# Jalankan unit test matematika kapasitas & unit-cost (4 tests)
python3 -m unittest discover -s tests -p test_capacity.py -v
```

### B. Load Test & Sampling Metrics API dengan JWT (`scripts/capacity.py`)
Gunakan `--token-file` (disimpan di luar repo, misalnya `/tmp/gearrent-token.txt`) karena runtime terbaru mewajibkan Bearer JWT Keycloak. *(Catatan: access token Keycloak default berlaku 5 menit, jadi perbarui token atau sesuaikan lifespan sebelum menjalankan semua fase.)*

**Linux / macOS (Bash):**
```bash
# 1. Simpan access token operator sementara di /tmp
curl -fsS -X POST "http://127.0.0.1:8081/realms/capstone/protocol/openid-connect/token" \
  -d "client_id=capstone-app" -d "grant_type=password" -d "username=operator" -d "password=operator" \
  | jq -r '.access_token' > /tmp/gearrent-token.txt

# 2. Jalankan 3 fase pengukuran pada kluster k3d-capstone
SRC_SHA=$(git rev-parse HEAD)
OUT_DIR="/tmp/m8-jwt-run"
rm -rf "$OUT_DIR"

python3 scripts/capacity.py --kubeconfig "$HOME/.kube/config" --context k3d-capstone \
  --namespace capstone --deployment capstone-app --selector app=capstone-app \
  --url http://127.0.0.1:8088 --host capstone.localhost --source "$SRC_SHA" \
  --executor "Local k3d JWT test" --token-file /tmp/gearrent-token.txt \
  --workers 0 --seconds 60 --sample-seconds 15 --out "$OUT_DIR/idle"

python3 scripts/capacity.py --kubeconfig "$HOME/.kube/config" --context k3d-capstone \
  --namespace capstone --deployment capstone-app --selector app=capstone-app \
  --url http://127.0.0.1:8088 --host capstone.localhost --source "$SRC_SHA" \
  --executor "Local k3d JWT test" --token-file /tmp/gearrent-token.txt \
  --workers 2 --seconds 120 --sample-seconds 15 --out "$OUT_DIR/load-2"

python3 scripts/capacity.py --kubeconfig "$HOME/.kube/config" --context k3d-capstone \
  --namespace capstone --deployment capstone-app --selector app=capstone-app \
  --url http://127.0.0.1:8088 --host capstone.localhost --source "$SRC_SHA" \
  --executor "Local k3d JWT test" --token-file /tmp/gearrent-token.txt \
  --workers 8 --seconds 120 --sample-seconds 15 --out "$OUT_DIR/load-8"

# 3. Hapus file token sementara setelah selesai
rm -f /tmp/gearrent-token.txt
```

**Linux / macOS (Fish shell):**
```fish
# 1. Simpan access token operator sementara di /tmp
curl -fsS -X POST "http://127.0.0.1:8081/realms/capstone/protocol/openid-connect/token" \
  -d "client_id=capstone-app" -d "grant_type=password" -d "username=operator" -d "password=operator" \
  | jq -r '.access_token' > /tmp/gearrent-token.txt

# 2. Jalankan 3 fase pengukuran pada kluster k3d-capstone
set SRC_SHA (git rev-parse HEAD)
set OUT_DIR "/tmp/m8-jwt-run"
rm -rf "$OUT_DIR"

python3 scripts/capacity.py --kubeconfig "$HOME/.kube/config" --context k3d-capstone \
  --namespace capstone --deployment capstone-app --selector app=capstone-app \
  --url http://127.0.0.1:8088 --host capstone.localhost --source "$SRC_SHA" \
  --executor "Local k3d JWT test" --token-file /tmp/gearrent-token.txt \
  --workers 0 --seconds 60 --sample-seconds 15 --out "$OUT_DIR/idle"

python3 scripts/capacity.py --kubeconfig "$HOME/.kube/config" --context k3d-capstone \
  --namespace capstone --deployment capstone-app --selector app=capstone-app \
  --url http://127.0.0.1:8088 --host capstone.localhost --source "$SRC_SHA" \
  --executor "Local k3d JWT test" --token-file /tmp/gearrent-token.txt \
  --workers 2 --seconds 120 --sample-seconds 15 --out "$OUT_DIR/load-2"

python3 scripts/capacity.py --kubeconfig "$HOME/.kube/config" --context k3d-capstone \
  --namespace capstone --deployment capstone-app --selector app=capstone-app \
  --url http://127.0.0.1:8088 --host capstone.localhost --source "$SRC_SHA" \
  --executor "Local k3d JWT test" --token-file /tmp/gearrent-token.txt \
  --workers 8 --seconds 120 --sample-seconds 15 --out "$OUT_DIR/load-8"

# 3. Hapus file token sementara setelah selesai
rm -f /tmp/gearrent-token.txt
```

### C. Generate Worksheet & Ringkasan Visual (`scripts/capacity_report.py`)
```bash
# Bangun worksheet & JPG dari 3 folder fase (idle, load-2, load-8)
python3 scripts/capacity_report.py --results /tmp/m8-jwt-run \
  --model docs/capacity/cost-assumptions.json \
  --report /tmp/m8-jwt-run/REPORT.md \
  --jpg /tmp/m8-jwt-run/baseline-summary.jpg
```
