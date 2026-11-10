# Course Snapshot - 27 October 2026

| Component | Pinned / expected version | Notes |
|---|---:|---|
| Gitea | 28.2.0 | Local Git + Actions server |
| Gitea Runner | 5.1.1 | Custom classroom image; trusted local repositories only |
| Argo CD | 3.5.6 | Non-HA install for local k3d |
| Keycloak | 26.8.1 | `start-dev` with imported classroom realm |
| PostgreSQL | 17-alpine | Application database major version pinned |
| Prometheus | 3.15.1 | Optional observability profile |
| Grafana | 13.2.4 | Optional observability profile |
| Trivy | 0.76.0 | Invoked as a container from CI/manual scan |
| Java runtime | 25 (LTS) | Application compilation, CI runner, and JVM container runtime |
| Docker Registry | 2 | Local insecure registry, localhost only |
| k3d | 5.9.0 recommended | Installed on student host |
| k3s | k3d default | Record actual server version in evidence |
| kubectl | current compatible client | Installed on student host |

The course package pins the infrastructure services that are most likely to affect reproducibility. Students must record the actual versions used in their project evidence.
