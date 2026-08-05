package io.github.danzizhangdev.stallora.auth;

import io.github.danzizhangdev.stallora.common.error.ErrorCode;

public enum AuthErrorCodes implements ErrorCode {

    INVALID_CREDENTIALS;

    public String code() {
        return "AUTH-0401";
    }

    public String defaultMessage() {
        return "Invalid username or password";
    }

    public int httpStatus() {
        return 401;
    }
}
