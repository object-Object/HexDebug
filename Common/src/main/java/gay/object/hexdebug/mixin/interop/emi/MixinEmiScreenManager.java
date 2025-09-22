package gay.object.hexdebug.mixin.interop.emi;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.emi.screen.EmiScreenManager;
import gay.object.hexdebug.gui.splicing.SplicingTableScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = EmiScreenManager.class, remap = false)
public abstract class MixinEmiScreenManager {
    @Definition(id = "keyCode", local = @Local(name = "keyCode", type = int.class))
    @Expression("keyCode == 89")
    @ModifyExpressionValue(method = "keyPressed", at = @At(value = "MIXINEXTRAS:EXPRESSION"), require = 0)
    private static boolean hexdebug$cancelHardcodedKeybindInSplicingTable(boolean original) {
        if (Minecraft.getInstance().screen instanceof SplicingTableScreen) {
            return false;
        }
        return original;
    }
}
