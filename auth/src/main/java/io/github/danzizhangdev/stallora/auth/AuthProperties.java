package io.github.danzizhangdev.stallora.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Demo user directory and token lifetime.
 *
 * <p>Passwords are plaintext in configuration on purpose: this service exists to
 * demonstrate token issuance, not to be a user store. Replace {@code users} with a real
 * repository and a password encoder before using it for anything.
 */
@ConfigurationProperties(prefix = "stallora.auth")
public record AuthProperties(List<DemoUser> users, Duration tokenTtl) {

    public record DemoUser(String username, String password, List<String> roles) {
    }

    public AuthProperties {
        if (users == null) {
            users = List.of();
        }
        if (tokenTtl == null) {
            tokenTtl = Duration.ofHours(1);
        }
    }
}
