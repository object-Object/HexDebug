package gay.object.hexdebug.core.api.debugging;

public enum StopReason {
    STEP(false),
    PAUSE(true),
    BREAKPOINT(true),
    EXCEPTION(true),
    STARTED(true),
    TERMINATED(true);

    public final boolean stopImmediately;

    StopReason(boolean stopImmediately) {
        this.stopImmediately = stopImmediately;
    }
}
