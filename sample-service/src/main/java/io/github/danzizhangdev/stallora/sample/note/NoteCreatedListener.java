package io.github.danzizhangdev.stallora.sample.note;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.danzizhangdev.stallora.outbox.IdempotentProcessor;
import io.github.danzizhangdev.stallora.outbox.OutboxHeaders;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class NoteCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(NoteCreatedListener.class);

    private final IdempotentProcessor processor;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public NoteCreatedListener(IdempotentProcessor processor,
                               org.springframework.jdbc.core.JdbcTemplate jdbc,
                               ObjectMapper mapper) {
        this.processor = processor;
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "sample-note-created", groupId = "sample-service")
    public void onMessage(ConsumerRecord<String, String> record) throws Exception {
        var header = record.headers().lastHeader(OutboxHeaders.MESSAGE_ID);
        if (header == null) {
            log.warn("Message without outbox id header, skipping");
            return;
        }
        UUID messageId = UUID.fromString(new String(header.value(), StandardCharsets.UTF_8));
        long noteId = mapper.readTree(record.value()).get("noteId").asLong();

        boolean processed = processor.processOnce(messageId, "sample-service",
                () -> jdbc.update("INSERT INTO sample_event_log (message_id, note_id) VALUES (?,?)", messageId, noteId));
        if (!processed) {
            log.info("Duplicate delivery of {} ignored (idempotent consumer)", messageId);
        }
    }
}
