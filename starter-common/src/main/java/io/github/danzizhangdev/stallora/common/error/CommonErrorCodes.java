package io.github.danzizhangdev.stallora.common.error;

/** Error codes that are not specific to any single service. */
public enum CommonErrorCodes implements ErrorCode {

    INTERNAL("CMN-0500", "Unexpected server error", 500),
    VALIDATION("CMN-0400", "Validation failed", 400),
    NOT_FOUND("CMN-0404", "Resource not found", 404);

    private final String code;
    private final String defaultMessage;
    private final int httpStatus;

    CommonErrorCodes(String code, String defaultMessage, int httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
