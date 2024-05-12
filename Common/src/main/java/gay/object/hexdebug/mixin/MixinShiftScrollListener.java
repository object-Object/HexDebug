package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.client.ShiftScrollListener;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gay.object.hexdebug.items.ItemDebugger;
import gay.object.hexdebug.registry.HexDebugItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// client side
@Mixin(ShiftScrollListener.class)
public abstract class MixinShiftScrollListener {
    @Inject(method = "IsScrollableItem", at = @At("RETURN"), cancellable = true)
    private static void hexdebug$IsScrollableItem(Item item, CallbackInfoReturnable<Boolean> cir) {
        if (item == HexDebugItems.DEBUGGER.getValue()) {
            cir.setReturnValue(true);
        }
    }

    @WrapOperation(
        method = "onScroll",
        at = @At(
            value = "INVOKE",
            target = "Lat/petrak/hexcasting/client/ShiftScrollListener;IsScrollableItem(Lnet/minecraft/world/item/Item;)Z",
            ordinal = 0
        )
    )
    private static boolean hexdebug$preferOffhandIfNotDebugging(Item item, Operation<Boolean> original) {
        var player = Minecraft.getInstance().player;
        if (
            player != null
            && !ItemDebugger.isDebugging()
            && item == HexDebugItems.DEBUGGER.getValue()
            && original.call(player.getOffhandItem().getItem())
        ) {
            return false;
        }
        return original.call(item);
    }
}
