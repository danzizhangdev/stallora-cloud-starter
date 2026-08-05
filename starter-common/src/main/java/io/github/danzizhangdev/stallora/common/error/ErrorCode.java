package io.github.danzizhangdev.stallora.common.error;

/**
 * A stable, machine-readable error identifier.
 *
 * <p>Each service contributes its own enum of codes; the prefix keeps them apart
 * (for example {@code CMN-} for shared codes). Clients branch on {@link #code()},
 * never on the human-readable message.
 */
public interface ErrorCode {

    String code();

    String defaultMessage();

    default int httpStatus() {
        return 400;
    }
}
