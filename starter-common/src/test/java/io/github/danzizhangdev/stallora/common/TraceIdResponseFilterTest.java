package io.github.danzizhangdev.stallora.common;

import io.github.danzizhangdev.stallora.common.trace.TraceHeaders;
import io.github.danzizhangdev.stallora.common.trace.TraceIdResponseFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraceIdResponseFilterTest {

    @Test
    void echoesTraceIdHeaderWhenMdcPresent() throws Exception {
        MDC.put("traceId", "trace-42");
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        new TraceIdResponseFilter().doFilter(req, res, new MockFilterChain());
        assertEquals("trace-42", res.getHeader(TraceHeaders.TRACE_ID));
        MDC.clear();
    }
}
