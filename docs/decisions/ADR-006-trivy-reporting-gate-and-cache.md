# ADR-006: Trivy Scan Reporting Gate (`--exit-code 0`) dan Persistent Cache Volume

- **Status:** Accepted
- **Tanggal:** 2026-11-26
- **Owner:** Tim Kelompok 2 (A02) — GearRent
- **Milestone:** M4 (Delivery Automation), M6 (Security)

## Context
Pipeline CI di `app/.gitea/workflows/ci.yml` memindai image container memakai `aquasec/trivy:0.76.0` dengan filter `--severity HIGH,CRITICAL`. Pada eksekusi awal tanpa volume cache, Trivy mengunduh `trivy-db` (`~136 MiB`) dan `trivy-java-db` (`~958 MiB`) dari awal pada setiap run sehingga gagal *timeout* (`context deadline exceeded`) saat koneksi internet lambat. Selain itu, tim perlu menentukan apakah temuan *vulnerability* pada base image langsung memblokir pipeline atau dinilai risikonya terlebih dahulu.

## Options Considered
1. **Blocking gate (`--exit-code 1`) tanpa cache DB:**
   - *Pro:* Otomatis menghentikan image dengan temuan `HIGH`/`CRITICAL`.
   - *Kontra:* Setiap run mengunduh ulang ~1,1 GB DB dan rentan gagal; temuan OS package di base image pihak ketiga langsung memblokir deploy meski tidak dipakai saat runtime.
2. **Reporting gate (`--exit-code 0`) + volume `trivy-cache` + `--timeout 15m` (Dipilih):**
   - *Pro:* DB kerentanan di-cache permanen di host (`-v trivy-cache:/root/.cache`), timeout 15 menit mengamankan pengunduhan perdana, dan setiap run menghasilkan laporan CVE untuk dianalisis dan diprioritaskan oleh tim.
   - *Kontra:* Temuan baru `CRITICAL` di masa depan tidak otomatis menggagalkan deploy sebelum kebijakan blocking diaktifkan.

## Decision
Menggunakan `aquasec/trivy:0.76.0` dengan `-v trivy-cache:/root/.cache`, `--timeout 15m`, `--severity HIGH,CRITICAL`, dan `--exit-code 0` sebagai *reporting gate* awal sambil mewajibkan interpretasi risiko setiap hasil scan.

## Consequences & Limitations
- **Observasi Aktual (CI Run #8, image `abfcb7462763`):** Scan mencatat `Total: 2 (HIGH: 2, CRITICAL: 0)` — keduanya `CVE-2026-109518` pada paket OS **`apt` dan `libapt-pkg7.0` (`3.2.0` → fixed `3.2.1ubuntu0.1`)** di base image `ubuntu 26.04` (`eclipse-temurin:21-jre`), sedangkan seluruh dependency JAR aplikasi Quarkus mencatat **`0` vulnerability**.
- **Interpretasi & Mitigasi:** Risiko runtime dinilai rendah karena container berjalan sebagai non-root (`USER 1001`) dan tidak menjalankan `apt` saat runtime; tindak lanjut yang disepakati adalah memperbarui base image `eclipse-temurin:21-jre` atau beralih ke image minimal/distroless, lalu menaikkan kebijakan ke `--exit-code 1` untuk `CRITICAL` setelah *exception process* ditetapkan.

## Traceability / Evidence
- `app/.gitea/workflows/ci.yml` (baris 48–54)
- `app/Dockerfile` (`eclipse-temurin:21-jre`, `USER 1001`)
- Commit `bf69f81` (*fix(ci): cache trivy DB in docker volume and increase scan timeout*)
