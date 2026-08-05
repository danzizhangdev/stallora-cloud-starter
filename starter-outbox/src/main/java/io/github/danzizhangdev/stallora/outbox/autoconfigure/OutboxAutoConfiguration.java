package io.github.danzizhangdev.stallora.outbox.autoconfigure;

import io.github.danzizhangdev.stallora.outbox.IdempotentProcessor;
import io.github.danzizhangdev.stallora.outbox.OutboxProperties;
import io.github.danzizhangdev.stallora.outbox.OutboxWriter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The {@code after} list is load-bearing: {@code @ConditionalOnBean} is evaluated in
 * auto-configuration order, so without it this class runs before the JdbcTemplate exists
 * and silently contributes nothing.
 */
@AutoConfiguration(after = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class
})
@ConditionalOnClass(JdbcTemplate.class)
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean
    public OutboxWriter outboxWriter(JdbcTemplate jdbc) {
        return new OutboxWriter(jdbc);
    }

    @Bean
    @ConditionalOnBean({JdbcTemplate.class, PlatformTransactionManager.class})
    @ConditionalOnMissingBean
    public IdempotentProcessor idempotentProcessor(JdbcTemplate jdbc, PlatformTransactionManager tm) {
        return new IdempotentProcessor(jdbc, new TransactionTemplate(tm));
    }
}
