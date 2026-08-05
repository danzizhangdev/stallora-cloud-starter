package io.github.danzizhangdev.stallora.sample.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KeyInfoService {

    private final AuthKeysClient client;

    public KeyInfoService(AuthKeysClient client) {
        this.client = client;
    }

    @CircuitBreaker(name = "auth", fallbackMethod = "fallback")
    public Map<String, Object> fetch() {
        return client.keysInfo().data();
    }

    /** A degraded answer beats a 500: the caller learns what is missing and can carry on. */
    Map<String, Object> fallback(Throwable t) {
        return Map.of("status", "auth-unavailable");
    }
}
