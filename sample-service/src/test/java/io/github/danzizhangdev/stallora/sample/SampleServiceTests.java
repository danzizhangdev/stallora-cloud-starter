package io.github.danzizhangdev.stallora.sample;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SampleServiceTests {

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
        r.add("stallora.outbox.relay.poll-interval-ms", () -> "200");
        // unreachable on purpose: this is what the circuit-breaker fallback test needs
        r.add("stallora.sample.auth-base-url", () -> "http://localhost:59999");
        r.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost:59999/never");
        r.add("spring.kafka.producer.close-timeout", () -> "1s");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @WithMockUser
    void postNote_persists_relays_andConsumerLogsExactlyOnce() throws Exception {
        String body = mvc.perform(post("/sample/notes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello outbox\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(header().exists("X-Trace-Id"))
                .andReturn().getResponse().getContentAsString();
        long noteId = com.jayway.jsonpath.JsonPath.parse(body).read("$.data.id", Long.class);

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Integer events = jdbc.queryForObject(
                "SELECT count(*) FROM sample_event_log WHERE note_id = ?", Integer.class, noteId);
            assertEquals(1, events);
        });
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM processed_message", Integer.class));
    }

    @Test
    @WithMockUser
    void getMissingNote_returns404WithSampleCode() throws Exception {
        mvc.perform(get("/sample/notes/999999"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.code").value("SMP-0404"));
    }

    @Test
    @WithMockUser
    void blankText_returns400ValidationCode() throws Exception {
        mvc.perform(post("/sample/notes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"\"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("CMN-0400"));
    }

    @Test
    @WithMockUser
    void keyInfo_fallsBackWhenAuthUnreachable() throws Exception {
        mvc.perform(get("/sample/key-info"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.status").value("auth-unavailable"));
    }
}
