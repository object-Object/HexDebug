package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.casting.eval.ResolvedPattern;
import at.petrak.hexcasting.api.casting.math.HexCoord;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import at.petrak.hexcasting.common.msgs.IMessage;
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternC2S;
import at.petrak.hexcasting.xplat.IClientXplatAbstractions;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import gay.object.hexdebug.gui.splicing.IMixinGuiSpellcasting;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

@Mixin(GuiSpellcasting.class)
public abstract class MixinGuiSpellcasting implements IMixinGuiSpellcasting {
    @Shadow(remap = false)
    private List<ResolvedPattern> patterns;

    @Final
    @Shadow(remap = false)
    private Set<HexCoord> usedSpots;

    @Nullable
    @Unique
    private BiConsumer<HexPattern, Integer> onDrawSplicingTablePattern$hexdebug = null;

    @Accessor(remap = false)
    @NotNull
    @Override
    public abstract InteractionHand getHandOpenedWith();

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

    @Override
    public void clearPatterns$hexdebug() {
        patterns.clear();
        usedSpots.clear();
    }
}
