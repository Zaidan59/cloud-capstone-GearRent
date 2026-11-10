# Security Notes and Classroom Boundaries

## Local-only design

The default services are published only on localhost. Do not rebind them to `0.0.0.0` on a shared/campus network unless the instructor explicitly approves the design.

## Demo credentials

The `.env.example` file contains classroom credentials. Change them when convenient, and never reuse them for real systems.

## Runner security

The reference Gitea Runner uses a **host execution label** and mounts `/var/run/docker.sock`. Docker-socket access is effectively privileged access to the host Docker daemon. This is acceptable only because the expected environment is:

- student-owned laptop;
- trusted coursework repositories;
- one team / one local Gitea instance.

A future shared LABFIK deployment should use isolated/dedicated runners, preferably DinD/rootless DinD or disposable runners.

## Kubernetes secrets

The GitOps template contains `secret.example.yaml` only. Do not commit the real database password or application secret. Create the runtime Secret separately, or later integrate a proper secret-management mechanism.

## Local registry

The local OCI registry is intentionally HTTP/insecure and must remain local. k3d is configured to trust this classroom registry only.

## Evidence redaction

Do not submit access tokens, passwords, session cookies, Keycloak tokens, runner registration tokens, or the Gitea runner `.runner` file.
