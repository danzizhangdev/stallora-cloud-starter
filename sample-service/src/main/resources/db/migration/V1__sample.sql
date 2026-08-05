CREATE TABLE sample_note (
    id         BIGSERIAL PRIMARY KEY,
    text       VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE sample_event_log (
    id          BIGSERIAL PRIMARY KEY,
    message_id  UUID   NOT NULL,
    note_id     BIGINT NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
