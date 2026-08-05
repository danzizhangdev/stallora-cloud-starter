# ADR-0009: Mainstream Spring Cloud over Spring Cloud Alibaba

## Status

Accepted, 2026-08. Supersedes the original Nacos/Sentinel/Seata selection.

## Context

The original stack draft used Spring Cloud Alibaba (Nacos, Sentinel, Seata). It is a
capable stack and dominant in one market. But this project's audiences — overseas
readers, hiring managers, and CodeCanyon buyers — overwhelmingly run mainstream
Spring Cloud on Kubernetes. Skills and content should transfer to that world.

## Decision

Use the official Spring Cloud release train only: Spring Cloud Gateway, Resilience4j,
Spring Security resource servers. No Alibaba-specific middleware.

## Consequences

- Every component is documented in English and appears in overseas job descriptions.
- We give up Nacos's convenient console and Sentinel's dashboard; Kubernetes-native
  configuration and Resilience4j cover the same needs with fewer moving parts (ADR-0010).
- Seata's transaction coordinator is replaced by sagas over an outbox (ADR-0011 and
  the inventory reservation design in the main spec).
