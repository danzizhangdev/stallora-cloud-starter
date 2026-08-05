package io.github.danzizhangdev.stallora.common.trace;

/** Header names shared across services. */
public final class TraceHeaders {

    public static final String TRACE_ID = "X-Trace-Id";
    public static final String IDEMPOTENCY_KEY = "X-Idempotency-Key";

    private TraceHeaders() {
    }
}
