package gay.object.hexdebug.mixin;

import gay.object.hexdebug.gui.IMixinScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen implements IMixinScreen {
    @Unique
    @Nullable
    private Screen hexdebug$previousScreen = null;

    @Override
    public void hexdebug$setPreviousScreen(@Nullable Screen screen) {
        hexdebug$previousScreen = screen;
    }

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void hexdebug$returnToPreviousScreen(CallbackInfo ci) {
        if (hexdebug$previousScreen != null) {
            Minecraft.getInstance().setScreen(hexdebug$previousScreen);
            hexdebug$previousScreen = null;
            ci.cancel();
        }
    }
}
