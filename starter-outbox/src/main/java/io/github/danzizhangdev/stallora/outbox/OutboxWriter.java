package io.github.danzizhangdev.stallora.outbox;

import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Writes an event into the same database transaction as the business change, which is the
 * whole point of the pattern: one commit, so a published fact can never disagree with the
 * row that caused it.
 */
public class OutboxWriter {

    private final JdbcTemplate jdbc;

    public OutboxWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Must be called inside the caller's transaction so the business write and the outbox row commit atomically. */
    public UUID write(String topic, String key, String jsonPayload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "OutboxWriter.write must be called inside an active transaction; " +
                    "otherwise the outbox row cannot commit atomically with your business write.");
        }
        UUID messageId = UUID.randomUUID();
        jdbc.update("INSERT INTO outbox_message (message_id, topic, message_key, payload, trace_id) VALUES (?,?,?,?,?)",
                messageId, topic, key, jsonPayload, MDC.get("traceId"));
        return messageId;
    }
}
