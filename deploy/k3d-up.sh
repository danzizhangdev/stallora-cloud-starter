#!/usr/bin/env bash
# One command to a running stack on k3d. Companion to docker-compose.yml - same images, same env contract.
#
# Runs fully offline once the images exist locally: every image the cluster needs is handed to
# k3s as a tarball in its airgap directory, so containerd never reaches out to a registry.
# That is not a nicety - on a network that hijacks or blocks Docker Hub, a fresh cluster
# otherwise fails to pull even the pause sandbox image and every pod stays ContainerCreating.
set -euo pipefail
cd "$(dirname "$0")/.."

CLUSTER=stallora
AIRGAP_DIR="${TMPDIR:-/tmp}/stallora-k3s-airgap"

# Everything the cluster itself needs, before any of our workloads exist. k3s pulls these
# from Docker Hub on a cold start, so on a restricted network the cluster never becomes
# usable: no pause image means every pod stays ContainerCreating, and no CoreDNS means
# service discovery - the whole point of ADR-0010 - silently does not work.
# Update these tags when you bump the k3s version (kubectl -n kube-system get pods -o
# jsonpath='{..image}' prints what your version wants).
K3S_IMAGES=(
  rancher/mirrored-pause:3.6
  rancher/mirrored-coredns-coredns:1.14.3
  rancher/local-path-provisioner:v0.0.36
  rancher/mirrored-metrics-server:v0.8.1
  rancher/klipper-helm:v0.10.0-build20260513
)
MIDDLEWARE_IMAGES=(
  postgres:16-alpine
  redis:7.2-alpine
  apache/kafka:3.9.1
  jaegertracing/all-in-one:1.71.0
)

echo "[1] build jars and images"
mvn -q -DskipTests package
for m in gateway auth sample-service; do
  docker build -t "stallora/$m:dev" "$m"
done

echo "[2] stage images for an offline cluster"
mkdir -p "$AIRGAP_DIR"
for img in "${K3S_IMAGES[@]}" "${MIDDLEWARE_IMAGES[@]}"; do
  docker image inspect "$img" >/dev/null 2>&1 || docker pull "$img"
done
tar_path="$AIRGAP_DIR/stallora-images.tar"
if [ ! -s "$tar_path" ] || [ -n "${REBUILD_AIRGAP:-}" ]; then
  docker save -o "$tar_path" "${K3S_IMAGES[@]}" "${MIDDLEWARE_IMAGES[@]}"
  echo "    wrote $tar_path ($(du -h "$tar_path" | cut -f1))"
else
  echo "    reusing $tar_path (set REBUILD_AIRGAP=1 to refresh)"
fi

echo "[3] cluster"
if ! k3d cluster list "$CLUSTER" >/dev/null 2>&1; then
  k3d cluster create "$CLUSTER" --agents 1 \
      --volume "$AIRGAP_DIR:/var/lib/rancher/k3s/agent/images@server:*;agent:*"
else
  echo "    (cluster exists, reusing)"
fi
# k3d writes https://0.0.0.0:<port> into the kubeconfig, which does not connect on every
# Docker backend (OrbStack returns EOF). Point it at loopback instead.
PORT=$(docker port "k3d-${CLUSTER}-serverlb" 6443/tcp | head -1 | sed -E 's/.*:([0-9]+)$/\1/')
kubectl config set-cluster "k3d-${CLUSTER}" --server="https://127.0.0.1:${PORT}" >/dev/null
kubectl config use-context "k3d-${CLUSTER}" >/dev/null

# The three application images are only ever built locally, so import them directly.
k3d image import -c "$CLUSTER" stallora/gateway:dev stallora/auth:dev stallora/sample-service:dev

echo "[4] helm install"
helm upgrade --install stallora deploy/helm/stallora-starter -n stallora --create-namespace

echo "[5] wait for rollouts"
for d in postgres redis kafka jaeger auth sample-service gateway; do
  kubectl -n stallora rollout status "deploy/$d" --timeout=300s
done

echo
echo "READY. In another terminal:"
echo "  kubectl -n stallora port-forward svc/gateway 18080:8080"
echo "then run:"
echo "  ./deploy/verify-e2e.sh http://localhost:18080"
