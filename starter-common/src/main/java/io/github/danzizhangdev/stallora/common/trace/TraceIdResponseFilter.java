package io.github.danzizhangdev.stallora.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Copies the current trace id onto the response, so a caller can quote it in a bug
 * report and it can be looked up directly in the tracing backend.
 */
public class TraceIdResponseFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            response.setHeader(TraceHeaders.TRACE_ID, traceId);
        }
        chain.doFilter(request, response);
    }
}
