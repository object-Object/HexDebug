package gay.object.hexdebug.api.client.splicing;

import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import gay.object.hexdebug.api.splicing.SplicingTableIotaClientView;
import gay.object.hexdebug.gui.splicing.SplicingTableScreen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * A factory for {@link SplicingTableIotaRenderer} instances.
 * <br>
 * Every method in this interface is called every time the splicing table changes which iotas are
 * currently visible, so don't do anything too laggy in here.
 */
public interface SplicingTableIotaRendererProvider {
    /** Creates and returns a new {@link SplicingTableIotaRenderer} for the provided iota. */
    @NotNull
    SplicingTableIotaRenderer createRenderer(
        @NotNull IotaType<?> type,
        @NotNull SplicingTableIotaClientView iota,
        int x,
        int y
    );

    /**
     * Creates and returns a new {@link Tooltip} for the provided iota.
     * <br>
     * In most cases, you'll likely want to override
     * {@link SplicingTableIotaRendererProvider#getTooltipBuilder} instead.
     */
    @NotNull
    default Tooltip createTooltip(
        @NotNull IotaType<?> type,
        @NotNull SplicingTableIotaClientView iota
    ) {
        return getTooltipBuilder(type, iota).build();
    }

    /**
     * Creates and returns a new {@link SplicingTableIotaTooltipBuilder} for the provided iota.
     * <br>
     * If you want to provide a tooltip directly instead of using this builder, you can override
     * {@link SplicingTableIotaRendererProvider#createTooltip} instead.
     */
    @NotNull
    default SplicingTableIotaTooltipBuilder getTooltipBuilder(
        @NotNull IotaType<?> type,
        @NotNull SplicingTableIotaClientView iota
    ) {
        var builder = new SplicingTableIotaTooltipBuilder(iota.name())
            .addDetailsLine(SplicingTableScreen.tooltipText("index", iota.index()));

        var typeKey = HexIotaTypes.REGISTRY.getKey(type);
        if (typeKey != null) {
            builder.addAdvancedLine(Component.literal(typeKey.toString()));
        }

        return builder.addAdvancedLine(SplicingTableScreen.tooltipText("depth", iota.depth()));
    }

    /** Returns the background type for this renderer. */
    @NotNull
    default SplicingTableIotaBackgroundType getBackgroundType(
        @NotNull IotaType<?> type,
        @NotNull SplicingTableIotaClientView iota
    ) {
        return SplicingTableIotaBackgroundType.GOLD;
    }
}
