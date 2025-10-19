package gay.object.hexdebug.core.api.debugging;

import at.petrak.hexcasting.api.casting.castables.Action;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType;
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import gay.object.hexdebug.core.api.HexDebugCoreAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Like {@link CastingEnvironment}, but for debugging.
 */
public abstract class DebugEnvironment {
    @NotNull
    private final ServerPlayer caster;
    @NotNull
    private final UUID sessionId = UUID.randomUUID();
    @Nullable
    private DebugStepType lastDebugStepType = null;
    @Nullable
    private Action lastEvaluatedAction = null;

    protected DebugEnvironment(@NotNull ServerPlayer caster) {
        this.caster = caster;
    }

    /**
     * Attempts to resume execution of the debuggee. This is called by the debugger after the
     * current continuation is successfully evaluated to completion.
     * @return true if the debug session can continue, or false if the debuggee should be terminated
     */
    public abstract boolean resume(
        @NotNull CastingEnvironment env,
        @NotNull CastingImage image,
        @NotNull ResolvedPatternType resolutionType
    );

    /**
     * Attempts to restart the debuggee on the given debug thread. This is called by the debugger
     * when requested by the user.
     * <br>
     * The previous debug thread is removed before this method is called, so the implementation may
     * use {@link HexDebugCoreAPI#createDebugThread} and {@link HexDebugCoreAPI#startDebuggingIotas}.
     * However, note that {@link DebugEnvironment} is <strong>not</strong> called before this
     * method, so it's up to the implementation whether they need to call that or not.
     */
    public abstract void restart(int threadId);

    /**
     * Terminates the debuggee. This is called by the debugger after {@link DebugEnvironment#resume}
     * returns {@code false}, during {@link HexDebugCoreAPI#terminateDebugThread}, or when requested
     * by the user.
     */
    public abstract void terminate();

    /**
     * For in-world debugees, returns whether the caster is close enough to the debuggee to allow
     * debug-related actions to be performed (eg. pause, step, restart).
     */
    @Contract(pure = true)
    public abstract boolean isCasterInRange();

    /**
     * Returns a display name for this debug session.
     * <br>
     * For example, debugger items return the name of the item, and spell circles return the name of
     * the impetus.
     */
    @Contract(pure = true)
    @NotNull
    public abstract Component getName();

    public void printDebugMessage(@NotNull Component message) {
        printDebugMessage(message, DebugOutputCategory.STDOUT, true);
    }

    public void printDebugMessage(
        @NotNull Component message,
        @NotNull DebugOutputCategory category
    ) {
        printDebugMessage(message, category, true);
    }

    public void printDebugMessage(
        @NotNull Component message,
        @NotNull DebugOutputCategory category,
        boolean withSource
    ) {
        HexDebugCoreAPI.INSTANCE.printDebugMessage(caster, sessionId, message, category, withSource);
    }

    public void printDebugMishap(
        @NotNull CastingEnvironment env,
        @NotNull OperatorSideEffect.DoMishap sideEffect
    ) {
        var message = sideEffect.getMishap().errorMessageWithName(env, sideEffect.getErrorCtx());
        if (message != null) {
            printDebugMessage(message, DebugOutputCategory.STDERR);
        }
    }

    @Contract(pure = true)
    public boolean isDebugging() {
        return HexDebugCoreAPI.INSTANCE.isSessionDebugging(this);
    }

    @NotNull
    public ServerPlayer getCaster() {
        return caster;
    }

    @NotNull
    public UUID getSessionId() {
        return sessionId;
    }

    @ApiStatus.Internal
    @Nullable
    public DebugStepType getLastDebugStepType() {
        return lastDebugStepType;
    }

    @ApiStatus.Internal
    public void setLastDebugStepType(@Nullable DebugStepType lastDebugStepType) {
        this.lastDebugStepType = lastDebugStepType;
    }

    @ApiStatus.Internal
    @Nullable
    public Action getLastEvaluatedAction() {
        return lastEvaluatedAction;
    }

    @ApiStatus.Internal
    public void setLastEvaluatedAction(@Nullable Action lastEvaluatedAction) {
        this.lastEvaluatedAction = lastEvaluatedAction;
    }
}
