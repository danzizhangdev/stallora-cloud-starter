package io.github.danzizhangdev.stallora.outbox.autoconfigure;

import io.github.danzizhangdev.stallora.outbox.OutboxProperties;
import io.github.danzizhangdev.stallora.outbox.OutboxRelay;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@AutoConfiguration(after = {KafkaAutoConfiguration.class, OutboxAutoConfiguration.class})
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "stallora.outbox.relay", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class OutboxRelayAutoConfiguration {

    @Bean
    @ConditionalOnBean({JdbcTemplate.class, KafkaTemplate.class, PlatformTransactionManager.class})
    @ConditionalOnMissingBean
    public OutboxRelay outboxRelay(JdbcTemplate jdbc, PlatformTransactionManager tm,
                                   KafkaTemplate<String, String> kafka, OutboxProperties props) {
        return new OutboxRelay(jdbc, new TransactionTemplate(tm), kafka, props.relay().batchSize());
    }
}
