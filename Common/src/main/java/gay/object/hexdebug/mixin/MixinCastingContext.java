package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.spell.Action;
import at.petrak.hexcasting.api.spell.casting.CastingContext;
import gay.object.hexdebug.casting.eval.IMixinCastingContext;
import gay.object.hexdebug.debugger.DebugStepType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/*
class DebugItemCastEnv(
    caster: ServerPlayer,
    castingHand: InteractionHand,
) : PackagedItemCastEnv(caster, castingHand), IDebugCastEnv {
    override var lastEvaluatedAction: Action? = null
    override var lastDebugStepType: DebugStepType? = null

    override fun printMessage(message: Component) {
        super.printMessage(message)
        printDebugMessage(caster, message)
    }

    override fun sendMishapMsgToPlayer(mishap: OperatorSideEffect.DoMishap) {
        super.sendMishapMsgToPlayer(mishap)
        mishap.mishap.errorMessageWithName(this, mishap.errorCtx)?.also {
            printDebugMessage(caster, it, OutputEventArgumentsCategory.STDERR)
        }
    }
}
 */

@Mixin(CastingContext.class)
public abstract class MixinCastingContext implements IMixinCastingContext {
    @Unique
    private boolean isDebugging$hexdebug = false;
    @Nullable
    @Unique
    private Action lastEvaluatedAction$hexdebug = null;
    @Nullable
    @Unique
    private DebugStepType lastDebugStepType$hexdebug = null;

    @Override
    public boolean isDebugging$hexdebug() {
        return isDebugging$hexdebug;
    }

    @Override
    public void setDebugging$hexdebug(boolean isDebugging) {
        isDebugging$hexdebug = isDebugging;
    }

    @Nullable
    @Override
    public Action getLastEvaluatedAction$hexdebug() {
        return lastEvaluatedAction$hexdebug;
    }

    @Override
    public void setLastEvaluatedAction$hexdebug(@Nullable Action lastEvaluatedAction) {
        lastEvaluatedAction$hexdebug = lastEvaluatedAction;
    }

    @Nullable
    @Override
    public DebugStepType getLastDebugStepType$hexdebug() {
        return lastDebugStepType$hexdebug;
    }

    @Override
    public void setLastDebugStepType$hexdebug(@Nullable DebugStepType lastDebugStepType) {
        lastDebugStepType$hexdebug = lastDebugStepType;
    }
}
