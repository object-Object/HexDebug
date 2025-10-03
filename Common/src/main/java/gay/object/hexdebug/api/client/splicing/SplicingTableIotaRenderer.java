package gay.object.hexdebug.api.client.splicing;

import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import gay.object.hexdebug.api.splicing.SplicingTableIotaClientView;
import gay.object.hexdebug.config.HexDebugClientConfig;
import gay.object.hexdebug.gui.splicing.SplicingTableScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public abstract class SplicingTableIotaRenderer {
    @NotNull
    private final IotaType<?> type;
    @NotNull
    private final SplicingTableIotaClientView iota;
    private int x;
    private int y;

    public SplicingTableIotaRenderer(
        @NotNull IotaType<?> type,
        @NotNull SplicingTableIotaClientView iota,
        int x,
        int y
    ) {
        this.type = type;
        this.iota = iota;
        this.x = x;
        this.y = y;
    }

    @NotNull
    public final IotaType<?> getType() {
        return type;
    }

    @NotNull
    public final SplicingTableIotaClientView getIota() {
        return iota;
    }

    public final int getX() {
        return x;
    }

    @ApiStatus.OverrideOnly
    public void setX(int x) {
        this.x = x;
    }

    public final int getY() {
        return y;
    }

    @ApiStatus.OverrideOnly
    public void setY(int y) {
        this.y = y;
    }

    /** Renders one frame of this iota. */
    public abstract void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    /** Returns the background type for this renderer. */
    @NotNull
    public SplicingTableIotaBackgroundType getBackgroundType() {
        return SplicingTableIotaBackgroundType.GOLD;
    }

    /**
     * Creates and returns a new {@link Tooltip} for the provided iota.
     * <br>
     * In most cases, you'll likely want to override
     * {@link SplicingTableIotaRenderer#buildTooltip} instead.
     */
    @NotNull
    public Tooltip createTooltip() {
        return buildTooltip().build();
    }

    /**
     * Creates and returns a new {@link SplicingTableIotaTooltipBuilder} for the provided iota.
     * <br>
     * If you want to provide a tooltip directly instead of using this builder, you can override
     * {@link SplicingTableIotaRenderer#createTooltip} instead.
     */
    @NotNull
    protected SplicingTableIotaTooltipBuilder buildTooltip() {
        Component name;
        if (
            type == HexIotaTypes.PATTERN
            || HexDebugClientConfig.getConfig().getSplicingTable().getShowNestedPatternNames()
        ) {
            name = iota.display();
        } else {
            name = type.display(iota.getData());
        }

        var builder = new SplicingTableIotaTooltipBuilder(name)
            .addDetailsLine(SplicingTableScreen.tooltipText("index", iota.index()));

        var typeKey = HexIotaTypes.REGISTRY.getKey(type);
        if (typeKey != null) {
            builder.addAdvancedLine(Component.literal(typeKey.toString()));
        }

        return builder.addAdvancedLine(SplicingTableScreen.tooltipText("depth", iota.depth()));
    }
}
