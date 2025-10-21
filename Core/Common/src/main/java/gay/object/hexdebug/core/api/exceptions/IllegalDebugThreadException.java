package gay.object.hexdebug.core.api.exceptions;

/**
 * The provided debug thread ID is invalid or already in use.
 */
public class IllegalDebugThreadException extends DebugException {
    public IllegalDebugThreadException() {}

    public IllegalDebugThreadException(String message) {
        super(message);
    }

    public IllegalDebugThreadException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalDebugThreadException(Throwable cause) {
        super(cause);
    }

    public IllegalDebugThreadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
