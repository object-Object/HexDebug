package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.spell.casting.CastingContext;
import at.petrak.hexcasting.api.spell.iota.Iota;
import gay.object.hexdebug.casting.eval.IMixinCastingContext;
import gay.object.hexdebug.casting.eval.IMixinCastingContextKt;
import gay.object.hexdebug.casting.eval.UtilsKt;
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "at.petrak.hexcasting.common.casting.operators.spells.OpPrint$Spell")
public class MixinOpPrintSpell {
    @Final
    @Shadow(remap = false)
    private Iota datum;

    @SuppressWarnings("UnreachableCode")
    @Inject(method = "cast", at = @At("HEAD"), remap = false)
    private void printDebugMessage$hexdebug(CastingContext ctx, CallbackInfo ci) {
        var msg = datum.display();
        var debugCastEnv = (IMixinCastingContext) (Object) ctx;
        if (debugCastEnv != null && debugCastEnv.isDebugging$hexdebug()) {
            UtilsKt.printDebugMessage(ctx.getCaster(), msg, OutputEventArgumentsCategory.STDOUT, true);
        }
    }
}
