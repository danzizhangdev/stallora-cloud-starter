#!/usr/bin/env bash
# One command to a running stack on k3d. Companion to docker-compose.yml - same images, same env contract.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "[1] build jars and images"
mvn -q -DskipTests package
for m in gateway auth sample-service; do
  docker build -t "stallora/$m:dev" "$m"
done

echo "[2] cluster"
k3d cluster create stallora --agents 1 2>/dev/null || echo "    (cluster exists, reusing)"
k3d image import -c stallora stallora/gateway:dev stallora/auth:dev stallora/sample-service:dev

echo "[3] helm install"
helm upgrade --install stallora deploy/helm/stallora-starter -n stallora --create-namespace

echo "[4] wait for rollouts"
for d in postgres redis kafka jaeger auth sample-service gateway; do
  kubectl -n stallora rollout status "deploy/$d" --timeout=300s
done

echo
echo "READY. In another terminal:"
echo "  kubectl -n stallora port-forward svc/gateway 18080:8080"
echo "then run:"
echo "  ./deploy/verify-e2e.sh http://localhost:18080"
