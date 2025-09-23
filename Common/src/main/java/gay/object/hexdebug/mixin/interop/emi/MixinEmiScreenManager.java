package gay.object.hexdebug.mixin.interop.emi;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import gay.object.hexdebug.config.HexDebugClientConfig;
import gay.object.hexdebug.gui.splicing.SplicingTableScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "dev.emi.emi.screen.EmiScreenManager", remap = false)
public abstract class MixinEmiScreenManager {
    @Definition(id = "keyCode", local = @Local(name = "keyCode", type = int.class))
    @Expression("keyCode == 89")
    @ModifyExpressionValue(method = "keyPressed", at = @At(value = "MIXINEXTRAS:EXPRESSION"), require = 0)
    private static boolean hexdebug$cancelHardcodedKeybindInSplicingTable(boolean original) {
        if (
            HexDebugClientConfig.getConfig().getSplicingTableKeybinds().getEnabled()
            && Minecraft.getInstance().screen instanceof SplicingTableScreen
        ) {
            return false;
        }
        return original;
    }
}
