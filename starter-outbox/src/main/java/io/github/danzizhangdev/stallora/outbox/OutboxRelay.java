package io.github.danzizhangdev.stallora.outbox;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Moves committed outbox rows to Kafka.
 *
 * <p>Delivery is at-least-once by design: the broker ack happens before the row is marked
 * SENT, so a crash in between re-sends the message. Consumers deduplicate — see
 * {@link IdempotentProcessor}.
 */
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final KafkaTemplate<String, String> kafka;
    private final int batchSize;

    public OutboxRelay(JdbcTemplate jdbc, TransactionTemplate tx, KafkaTemplate<String, String> kafka, int batchSize) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.kafka = kafka;
        this.batchSize = batchSize;
    }

    /** One polling pass. FOR UPDATE SKIP LOCKED lets multiple relay instances run without double-sending. */
    @Scheduled(fixedDelayString = "${stallora.outbox.relay.poll-interval-ms:1000}")
    public void relayOnce() {
        // A pass that cannot even reach the database (restart, failover, shutdown) must not
        // dump a stack trace on every tick; the next pass picks the rows up unchanged.
        try {
            relayBatch();
        } catch (RuntimeException e) {
            log.warn("Outbox relay pass could not run: {}", e.getMessage());
        }
    }

    private void relayBatch() {
        tx.executeWithoutResult(status -> {
            List<Map<String, Object>> batch = jdbc.queryForList(
                    "SELECT id, message_id, topic, message_key, payload, trace_id FROM outbox_message " +
                    "WHERE status = 'NEW' ORDER BY id LIMIT " + batchSize + " FOR UPDATE SKIP LOCKED");
            for (Map<String, Object> row : batch) {
                UUID messageId = (UUID) row.get("message_id");
                try {
                    ProducerRecord<String, String> rec = new ProducerRecord<>(
                            (String) row.get("topic"), (String) row.get("message_key"), (String) row.get("payload"));
                    rec.headers().add(OutboxHeaders.MESSAGE_ID, messageId.toString().getBytes(StandardCharsets.UTF_8));
                    String traceId = (String) row.get("trace_id");
                    if (traceId != null) {
                        rec.headers().add(OutboxHeaders.TRACE_ID, traceId.getBytes(StandardCharsets.UTF_8));
                    }
                    kafka.send(rec).get(10, TimeUnit.SECONDS);   // synchronous: only mark SENT after broker ack
                    jdbc.update("UPDATE outbox_message SET status='SENT', sent_at=now(), attempts=attempts+1 WHERE id=?",
                            row.get("id"));
                } catch (Exception e) {
                    log.warn("Outbox relay failed for message {}; row stays NEW and will be retried", messageId, e);
                    break;   // keep ordering; remaining rows retried next pass
                }
            }
        });
    }
}
