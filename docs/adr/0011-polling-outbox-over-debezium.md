# ADR-0011: Polling outbox publisher over Debezium CDC

## Status

Accepted, 2026-08.

## Context

The transactional outbox needs a relay that moves committed rows to Kafka. The
"standard answer" is CDC: Debezium tails the WAL and never misses a row. It also adds
Kafka Connect — roughly another gigabyte and a second distributed system to operate,
inside a 6 GB budget.

## Decision

Poll the outbox table with FOR UPDATE SKIP LOCKED, send synchronously, mark rows SENT
after the broker acks. Delivery is at-least-once; consumers deduplicate through the
processed_message table (IdempotentProcessor).

## Consequences

- Identical semantics to CDC for this workload; one less system to run and explain.
- Polling adds up to one poll-interval of latency (default 1s) — irrelevant here.
- SKIP LOCKED makes multiple relay instances safe: rows are claimed, not double-sent.

## When to switch to Debezium

Sustained thousands of events/second, sub-100ms relay latency requirements, or an
operations team already running Kafka Connect. The schema stays the same either way —
that is the point of keeping the outbox contract in the database.
