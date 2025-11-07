package gay.object.hexdebug.core.api.exceptions;

/**
 * Base class for errors thrown by debugger-related API methods.
 */
public class DebugException extends Exception {
    public DebugException() {}

    public DebugException(String message) {
        super(message);
    }

    public DebugException(String message, Throwable cause) {
        super(message, cause);
    }

    public DebugException(Throwable cause) {
        super(cause);
    }

    public DebugException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
