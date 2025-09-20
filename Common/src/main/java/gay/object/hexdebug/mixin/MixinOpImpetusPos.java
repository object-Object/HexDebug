package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.common.casting.actions.circles.OpImpetusPos;
import gay.object.hexdebug.casting.eval.SplicingTableCastEnv;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(OpImpetusPos.class)
public abstract class MixinOpImpetusPos implements ConstMediaAction {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private void hexdebug$handleSplicingTableEnv(
        List<? extends Iota> args,
        CastingEnvironment ctx,
        CallbackInfoReturnable<List<? extends Iota>> cir
    ) {
        if (ctx instanceof SplicingTableCastEnv env) {
            cir.setReturnValue(OperatorUtils.getAsActionResult(env.getBlockPos()));
        }
    }
}
