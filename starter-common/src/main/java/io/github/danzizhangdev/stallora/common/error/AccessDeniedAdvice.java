package io.github.danzizhangdev.stallora.common.error;

import io.github.danzizhangdev.stallora.common.api.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Keeps a denied @PreAuthorize check a 403.
 *
 * <p>Without this, the catch-all {@code Exception} handler in {@link GlobalExceptionHandler}
 * wins: the AuthorizationDeniedException raised inside a handler method is resolved by
 * {@code @ExceptionHandler} before it can reach Spring Security's ExceptionTranslationFilter,
 * which sits outside the DispatcherServlet — so an authorization failure would surface as
 * a 500. This advice runs first (HIGHEST_PRECEDENCE) and answers with the real status.
 *
 * <p>An unauthenticated request is unaffected: it never reaches a controller, so the
 * bearer-token entry point still returns 401.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessDeniedAdvice {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> denied(AccessDeniedException e) {
        return ResponseEntity.status(CommonErrorCodes.FORBIDDEN.httpStatus())
                .body(ApiResponse.error(CommonErrorCodes.FORBIDDEN, null));
    }
}
