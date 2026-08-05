package io.github.danzizhangdev.stallora.outbox;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = OutboxTestApp.class, properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:outbox-schema.sql",
        "stallora.outbox.relay.enabled=false"
})
@Testcontainers
class OutboxWriterTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        // the relay is off in this test, so no broker is needed; the Kafka auto-configuration is harmless
        r.add("spring.kafka.bootstrap-servers", () -> "localhost:1");
    }

    @Autowired
    OutboxWriter writer;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void write_outsideTransaction_throws() {
        assertThrows(IllegalStateException.class, () -> writer.write("t", "k", "{}"));
    }

    @Test
    void write_insideTransaction_persistsNewRowWithTraceId() {
        MDC.put("traceId", "trace-outbox");
        UUID id = tx.execute(s -> writer.write("orders", "42", "{\"orderId\":42}"));
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM outbox_message WHERE message_id = ?", id);
        assertEquals("NEW", row.get("status"));
        assertEquals("orders", row.get("topic"));
        assertEquals("trace-outbox", row.get("trace_id"));
        MDC.clear();
    }
}
