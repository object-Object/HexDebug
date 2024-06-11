package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.client.ShiftScrollListener;
import gay.object.hexdebug.config.HexDebugConfig;
import gay.object.hexdebug.items.ItemDebugger;
import gay.object.hexdebug.registry.HexDebugItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Contract;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// client side
@Mixin(ShiftScrollListener.class)
public abstract class MixinShiftScrollListener {
    @Shadow(remap = false)
    private static double offHandDelta;

    @Contract
    @Invoker(value = "IsScrollableItem", remap = false)
    public static boolean hexdebug$invokeIsScrollableItem(Item item) {
        throw new AssertionError();
    }

    @Inject(method = "IsScrollableItem", at = @At("RETURN"), cancellable = true)
    private static void hexdebug$IsScrollableItem(Item item, CallbackInfoReturnable<Boolean> cir) {
        if (item == HexDebugItems.DEBUGGER.getValue()) {
            cir.setReturnValue(true);
        }
    }

    // TODO: the duplicated logic here feels nasty, but everything else I tried had issues with remapping
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hexdebug$preferOffhandIfNotDebugging(double delta, boolean needsSneaking, CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        if (
            // onScroll preconditions
            player != null
            && (player.isShiftKeyDown() || !needsSneaking)
            && !player.isSpectator()
            // additional logic
            && HexDebugConfig.INSTANCE.getClient().getSmartDebuggerSneakScroll()
            && !ItemDebugger.isDebugging()
            && player.getMainHandItem().getItem() == HexDebugItems.DEBUGGER.getValue()
            && hexdebug$invokeIsScrollableItem(player.getOffhandItem().getItem())
        ) {
            offHandDelta += delta;
            cir.setReturnValue(true);
        }
    }
}
