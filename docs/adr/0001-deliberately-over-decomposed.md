# ADR-0001: This system is deliberately over-decomposed

## Status

Accepted, 2026-08.

## Context

Stallora is a multi-vendor commerce platform built by one engineer at ~10 hours a week.
By any cost-benefit measure for the product alone, a modular monolith would win. But the
product is only one of this project's four goals, and it is ranked last. The first goal
is to learn — and be able to demonstrate — how distributed systems are actually run in
production: service-to-service auth, cross-service consistency without a transaction
coordinator, observability on a budget, deployment-agnostic configuration.

You cannot learn cross-service consistency in a monolith. There is no network between
your modules, no partial failure, no message that arrives twice.

## Decision

Split the backend into a gateway plus five business services (auth, catalog, inventory,
order, settlement), each owning its schema, communicating only via REST and Kafka events.
Run all of it in ~4 GB so a buyer's 6 GB VPS can hold it.

## Consequences

- Every hard problem this project exists to practise (idempotency, sagas, outbox,
  fail-fast inventory) now genuinely exists instead of being simulated.
- Operational cost is real: 6 JVMs, one broker, tracing. The memory budget is a
  first-class constraint and shows up in every infrastructure choice (see ADR-0010).
- Buyers get a system whose seams are visible and documented, which is the point.

## When I would collapse this back

A team building this as a product with these requirements should start with a modular
monolith and extract services only when scaling or ownership boundaries force it.
This repository optimises for learning surface, not for shipping speed.
