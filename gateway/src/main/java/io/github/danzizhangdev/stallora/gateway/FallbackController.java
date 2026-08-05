package io.github.danzizhangdev.stallora.gateway;

import io.github.danzizhangdev.stallora.common.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * The envelope is built by hand here: this is a WebFlux application, so the servlet-only
 * auto-configuration in starter-common deliberately does not apply.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/sample")
    public ResponseEntity<ApiResponse<Void>> sampleFallback() {
        ApiResponse<Void> body = new ApiResponse<>("GW-0503",
                "sample-service is temporarily unavailable, please retry shortly",
                null, MDC.get("traceId"), Instant.now());
        return ResponseEntity.status(503).body(body);
    }
}
