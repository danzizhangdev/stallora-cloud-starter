package io.github.danzizhangdev.stallora.common.error;

import io.github.danzizhangdev.stallora.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns exceptions into the shared {@link ApiResponse} envelope.
 *
 * <p>Unexpected exceptions are logged with their stack trace but answered with a
 * generic message, so internals never reach the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> business(BusinessException e) {
        return ResponseEntity.status(e.errorCode().httpStatus())
                .body(ApiResponse.error(e.errorCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream().findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage()).orElse(null);
        return ResponseEntity.status(CommonErrorCodes.VALIDATION.httpStatus())
                .body(ApiResponse.error(CommonErrorCodes.VALIDATION, detail));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(CommonErrorCodes.INTERNAL.httpStatus())
                .body(ApiResponse.error(CommonErrorCodes.INTERNAL, null));
    }
}
