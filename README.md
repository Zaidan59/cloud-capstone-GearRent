# Cloud Computing Common Capstone Infrastructure Package

This package is the common local infrastructure baseline for the **Cloud Computing Final Capstone Project**.
It is deliberately independent of the business case selected by a student team.

## What this package standardizes

- Git hosting and code review: **Gitea**
- CI execution: **Gitea Runner**
- OCI image storage: local **Docker Registry**
- Identity: **Keycloak**
- Application database: **PostgreSQL**
- Kubernetes runtime: **k3d / k3s**
- GitOps CD: **Argo CD**
- Metrics/dashboard: **Prometheus + Grafana**
- Container vulnerability scanning: **Trivy** (run from CI or manually)

The package does **not** provide the capstone application itself. Each team creates its own information-system workload while using the same infrastructure responsibilities and evidence model.

## Start here

1. Read `guides/Cloud_Computing_Common_Capstone_Infrastructure_Guide.docx`.
2. Copy `infrastructure/.env.example` to `infrastructure/.env`.
3. Run the environment doctor.
4. Start core services.
5. Bootstrap Gitea.
6. Start the CI runner.
7. Create the k3d cluster.
8. Bootstrap the two project repositories.
9. Install Argo CD.
10. Enable observability when the project reaches the operations milestone.

## Profiles and laptop resource strategy

- **Core:** Gitea, PostgreSQL, Keycloak, local registry.
- **CI:** Gitea Runner (started only after registration token bootstrap).
- **Observability:** Prometheus + Grafana.

On an 8 GB laptop, run only what is needed for the current milestone. On 16 GB+ laptops, the full local stack plus a one-server/one-agent k3d cluster is normally much more comfortable.

## Security boundary

This package is for a **trusted student-owned local lab**. Demo credentials, localhost ports, and the host Docker socket are intentionally used to reduce setup complexity. They are **not** a production security design.
