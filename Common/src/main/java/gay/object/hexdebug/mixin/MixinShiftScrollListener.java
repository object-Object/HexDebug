package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.client.ShiftScrollListener;
import gay.object.hexdebug.registry.HexDebugItems;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShiftScrollListener.class)
public class MixinShiftScrollListener {
    @Inject(method = "IsScrollableItem", at = @At("RETURN"), cancellable = true)
    private static void hexdebug$IsScrollableItem(Item item, CallbackInfoReturnable<Boolean> cir) {
        if (item == HexDebugItems.DEBUGGER.getValue()) {
            cir.setReturnValue(true);
        }
    }
}
