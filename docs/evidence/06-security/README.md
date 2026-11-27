# 06 — Security: IAM OIDC, Secrets & Vulnerability Scan (M6)

## Evidence Metadata

| Field | Detail |
| :--- | :--- |
| **Scope** | Autentikasi Keycloak OIDC Bearer JWT (`trust-headers=false`), otorisasi berbasis role (`user`, `operator`, `admin`), isolasi tenant via JWT `groups` + anti-spoofing, pemisahan Secret dari Git, serta interpretasi hasil scan Trivy |
| **Owner** | Muhammad Zaidan (`2410512043`) & Tim Kelompok 2 |
| **Source / Version** | `app/src/main/java/id/ac/upnvj/gearrent/security/AccessContextResolver.java`, `OpenApiHeaderFilter.java`, `OidcSecurityTest.java`, `TenantIsolationAndRoleTest.java`, `infrastructure/keycloak/realm-capstone.json`, `gitops/base/secret.example.yaml`, [`ADR-005`](../../decisions/ADR-005-keycloak-oidc-jwt.md), [`ADR-006`](../../decisions/ADR-006-trivy-reporting-gate-and-cache.md) |
| **Timestamp** | Implementasi M6: 26 November 2026 (commit `45f1ae6` / PR #1 `8431473`); verifikasi test otomatis ulang: 27 November 2026 |
| **Method** | 1) Eksekusi otomatis `cd app && ./mvnw -B verify` (`OidcSecurityTest` memakai `OidcWiremockTestResource` + `TenantIsolationAndRoleTest`); 2) Tinjauan manifest `gitops/` & `.gitignore`; 3) Scan container Trivy `0.76.0` di Gitea CI (`--severity HIGH,CRITICAL --exit-code 0`) |
| **Observation** | - **Automated tests:** `OidcSecurityTest` (**2/2 lulus**) dan `TenantIsolationAndRoleTest` (**3/3 lulus**) membuktikan: request tanpa token, header spoof `X-Tenant-Id`/`X-User-Role`, atau JWT invalid ditolak **`401`**; user yang memaksa `X-Tenant-Id` tenant lain, user biasa memanggil `/api/reports/summary`, atau operator tenant B mengakses unit tenant A ditolak **`403`**; hanya admin multi-tenant (`["/tenant-a", "/tenant-b"]`) yang boleh memilih tenant terdaftar.\n- **Secret handling:** `gitops/base/secret.example.yaml` hanya berisi placeholder `CHANGE-ME-LOCALLY` dan tidak masuk `kustomization.yaml`; `.gitignore` mengecualikan `.env`, `.runtime/`, dan `backups/`.\n- **Trivy scan (`ADR-006`, Run #8 `abfcb7462763`):** `Total: 2 (HIGH: 2, CRITICAL: 0)` yaitu `CVE-2026-109518` pada OS package `apt` dan `libapt-pkg7.0` di base image `eclipse-temurin:21-jre`, sedangkan JAR aplikasi Quarkus mencatat `0` CVE |
| **Interpretation** | Kontrol identitas tidak lagi mempercayai header klien yang mudah dipalsukan. Risiko CVE pada `apt` dinilai rendah saat runtime karena container berjalan non-root (`USER 1001`) dan tidak memanggil `apt` saat melayani request |
| **Limitation** | 1) `quarkus.oidc.token.issuer=any` diaktifkan khusus menjembatani split-horizon lokal (`keycloak:8080` vs `127.0.0.1:8081`) dan wajib dikunci ke domain HTTPS tunggal di produksi (`ADR-005`); 2) Trivy menggunakan `--exit-code 0` (reporting gate), belum memblokir build otomatis bila ada temuan baru; 3) Folder ini **belum menyimpan capture screenshot/log terminal live** dari kluster `k3d` saat demo |
| **Decision Supported** | Menetapkan Keycloak OIDC + `groups` claim sebagai sumber kebenaran tenant (`ADR-005`) dan memprioritaskan pembaruan base image daripada memblokir CI tanpa pengecualian (`ADR-006`) |

## Checklist Bukti Live yang Masih Perlu Ditambahkan ke Folder Ini
- [ ] Screenshot/log uji `curl` live di `http://capstone.localhost:8088` (sesuai `TESTING.md` Bagian 6.A: `401`, `200`, `403` RBAC, `403` anti-spoofing, `403` lintas tenant) — **pastikan string token JWT disensor/redaksi**.
- [ ] Output `kubectl -n capstone get secret capstone-app-secret` (hanya metadata/key, **jangan** tampilkan nilai base64/password).
- [ ] Log output atau screenshot Trivy scan terbaru dari Gitea CI / Docker CLI.
