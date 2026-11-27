# ADR-004: Pemisahan App CI (Gitea Actions) dan GitOps CD (Argo CD `selfHeal`)

- **Status:** Accepted
- **Tanggal:** 2026-11-26
- **Owner:** Tim Kelompok 2 (A02) — GearRent
- **Milestone:** M3 (Cloud-Native Platform), M4 (Delivery Automation), M7 (Reliability)

## Context
Sesuai batas tanggung jawab *Common Capstone Infrastructure*, deployment ke Kubernetes `k3d` harus bersifat deklaratif, memiliki jejak audit revisi, dan memisahkan tahap build/verifikasi dari tahap rekonsiliasi kluster. Selain itu, pengembangan aplikasi dan konfigurasi GitOps berada dalam satu monorepo tim di GitHub sebelum dikirim ke Gitea lokal.

## Options Considered
1. **CI langsung menjalankan `kubectl apply` dari runner:**
   - *Pro:* Sederhana pada awalnya.
   - *Kontra:* Melanggar prinsip GitOps, tidak ada rekonsiliasi otomatis terhadap drift konfigurasi di kluster, dan runner harus memegang akses admin kubeconfig.
2. **Pull-based GitOps dengan pemisahan repo `capstone-app` & `capstone-gitops` + Argo CD (Dipilih):**
   - *Pro:* CI hanya melakukan verify (`./mvnw -B verify`), build image non-root dengan tag 12-char Git SHA yang immutable, Trivy scan, push ke `localhost:5001`, dan update `newTag` pada `overlays/dev/kustomization.yaml` di `capstone-gitops`. Argo CD `3.5.6` melakukan sinkronisasi otomatis (`prune: true`, `selfHeal: true`).
   - *Kontra:* Membutuhkan skrip `git subtree split --prefix app` (`scripts/deploy-lab.sh`) untuk mendorong perubahan folder `app/` dari monorepo ke `capstone-app` di Gitea lokal, serta jeda polling sinkronisasi ~3 menit (kecuali di-refresh manual).

## Decision
Mempertahankan pemisahan tegas antara **Gitea CI** (`app/.gitea/workflows/ci.yml`) dan **Argo CD GitOps** (`templates/argocd/application.yaml`), menggunakan DNS internal jaringan Docker (`gitea:3000`, `postgres:5432`, `keycloak:8080`, `registry:5000`) agar tidak terblokir firewall host, serta menyimpan `capstone-app-secret` langsung di kluster di luar Git.

## Consequences & Limitations
- **Positif:** Setiap rollout di `k3d` dapat ditelusuri ke tag SHA spesifik (mis. `abfcb7462763` pada CI Run #8), pod yang terhapus langsung dibuat ulang (`~15,6 detik` terukur pada uji M7), dan drift manual kluster direkonsiliasi otomatis oleh Argo CD.
- **Limitasi:** Gitea Runner lokal (`5.1.1`) me-mount `/var/run/docker.sock` (`SECURITY_NOTES.md`), yang hanya aman untuk lab lokal tim tepercaya dan harus diganti *isolated/rootless DinD runner* pada lingkungan bersama.

## Traceability / Evidence
- `app/.gitea/workflows/ci.yml`
- `gitops/base/` & `gitops/overlays/dev/kustomization.yaml`
- `templates/argocd/application.yaml`
- `scripts/deploy-lab.sh`, `scripts/deploy-lab.ps1`, `SECURITY_NOTES.md`
