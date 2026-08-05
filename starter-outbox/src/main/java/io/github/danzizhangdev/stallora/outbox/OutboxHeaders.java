package io.github.danzizhangdev.stallora.outbox;

/** Kafka headers the relay attaches so consumers can deduplicate and keep the trace. */
public final class OutboxHeaders {

    public static final String MESSAGE_ID = "x-outbox-message-id";
    public static final String TRACE_ID = "x-outbox-trace-id";

    private OutboxHeaders() {
    }
}
