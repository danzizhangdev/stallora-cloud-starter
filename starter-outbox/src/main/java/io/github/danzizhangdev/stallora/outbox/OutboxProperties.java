package io.github.danzizhangdev.stallora.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stallora.outbox")
public record OutboxProperties(Relay relay) {

    public record Relay(Boolean enabled, Integer batchSize, Long pollIntervalMs) {
        public Relay {
            if (enabled == null) {
                enabled = true;
            }
            if (batchSize == null) {
                batchSize = 50;
            }
            if (pollIntervalMs == null) {
                pollIntervalMs = 1000L;
            }
        }
    }

    public OutboxProperties {
        if (relay == null) {
            relay = new Relay(null, null, null);
        }
    }
}
