package gay.object.hexdebug.core.api.exceptions;

/**
 * The provided debug session ID or debug environment is invalid or already in use.
 */
public class IllegalDebugSessionException extends DebugException {
    public IllegalDebugSessionException() {}

    public IllegalDebugSessionException(String message) {
        super(message);
    }

    public IllegalDebugSessionException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalDebugSessionException(Throwable cause) {
        super(cause);
    }

    public IllegalDebugSessionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
