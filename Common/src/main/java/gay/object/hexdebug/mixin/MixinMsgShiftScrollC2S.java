package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.common.msgs.MsgShiftScrollC2S;
import gay.object.hexdebug.items.DebuggerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// server side
@Mixin(MsgShiftScrollC2S.class)
public class MixinMsgShiftScrollC2S {
    @Inject(method = "handleForHand", at = @At("HEAD"))
    private void hexdebug$handleForHand(ServerPlayer sender, InteractionHand hand, double delta, CallbackInfo ci) {
        if (delta != 0) {
            var stack = sender.getItemInHand(hand);
            var item = stack.getItem();
            if (item instanceof DebuggerItem debugger) {
                debugger.handleShiftScroll(sender, stack, delta);
            }
        }
    }
}
