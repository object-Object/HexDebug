package gay.object.hexdebug.core.api.debugging;

import org.jetbrains.annotations.ApiStatus;

public enum StopReason {
    STEP(false),
    PAUSE(true),
    BREAKPOINT(true),
    EXCEPTION(true),
    STARTED(true),
    TERMINATED(true);

    @ApiStatus.Internal
    public final boolean stopImmediately;

    StopReason(boolean stopImmediately) {
        this.stopImmediately = stopImmediately;
    }
}
