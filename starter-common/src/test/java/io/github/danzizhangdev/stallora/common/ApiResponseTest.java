package io.github.danzizhangdev.stallora.common;

import io.github.danzizhangdev.stallora.common.api.ApiResponse;
import io.github.danzizhangdev.stallora.common.error.CommonErrorCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void ok_carriesDataCodeAndTraceId() {
        MDC.put("traceId", "abc123");
        ApiResponse<String> r = ApiResponse.ok("hello");
        assertEquals("OK", r.code());
        assertEquals("hello", r.data());
        assertEquals("abc123", r.traceId());
        assertNotNull(r.timestamp());
    }

    @Test
    void error_fallsBackToDefaultMessage() {
        ApiResponse<Void> r = ApiResponse.error(CommonErrorCodes.NOT_FOUND, null);
        assertEquals("CMN-0404", r.code());
        assertEquals("Resource not found", r.message());
        assertNull(r.data());
    }
}
