package io.github.danzizhangdev.stallora.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * At-least-once delivery is the default over Kafka, so consumers have to make the second
 * copy of a message a no-op. The dedup row and the side effect share one transaction —
 * otherwise a crash between them would either lose the effect or block it forever.
 */
public class IdempotentProcessor {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public IdempotentProcessor(JdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    /** Runs action exactly once per (messageId, consumerGroup). Returns false when the message was already processed. */
    public boolean processOnce(UUID messageId, String consumerGroup, Runnable action) {
        return Boolean.TRUE.equals(tx.execute(status -> {
            int inserted = jdbc.update(
                    "INSERT INTO processed_message (message_id, consumer_group) VALUES (?,?) ON CONFLICT DO NOTHING",
                    messageId, consumerGroup);
            if (inserted == 0) {
                return false;
            }
            action.run();   // same transaction: the dedup row and the side effect commit together
            return true;
        }));
    }
}
