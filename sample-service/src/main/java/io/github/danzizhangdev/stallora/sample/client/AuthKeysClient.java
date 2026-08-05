package io.github.danzizhangdev.stallora.sample.client;

import io.github.danzizhangdev.stallora.common.api.ApiResponse;
import org.springframework.web.service.annotation.GetExchange;

import java.util.Map;

/**
 * Spring's HTTP Interface — the declarative client that made OpenFeign unnecessary here
 * (ADR-0010). The interface is the contract; the proxy is built in {@link ClientConfig}.
 */
public interface AuthKeysClient {

    @GetExchange("/auth/keys/info")
    ApiResponse<Map<String, Object>> keysInfo();
}
