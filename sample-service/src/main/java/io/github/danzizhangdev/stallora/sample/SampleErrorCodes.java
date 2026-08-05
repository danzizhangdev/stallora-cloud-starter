package io.github.danzizhangdev.stallora.sample;

import io.github.danzizhangdev.stallora.common.error.ErrorCode;

public enum SampleErrorCodes implements ErrorCode {

    NOTE_NOT_FOUND;

    public String code() {
        return "SMP-0404";
    }

    public String defaultMessage() {
        return "Note not found";
    }

    public int httpStatus() {
        return 404;
    }
}
