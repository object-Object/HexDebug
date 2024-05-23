package gay.object.hexdebug.mixin.hexal;

import at.petrak.hexcasting.api.casting.castables.Action;
import gay.object.hexdebug.debugger.DebugStepType;
import gay.object.hexdebug.debugger.hexal.IMixinWispCastEnv;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ram.talia.hexal.api.casting.eval.env.WispCastEnv;

@Mixin(WispCastEnv.class)
public class MixinWispCastEnv implements IMixinWispCastEnv {
    @Unique
    private boolean isDebugging$hexdebug = false;
    @Unique
    @Nullable
    private Action lastEvaluatedAction$hexdebug = null;
    @Unique
    @Nullable
    private DebugStepType lastDebugStepType$hexdebug = null;

    @Override
    public boolean isDebugging() {
        return isDebugging$hexdebug;
    }

    @Override
    public void setDebugging(boolean isDebugging) {
        isDebugging$hexdebug = isDebugging;
    }

    @Nullable
    @Override
    public Action getLastEvaluatedAction() {
        return lastEvaluatedAction$hexdebug;
    }

    @Override
    public void setLastEvaluatedAction(@Nullable Action action) {
        lastEvaluatedAction$hexdebug = action;
    }

    @Nullable
    @Override
    public DebugStepType getLastDebugStepType() {
        return lastDebugStepType$hexdebug;
    }

    @Override
    public void setLastDebugStepType(@Nullable DebugStepType debugStepType) {
        lastDebugStepType$hexdebug = debugStepType;
    }
}
