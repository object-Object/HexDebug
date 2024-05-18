package gay.object.hexdebug.mixin;


import at.petrak.hexcasting.api.spell.Action;
import at.petrak.hexcasting.api.spell.OperationResult;
import at.petrak.hexcasting.api.spell.casting.CastingContext;
import at.petrak.hexcasting.api.spell.casting.eval.SpellContinuation;
import at.petrak.hexcasting.api.spell.iota.Iota;
import at.petrak.hexcasting.common.casting.operators.eval.OpEval;
import gay.object.hexdebug.casting.eval.IMixinCastingContext;
import gay.object.hexdebug.debugger.DebugStepType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;


@Mixin(OpEval.class)
public abstract class MixinOpEval implements Action {
    @SuppressWarnings("UnreachableCode")
    @Inject(method = "operate", at = @At("RETURN"), remap = false)
    private void setDebugStepType(
        SpellContinuation continuation,
        List<Iota> stack,
        Iota ravenmind,
        CastingContext ctx,
        CallbackInfoReturnable<OperationResult> cir
    ) {
        var debugCastEnv = (IMixinCastingContext) (Object) ctx;
        if (debugCastEnv != null) {
            debugCastEnv.setLastEvaluatedAction$hexdebug(this);
            debugCastEnv.setLastDebugStepType$hexdebug(DebugStepType.IN);
        }
    }
}
