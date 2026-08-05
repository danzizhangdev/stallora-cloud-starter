package io.github.danzizhangdev.stallora.outbox;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = OutboxTestApp.class, properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:outbox-schema.sql",
        "stallora.outbox.relay.poll-interval-ms=200",
        // Testcontainers stops the broker before the Spring context closes, and the producer's
        // default 30s close timeout would then hold the JVM open long enough for Surefire to
        // kill the fork. Test-only setting; production keeps the default.
        "spring.kafka.producer.close-timeout=1s"
})
@Testcontainers
class OutboxRelayTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static org.testcontainers.kafka.KafkaContainer kafka =
            new org.testcontainers.kafka.KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    OutboxWriter writer;

    @Autowired
    IdempotentProcessor processor;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void writtenMessage_isRelayedToKafka_markedSent_andRedeliveryIsDeduped() {
        UUID id = tx.execute(s -> writer.write("relay-test", "k1", "{\"n\":1}"));

        try (KafkaConsumer<String, String> consumer = newConsumer("relay-test")) {
            // 1) the relay delivers the row and marks it SENT
            ConsumerRecord<String, String> rec = pollOne(consumer, Duration.ofSeconds(30));
            assertEquals("{\"n\":1}", rec.value());
            assertEquals(id.toString(), new String(rec.headers().lastHeader(OutboxHeaders.MESSAGE_ID).value()));
            Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertEquals("SENT", jdbc.queryForObject(
                    "SELECT status FROM outbox_message WHERE message_id = ?", String.class, id)));

            // 2) simulate "sent to the broker, crashed before marking": flipping the row back to NEW
            //    must produce a redelivery - that is what at-least-once means
            jdbc.update("UPDATE outbox_message SET status = 'NEW' WHERE message_id = ?", id);
            ConsumerRecord<String, String> redelivered = pollOne(consumer, Duration.ofSeconds(30));
            assertEquals(id.toString(), new String(redelivered.headers().lastHeader(OutboxHeaders.MESSAGE_ID).value()));

            // 3) the consumer side makes the duplicate harmless
            AtomicInteger runs = new AtomicInteger();
            assertTrue(processor.processOnce(id, "test-group", runs::incrementAndGet));
            assertFalse(processor.processOnce(id, "test-group", runs::incrementAndGet));
            assertEquals(1, runs.get());
        }
    }

    private KafkaConsumer<String, String> newConsumer(String topic) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "relay-test-consumer");
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        KafkaConsumer<String, String> c = new KafkaConsumer<>(p);
        c.subscribe(List.of(topic));
        return c;
    }

    private ConsumerRecord<String, String> pollOne(KafkaConsumer<String, String> c, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            var records = c.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                return records.iterator().next();
            }
        }
        throw new AssertionError("no record within " + timeout);
    }
}
