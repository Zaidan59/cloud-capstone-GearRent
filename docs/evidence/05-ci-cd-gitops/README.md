# 05 — Delivery Automation: CI/CD & GitOps (M4)

## Evidence Metadata

| Field | Detail |
| :--- | :--- |
| **Scope** | Pipeline otomatis Gitea Actions (`capstone-ci`), publikasi image bertag SHA ke OCI registry lokal (`localhost:5001` / `registry:5000`), pembaruan repo `capstone-gitops`, dan rekonsiliasi Argo CD (`prune` + `selfHeal`) |
| **Owner** | Kelompok 2 (Kevin, Zaidan, Raihan, Justin) |
| **Source / Version** | Ringkasan visual [`05_evidence.png`](05_evidence.png); definisi aktual di `app/.gitea/workflows/ci.yml`, `templates/argocd/application.yaml`, dan `scripts/deploy-lab.sh` |
| **Timestamp** | Gambar baseline: 12 November 2026; pipeline aktual diperbarui pada commit `bf69f81` (cache Trivy) |
| **Method** | Push `app/` via `git subtree split --prefix app` (`./scripts/deploy-lab.sh`) ke Gitea `student/capstone-app` -> runner `5.1.1` menjalankan `./mvnw -B verify`, `docker build` tag 12-char SHA, Trivy scan, `docker push`, dan commit update `newTag` pada `overlays/dev/kustomization.yaml` di `capstone-gitops` -> Argo CD `3.5.6` sinkronisasi otomatis |
| **Observation** | CI tidak pernah menjalankan `kubectl apply`. Setiap image memiliki tag SHA Git 12 karakter yang immutable sehingga perubahan dapat dilacak dari kode ke pod aktif. Argo CD dikonfigurasi dengan `automated: { prune: true, selfHeal: true }` |
| **Interpretation** | Pemisahan batas *CI (build/verify/publish)* dan *CD (pull-based GitOps)* memastikan kondisi kluster selalu mencerminkan *desired state* di Git dan drift manual otomatis dikembalikan |
| **Limitation** | 1) Kartu lama [`05_evidence.png`](05_evidence.png) mencantumkan Trivy `CRITICAL: 0, HIGH: 0` dari scan awal, sedangkan pada pipeline aktual (`ADR-006`, Run #8 `abfcb7462763`) Trivy mencatat **`HIGH: 2, CRITICAL: 0`** pada OS package `apt`/`libapt-pkg7.0`; 2) `deploy-lab.sh` hanya mendorong folder `app/`, sehingga perubahan manifest di `gitops/` harus didorong terpisah ke `capstone-gitops`; 3) Gitea Runner memakai Docker socket host (`SECURITY_NOTES.md`), hanya aman untuk lab lokal |
| **Decision Supported** | Mendukung `ADR-004` (pemisahan CI & GitOps) dan `ADR-006` (volume `trivy-cache` + `--timeout 15m` + `--exit-code 0` reporting gate) |

## Berkas & Tindak Lanjut
- Tersedia: [`05_evidence.png`](05_evidence.png)
- Disarankan tambah: screenshot asli Gitea Actions Run terbaru (`Synced` & `Healthy` di Argo CD UI + halaman commit `capstone-gitops`).
