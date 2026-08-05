# Stallora Cloud Starter

Production-shaped Spring Cloud starter: gateway, JWT auth, transactional outbox,
one-command Docker Compose and Helm deployments. Apache-2.0.

> Part of the Stallora project — a multi-vendor commerce platform built in public.

## Requirements

- JDK 21, Maven 3.9+, Docker
- The full stack fits in **under 2 GB of RAM**

### Measured memory

`docker stats` right after `./deploy/verify-cold-start.sh` passed, on an Apple-silicon Mac
with the Docker VM capped at 6 GiB (limit column reads 5.841 GiB — that cap is the point:
the gate is not allowed to pass on a machine bigger than a cheap VPS):

| Container | Memory |
|---|---|
| gateway | 494.0 MiB |
| sample-service | 512.2 MiB |
| auth | 343.4 MiB |
| kafka | 276.9 MiB |
| postgres | 37.3 MiB |
| jaeger | 10.6 MiB |
| redis | 4.9 MiB |
| **Total** | **≈ 1.64 GiB (1.76 GB)** |

Three JVMs at `-Xmx256m` account for ~1.3 GiB of that. Kafka is only this small because
its heap is pinned with `KAFKA_HEAP_OPTS=-Xmx512m -Xms512m`; the image defaults to a 1 GB
heap, which is the single easiest way to blow a small budget.

## Quick start

```bash
mvn -DskipTests package
docker compose -f deploy/docker-compose.yml up -d --build
```

## Modules

| Module | What it is |
|---|---|
| starter-common | Shared conventions: response envelope, error codes, exception handler, trace headers |
| starter-outbox | Transactional outbox: schema template, writer, polling relay, idempotent consumer |
| auth | RS256 JWT issuance (Nimbus) + JWKS endpoint, generic role claims |
| gateway | Routing, local JWT validation, Redis rate limiting, circuit breaking |
| sample-service | End-to-end demo: HTTP interface client, fallback, tracing, outbox round-trip |
