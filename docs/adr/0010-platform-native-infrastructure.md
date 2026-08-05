# ADR-0010: Platform-native infrastructure over dedicated middleware

## Status

Accepted, 2026-08.

## Context

The classic Spring Cloud stack ships its own middleware for discovery (Eureka), config
(Config Server), tracing backends (Zipkin), HTTP clients (OpenFeign) and messaging
(often RabbitMQ). Each is another JVM or dependency inside a 6 GB budget, and several
are in maintenance mode while the industry standardises on Kubernetes primitives and
OpenTelemetry.

## Decision

| Concern | Choice | Instead of |
|---|---|---|
| Discovery | K8s Service DNS / compose service names | Eureka |
| Configuration | env vars + ConfigMap/Secret, Spring profiles | Config Server |
| Service calls | Spring HTTP Interface (@HttpExchange) + RestClient | OpenFeign |
| Tracing | Micrometer Tracing + OTLP -> Jaeger | Brave + Zipkin |
| Messaging | Kafka (KRaft, apache/kafka image) | RabbitMQ |
| Database | PostgreSQL (SKIP LOCKED, JSONB suit the outbox) | MySQL |
| Consistency | Saga + transactional outbox | Seata coordinator |

We do NOT add spring-cloud-kubernetes: with DNS discovery and env-based config the
application needs no Kubernetes API access at all, which also keeps Docker Compose as a
first-class deployment (same images, same code, two deploy paths).

## Consequences

- The application layer is deployment-agnostic; compose remains the buyer path, Helm
  the production-shaped path.
- We own more conventions in code (outbox, idempotency) — that is deliberate: those
  patterns transfer, running a coordinator does not.
- Memory budget after the swap: ~4.0 GB full stack, fits a 6 GB VPS.
