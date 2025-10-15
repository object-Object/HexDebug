package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.common.msgs.MsgShiftScrollC2S;
import gay.object.hexdebug.items.DebuggerItem;
import gay.object.hexdebug.items.EvaluatorItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// server side
@Mixin(MsgShiftScrollC2S.class)
public class MixinMsgShiftScrollC2S {
    @Final
    @Shadow(remap = false)
    private boolean isCtrl;

    @Inject(method = "handleForHand", at = @At("HEAD"))
    private void hexdebug$handleForHand(ServerPlayer sender, InteractionHand hand, double delta, CallbackInfo ci) {
        if (delta != 0) {
            var stack = sender.getItemInHand(hand);
            var item = stack.getItem();
            if (item instanceof DebuggerItem debugger) {
                debugger.handleShiftScroll(sender, stack, delta, isCtrl);
            } else if (item instanceof EvaluatorItem evaluator) {
                evaluator.handleShiftScroll(sender, stack, delta, isCtrl);
            }
        }
    }
}
