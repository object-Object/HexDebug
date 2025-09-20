package gay.object.hexdebug.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gay.object.hexdebug.HexDebug;
import org.spongepowered.asm.mixin.Mixin;

// scuffed workaround for https://github.com/architectury/architectury-loom/issues/189
@Mixin({
    net.minecraft.data.Main.class,
    net.minecraft.server.Main.class,
})
public class MixinDatagenMain {
    @WrapMethod(method = "main", remap = false)
    private static void hexdebug$systemExitAfterDatagenFinishes(String[] strings, Operation<Void> original) {
        try {
            original.call((Object) strings);
        } catch (Throwable throwable) {
            HexDebug.LOGGER.error("Datagen failed!", throwable);
            System.exit(1);
        }
        HexDebug.LOGGER.info("Datagen succeeded, terminating.");
        System.exit(0);
    }
}
