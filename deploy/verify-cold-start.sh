#!/usr/bin/env bash
# C-gate: cold start from nothing, exactly what a buyer runs.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "[0] docker VM memory must be capped at ~6GiB for the gate"
docker info --format '{{.MemTotal}}' | awk '{gb=$1/1024/1024/1024; printf "    docker VM memory: %.1f GiB\n", gb; if (gb > 6.6) { print "    ❌ cap the VM to 6GiB first (OrbStack Settings -> Resources)"; exit 1 } }'

echo "[1] build jars"
mvn -q -DskipTests package

echo "[2] cold start"
docker compose -f deploy/docker-compose.yml down -v --remove-orphans
docker compose -f deploy/docker-compose.yml up -d --build

echo "[3] wait for 6 healthchecked containers (jaeger is distroless: probed from host below)"
for i in $(seq 1 60); do
  HEALTHY=$(docker compose -f deploy/docker-compose.yml ps --format json | jq -rs '[.[] | select(.Health == "healthy")] | length')
  echo "    healthy: $HEALTHY/6"
  [ "$HEALTHY" = "6" ] && break
  sleep 5
done
[ "$HEALTHY" = "6" ] || { docker compose -f deploy/docker-compose.yml ps; echo "❌ not all healthy within 300s"; exit 1; }

echo "[4] jaeger reachable from host"
curl -fsS -o /dev/null http://localhost:16686/ && echo "    ✅ jaeger UI 200"

echo "[5] end-to-end probe"
./deploy/verify-e2e.sh http://localhost:8080

echo "[6] traces arrived for all three services"
sleep 10   # give the OTLP exporter a beat
SERVICES=$(curl -s http://localhost:16686/api/services | jq -r '.data[]')
for s in gateway auth sample-service; do
  echo "$SERVICES" | grep -qx "$s" && echo "    ✅ $s in Jaeger" || { echo "    ❌ $s missing in Jaeger (got: $SERVICES)"; exit 1; }
done

echo "[7] measured memory (paste this table into README.md)"
docker stats --no-stream --format 'table {{.Name}}\t{{.MemUsage}}'

echo "C-GATE PASS"
