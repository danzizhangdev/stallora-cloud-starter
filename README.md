# Stallora Cloud Starter

Production-shaped Spring Cloud starter: gateway, JWT auth, transactional outbox,
one-command Docker Compose and Helm deployments. Apache-2.0.

> Part of the Stallora project — a multi-vendor commerce platform built in public.

## Requirements

- JDK 21, Maven 3.9+, Docker
- Full stack needs ~2.8 GB RAM (measured figure lands here in a later commit)

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
