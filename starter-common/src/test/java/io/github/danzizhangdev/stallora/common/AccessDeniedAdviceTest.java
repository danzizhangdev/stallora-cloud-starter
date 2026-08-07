package io.github.danzizhangdev.stallora.common;

import io.github.danzizhangdev.stallora.common.error.AccessDeniedAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessDeniedAdviceTest {

    @Test
    void deniedAuthorizationAnswersWith403AndStableCode() {
        var response = new AccessDeniedAdvice().denied(new AccessDeniedException("Access Denied"));
        assertEquals(403, response.getStatusCode().value());
        assertEquals("CMN-0403", response.getBody().code());
    }
}
