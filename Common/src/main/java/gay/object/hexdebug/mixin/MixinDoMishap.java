package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.spell.casting.CastingHarness;
import at.petrak.hexcasting.api.spell.casting.sideeffects.OperatorSideEffect;
import at.petrak.hexcasting.api.spell.mishaps.Mishap;
import gay.object.hexdebug.casting.eval.IMixinCastingContext;
import gay.object.hexdebug.casting.eval.IMixinCastingContextKt;
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OperatorSideEffect.DoMishap.class)
public abstract class MixinDoMishap {
    @Final
    @Shadow(remap = false)
    private Mishap mishap;
    @Final
    @Shadow(remap = false)
    private Mishap.Context errorCtx;

    @SuppressWarnings("UnreachableCode")
    @Inject(method = "performEffect", at = @At("HEAD"), remap = false)
    private void printDebugMessage$hexdebug(CastingHarness harness, CallbackInfoReturnable<Boolean> cir) {
        var ctx = harness.getCtx();
        var msg = mishap.errorMessage(ctx, errorCtx);
        var debugCastEnv = (IMixinCastingContext) (Object) ctx;
        if (debugCastEnv != null && debugCastEnv.isDebugging$hexdebug()) {
            IMixinCastingContextKt.printDebugMessage(ctx.getCaster(), msg, OutputEventArgumentsCategory.STDERR, true);
        }
    }
}
