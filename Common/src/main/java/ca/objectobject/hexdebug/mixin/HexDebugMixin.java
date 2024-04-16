package ca.objectobject.hexdebug.mixin;

import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class HexDebugMixin {
    @Inject(at = @At("HEAD"), method = "init()V")
    private void init(CallbackInfo info) {
        System.out.println("Hello from hexdebug common mixin!");
    }
}