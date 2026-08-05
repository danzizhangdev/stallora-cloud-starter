package io.github.danzizhangdev.stallora.common.error;

/**
 * Thrown for expected, business-rule failures. Carries an {@link ErrorCode} so the
 * global handler can map it to an HTTP status and a stable code without guessing.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
