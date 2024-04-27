package gay.object.hexdebug.forge.mixin;

import gay.object.hexdebug.HexDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// scuffed workaround for https://github.com/architectury/architectury-loom/issues/189
@Mixin(net.minecraft.data.Main.class)
public class MixinDatagenMain {
    @Inject(method = "main", at = @At("TAIL"), remap = false)
    private static void hexdebug$systemExitAfterDatagenFinishes(String[] strings, CallbackInfo ci) {
        HexDebug.LOGGER.info("Terminating datagen.");
        System.exit(0);
    }
}
