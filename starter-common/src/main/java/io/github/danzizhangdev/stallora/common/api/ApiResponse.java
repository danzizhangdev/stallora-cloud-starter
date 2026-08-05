package io.github.danzizhangdev.stallora.common.api;

import io.github.danzizhangdev.stallora.common.error.ErrorCode;
import org.slf4j.MDC;

import java.time.Instant;

/**
 * Uniform response envelope shared by every Stallora service.
 *
 * <p>The {@code traceId} is read from the logging MDC, so the same identifier that
 * appears in the logs and in the tracing backend is handed back to the caller.
 *
 * @param <T> payload type
 */
public record ApiResponse<T>(String code, String message, T data, String traceId, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "Success", data, currentTraceId(), Instant.now());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.code(),
                message != null ? message : errorCode.defaultMessage(),
                null, currentTraceId(), Instant.now());
    }

    private static String currentTraceId() {
        return MDC.get("traceId");
    }
}
