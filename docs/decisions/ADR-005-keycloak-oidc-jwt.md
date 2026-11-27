# ADR-005: Migrasi ke Keycloak OIDC Bearer JWT (`trust-headers=false`) & Split-Horizon Issuer

- **Status:** Accepted
- **Tanggal:** 2026-11-26
- **Owner:** Muhammad Zaidan (2410512043) — M6 Security
- **Milestone:** M6 (Security)

## Context
Pada tahap MVP (M2), aplikasi memakai header HTTP (`X-Tenant-Id`, `X-User-Role`, `X-User-Id`) dengan `gearrent.auth.trust-headers=true` agar alur bisnis dan tes isolasi dapat dibangun lebih dulu. Pada M6, header yang dapat dipalsukan (*spoof*) oleh klien harus diganti dengan autentikasi JWT terverifikasi melalui Keycloak `26.8.1`. Selain itu, terdapat perbedaan alamat akses Keycloak antara pod Kubernetes (`http://keycloak:8080/realms/capstone`) dan klien/host laptop (`http://127.0.0.1:8081/realms/capstone`), serta kebutuhan agar test lokal (`./mvnw verify`) tetap cepat tanpa wajib menyalakan container Keycloak eksternal.

## Options Considered
1. **Mempertahankan header trust dan hanya mengandalkan reverse-proxy:**
   - *Pro:* Tidak perlu ubah kode.
   - *Kontra:* Tidak memenuhi kontrol IAM OIDC M6; siapa pun yang dapat menghubungi service/ingress bisa memalsukan `X-User-Role: admin` atau `X-Tenant-Id`.
2. **Integrasi `quarkus-oidc` dengan validasi strict `iss` URL tunggal:**
   - *Pro:* Validasi string `iss` ketat.
   - *Kontra:* Token yang diambil host dari `127.0.0.1:8081` memiliki claim `iss=http://127.0.0.1:8081/realms/capstone`, sehingga ditolak pod `k3d` yang mengambil discovery metadata dari `http://keycloak:8080/realms/capstone`.
3. **Integrasi `quarkus-oidc` (`trust-headers=false`) + `token.issuer=any` + `groups` claim mapping (Dipilih):**
   - *Pro:* Tanda tangan RSA JWT dan masa berlaku (`exp`, 300 detik) tetap diverifikasi terhadap JWKS Keycloak; `AccessContextResolver` membaca `preferred_username`, `realm_access.roles`, dan `groups` (`/tenant-a`, `/tenant-b`) dari JWT serta menolak `X-Tenant-Id` spoofing (`403`).
   - *Kontra:* `quarkus.oidc.token.issuer=any` menonaktifkan pencocokan string `iss` di lingkungan split-horizon lokal (pada produksi HTTPS dengan satu domain publik, `issuer` harus dikunci ke domain resmi).

## Decision
1. Menambahkan `io.quarkus:quarkus-oidc` (`application-type=service`, `gearrent.auth.trust-headers=false` pada runtime) dan mengatur `quarkus.oidc.token.issuer=any` untuk topologi lab lokal.
2. Melengkapi profil user di `infrastructure/keycloak/realm-capstone.json` (`emailVerified: true`, `requiredActions: []`), menambahkan *protocol mapper* `groups` pada client `capstone-app`, menetapkan group tenant untuk `operator` (`/tenant-a`), menambah `operatorb` (`/tenant-b`), dan memberi `appadmin` akses `["/tenant-a", "/tenant-b"]` (dengan `X-Tenant-Id` opsional khusus multi-tenant admin).
3. Memperbarui Swagger UI (`OpenApiHeaderFilter`) agar memakai skema **`bearerAuth`** dan menambahkan `OidcSecurityTest` berbasis `OidcWiremockTestResource` (`%test.quarkus.keycloak.devservices.enabled=false`).

## Consequences & Limitations
- **Positif:** Teruji **11/11** automated tests (`./mvnw -B clean verify`) dan **14/14** uji live di `http://capstone.localhost:8088` (`200`, `401` tanpa token/spoof header/token palsu, `403` lintas role/anti-spoofing/lintas tenant).
- **Limitasi:** Pengaturan `quarkus.oidc.token.issuer=any` hanya ditujukan untuk jembatan DNS lokal `keycloak:8080` vs `127.0.0.1:8081` dan wajib dikembalikan ke validasi issuer eksplisit ketika sudah memakai domain HTTPS tunggal di CloudStack/produksi.

## Traceability / Evidence
- `app/pom.xml`, `app/src/main/resources/application.properties`
- `app/src/main/java/id/ac/upnvj/gearrent/security/AccessContextResolver.java`
- `app/src/main/java/id/ac/upnvj/gearrent/security/OpenApiHeaderFilter.java`
- `app/src/test/java/id/ac/upnvj/gearrent/OidcSecurityTest.java`
- `infrastructure/keycloak/realm-capstone.json`
- Commit `45f1ae6` / PR #1 (`8431473`)
