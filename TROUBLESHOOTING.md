# Troubleshooting Guide

Use a bottom-up method:

1. Host tools and Docker engine
2. Docker Compose services
3. Gitea user/runner registration
4. Registry push/pull
5. k3d cluster and Kubernetes objects
6. Argo CD repository access and sync
7. Application configuration and health
8. Prometheus scrape and Grafana dashboard

## Common checks

```bash
docker info
docker compose -f infrastructure/docker-compose.yml ps
curl http://127.0.0.1:3001/api/healthz
curl http://127.0.0.1:8081/realms/capstone/.well-known/openid-configuration
curl http://127.0.0.1:5001/v2/
k3d cluster list
kubectl get nodes
kubectl get pods -A
kubectl -n capstone get all,ingress
kubectl -n capstone describe pod <pod>
kubectl -n capstone logs <pod>
kubectl -n argocd get pods
```

## Port conflicts

Default host ports:

- 3001 Gitea
- 2222 Gitea SSH
- 5432 PostgreSQL
- 8081 Keycloak
- 5001 OCI Registry
- 8088 k3d HTTP ingress
- 8443 k3d HTTPS ingress
- 8082 Argo CD UI port-forward (when used)
- 9090 Prometheus
- 3000 Grafana

Change only one side of a mapping at a time, and record the new value in evidence.
