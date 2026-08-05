-- Canonical outbox schema. Copy this file into your service's Flyway migrations.
CREATE TABLE IF NOT EXISTS outbox_message (
    id          BIGSERIAL PRIMARY KEY,
    message_id  UUID        NOT NULL UNIQUE,
    topic       VARCHAR(255) NOT NULL,
    message_key VARCHAR(255),
    payload     TEXT        NOT NULL,
    trace_id    VARCHAR(64),
    status      VARCHAR(16) NOT NULL DEFAULT 'NEW',
    attempts    INT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at     TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_outbox_message_new ON outbox_message (id) WHERE status = 'NEW';

CREATE TABLE IF NOT EXISTS processed_message (
    message_id     UUID         NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    processed_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (message_id, consumer_group)
);
