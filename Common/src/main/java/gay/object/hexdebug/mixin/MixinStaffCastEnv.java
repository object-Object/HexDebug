package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv;
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternC2S;
import gay.object.hexdebug.items.ItemEvaluator;
import gay.object.hexdebug.registry.HexDebugItems;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StaffCastEnv.class)
public abstract class MixinStaffCastEnv {
    @Inject(
        method = "handleNewPatternOnServer",
        at = @At(
            value = "INVOKE",
            target = "Lat/petrak/hexcasting/xplat/IXplatAbstractions;getStaffcastVM(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/InteractionHand;)Lat/petrak/hexcasting/api/casting/eval/vm/CastingVM;",
            ordinal = 0
        ),
        cancellable = true
    )
    private static void handleNewEvaluatorPatternOnServer$hexdebug(
        ServerPlayer sender,
        MsgNewSpellPatternC2S msg,
        CallbackInfo ci
    ) {
        var item = sender.getItemInHand(msg.handUsed()).getItem();
        if (item == HexDebugItems.EVALUATOR.getValue()) {
            var success = ItemEvaluator.handleNewEvaluatorPatternOnServer(sender, msg);
            if (success) {
                ci.cancel();
            }
        }
    }
}
