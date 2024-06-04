package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import at.petrak.hexcasting.common.msgs.IMessage;
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternC2S;
import at.petrak.hexcasting.xplat.IClientXplatAbstractions;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import gay.object.hexdebug.gui.IMixinGuiSpellcasting;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(GuiSpellcasting.class)
public class MixinGuiSpellcasting implements IMixinGuiSpellcasting {
    @Nullable
    @Unique
    private BiConsumer<HexPattern, Integer> onDrawSplicingTablePattern$hexdebug = null;

    @WrapWithCondition(
        method = "mouseReleased",
        at = @At(
            value = "INVOKE",
            target = "Lat/petrak/hexcasting/xplat/IClientXplatAbstractions;sendPacketToServer(Lat/petrak/hexcasting/common/msgs/IMessage;)V",
            remap = false
        )
    )
    private boolean redirectSplicingTableStaffPacket$hexdebug(IClientXplatAbstractions instance, IMessage message) {
        if (onDrawSplicingTablePattern$hexdebug != null && message instanceof MsgNewSpellPatternC2S newSpellPatternC2S) {
            onDrawSplicingTablePattern$hexdebug.accept(newSpellPatternC2S.pattern(), newSpellPatternC2S.resolvedPatterns().size() - 1);
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    public BiConsumer<HexPattern, Integer> getOnDrawSplicingTablePattern$hexdebug() {
        return onDrawSplicingTablePattern$hexdebug;
    }

    @Override
    public void setOnDrawSplicingTablePattern$hexdebug(@Nullable BiConsumer<HexPattern, Integer> unitFunction1) {
        onDrawSplicingTablePattern$hexdebug = unitFunction1;
    }
}
