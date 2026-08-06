package io.github.danzizhangdev.stallora.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stallora.outbox")
public record OutboxProperties(Relay relay) {

    /**
     * @param maxAttempts after this many failed sends a row is parked as FAILED instead of
     *                    blocking every later row behind it. A poison message must not be
     *                    able to stop the whole relay.
     */
    public record Relay(Boolean enabled, Integer batchSize, Long pollIntervalMs, Integer maxAttempts) {
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
            if (maxAttempts == null) {
                maxAttempts = 10;
            }
        }
    }

    public OutboxProperties {
        if (relay == null) {
            relay = new Relay(null, null, null, null);
        }
    }
}
