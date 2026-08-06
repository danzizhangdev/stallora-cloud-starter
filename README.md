# Stallora Cloud Starter

Production-shaped Spring Cloud starter: gateway, JWT auth, transactional outbox,
one-command Docker Compose **and** Helm deployments of the same images. Apache-2.0.

> Part of the Stallora project — a multi-vendor commerce platform built in public.

The application layer has no deployment dependencies: discovery is DNS, configuration is
environment variables. That is why the identical images run under Compose and under
Kubernetes with nothing but different env values (see [ADR-0010](docs/adr/0010-platform-native-infrastructure.md)).

## Maven Central

Two libraries are published; the three services are reference implementations you copy from.

```xml
<dependency>
  <groupId>io.github.danzizhangdev</groupId>
  <artifactId>starter-common</artifactId>
  <version>0.1.0</version>
</dependency>
<dependency>
  <groupId>io.github.danzizhangdev</groupId>
  <artifactId>starter-outbox</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Requirements

- JDK 21, Maven 3.9+, Docker
- The full stack fits in **under 2 GB of RAM**

### Measured memory

`docker stats` right after `./deploy/verify-cold-start.sh` passed, on an Apple-silicon Mac
with the Docker VM capped at 6 GiB (limit column reads 5.841 GiB — that cap is the point:
the gate is not allowed to pass on a machine bigger than a cheap VPS):

| Container | Memory |
|---|---|
| sample-service | 488.2 MiB |
| gateway | 471.4 MiB |
| auth | 345.9 MiB |
| kafka | 289.3 MiB |
| postgres | 38.5 MiB |
| jaeger | 12.1 MiB |
| redis | 5.3 MiB |
| **Total** | **≈ 1.61 GiB (1.73 GB)** |

Three JVMs at `-Xmx256m` account for ~1.3 GiB of that. Kafka is only this small because
its heap is pinned with `KAFKA_HEAP_OPTS=-Xmx512m -Xms512m`; the image defaults to a 1 GB
heap, which is the single easiest way to blow a small budget.

## Quick start (Docker Compose)

```bash
mvn -DskipTests package
docker compose -f deploy/docker-compose.yml up -d --build
./deploy/verify-e2e.sh http://localhost:8080
```

The probe expects `E2E PASS`. Demo credentials are `alice` / `password-alice` (role
`admin`) and `bob` / `password-bob` (role `user`) — defined in `auth/src/main/resources/application.yml`.

Everything in one command, from nothing, the way a buyer would run it:

```bash
./deploy/verify-cold-start.sh      # down -v -> up --build -> healthchecks -> e2e -> traces -> docker stats
```

Developing a service in your IDE against middleware only:

```bash
docker compose -f deploy/docker-compose.dev.yml up -d
```

## Kubernetes path (k3d)

```bash
./deploy/k3d-up.sh
kubectl -n stallora port-forward svc/gateway 18080:8080   # in another terminal
./deploy/verify-e2e.sh http://localhost:18080             # same script, same assertions
k3d cluster delete stallora                               # when you are done
```

Same images, same environment contract, same end-to-end script — the Helm chart only
supplies different values.

### If your network cannot reach Docker Hub

`k3d-up.sh` already assumes it cannot. Every image the cluster needs — including k3s's own
pause, CoreDNS, local-path-provisioner, metrics-server and klipper-helm — is exported with
`docker save` into `$TMPDIR/stallora-k3s-airgap/stallora-images.tar` and handed to k3s as an
airgap volume, so containerd never contacts a registry. Only the initial `docker pull` on
your own machine needs network access.

The Compose path still needs Docker Hub **once**, to fetch `eclipse-temurin:21-jre` and the
four middleware images. After that both paths work offline. Note that BuildKit checks the
base image's registry metadata even when a copy is cached, so if `docker build` fails with
`failed to fetch anonymous token`, pull the base image first while you do have access:

```bash
docker pull eclipse-temurin:21-jre
```

Two things to know about the airgap tarball:

- Run `REBUILD_AIRGAP=1 ./deploy/k3d-up.sh` to refresh that tarball after you change an
  image tag.
- **When you bump the k3s version, update the `K3S_IMAGES` list in the script.** Its tags
  are tied to the k3s release. `kubectl -n kube-system get pods -o jsonpath='{..image}'`
  prints what your version actually asks for. Getting this wrong is quiet rather than loud:
  workload pods start fine and only CoreDNS fails, which surfaces much later as
  `UnknownHostException` on a service name.

## Modules

| Module | What it is |
|---|---|
| starter-common | Shared conventions: response envelope, error codes, exception handler, trace headers |
| starter-outbox | Transactional outbox: schema template, writer, polling relay, idempotent consumer |
| auth | RS256 JWT issuance (Nimbus) + JWKS endpoint, generic role claims |
| gateway | Routing, local JWT validation, Redis rate limiting, circuit breaking |
| sample-service | End-to-end demo: HTTP interface client, fallback, tracing, outbox round-trip |

## Using the outbox in your own service

**1. Copy the schema into your own migrations.** The canonical file ships inside the jar at
`outbox-schema.sql`; `sample-service` keeps its copy as `db/migration/V2__outbox.sql`. The
library never touches your migration history.

**2. Write the event inside the business transaction.** `write` throws if there is no active
transaction, because an outbox row that commits separately defeats the whole pattern:

```java
@Transactional
public Note create(String text) {
    Long id = jdbc.queryForObject("INSERT INTO sample_note (text) VALUES (?) RETURNING id", Long.class, text);
    outbox.write("sample-note-created", String.valueOf(id), "{\"noteId\":" + id + "}");
    return new Note(id, text);
}
```

**3. Make the consumer idempotent.** Delivery is at-least-once, so the second copy has to be
a no-op. The dedup row and your side effect share one transaction:

```java
@KafkaListener(topics = "sample-note-created", groupId = "sample-service")
public void onMessage(ConsumerRecord<String, String> record) {
    UUID messageId = UUID.fromString(new String(record.headers().lastHeader(OutboxHeaders.MESSAGE_ID).value()));
    processor.processOnce(messageId, "sample-service", () -> jdbc.update("INSERT INTO ...", messageId));
}
```

The relay runs itself: put `starter-outbox` on the classpath, give the service a
`JdbcTemplate` and a `KafkaTemplate`, and it polls every `stallora.outbox.relay.poll-interval-ms`
(default 1s, `stallora.outbox.relay.enabled=false` to switch it off).

## Architecture decisions

| ADR | In one sentence |
|---|---|
| [0001](docs/adr/0001-deliberately-over-decomposed.md) | The system is deliberately over-decomposed, because you cannot learn cross-service consistency in a monolith. |
| [0009](docs/adr/0009-mainstream-spring-cloud-over-alibaba.md) | Mainstream Spring Cloud only — the audience runs that, not Alibaba-specific middleware. |
| [0010](docs/adr/0010-platform-native-infrastructure.md) | Platform primitives instead of dedicated middleware: DNS discovery, env config, OTLP tracing, no service registry. |
| [0011](docs/adr/0011-polling-outbox-over-debezium.md) | A polling relay with SKIP LOCKED instead of Debezium CDC — same semantics, one less distributed system. |

## Third-party components and licences

| Component | Licence |
|---|---|
| Spring Boot, Spring Cloud, Spring Security, spring-kafka | Apache-2.0 |
| Resilience4j, Micrometer Tracing, OpenTelemetry OTLP exporter | Apache-2.0 |
| Nimbus JOSE + JWT | Apache-2.0 |
| springdoc-openapi | Apache-2.0 |
| Flyway Community Edition | Apache-2.0 |
| Testcontainers (test scope) | MIT |
| PostgreSQL JDBC driver | BSD-2-Clause |
| `postgres:16-alpine` image | PostgreSQL Licence |
| `redis:7.2-alpine` image | BSD-3-Clause |
| `apache/kafka:3.9.1` image | Apache-2.0 |
| `jaegertracing/all-in-one:1.71.0` image | Apache-2.0 |
| `eclipse-temurin:21-jre` image | GPLv2 with Classpath Exception |

Redis is pinned to **7.2** on purpose: it is the last BSD-licensed line, and 7.4+ ships
under RSALv2. If you want a newer, permissively licensed cache, use Valkey rather than
floating this tag.

## Known limitations

These are deliberate for a starter, and each one is a one-file change when you need more:

- **The auth signing key is generated at startup**, so a restart invalidates issued tokens.
  Resource servers recover on their own (they re-fetch the JWKS on an unknown `kid`).
  Load a persisted or rotating key pair for real use.
- **Following from that, auth cannot be scaled past one replica as it stands.** Each pod
  would mint its own key and publish only that key at `/.well-known/jwks.json`, so a token
  from pod A fails validation whenever the JWKS request lands on pod B. The chart pins
  `replicas: 1` for this reason; a shared or persisted key pair is the prerequisite for
  scaling it out.
- **Demo users live in `application.yml` in plaintext.** Replace `AuthProperties.users`
  with a real user store and a password encoder.
- **The Helm chart is a demo profile**: single replicas, `emptyDir` for PostgreSQL. Data
  disappears with the pod. Use a StatefulSet and a real volume for anything you keep.
- **Delivery is at-least-once, not exactly-once.** Every consumer must go through
  `IdempotentProcessor` (or its own dedup) — that is a design choice, not a gap.
- **A message the broker keeps rejecting is parked, not retried forever.** After
  `stallora.outbox.relay.max-attempts` (default 10) the row moves to `status='FAILED'` so it
  stops blocking every later row in the same topic. There is no dead-letter topic yet:
  `SELECT * FROM outbox_message WHERE status='FAILED'` is the queue you have to watch.

## Licence

Apache-2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
