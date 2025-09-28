package gay.object.hexdebug.api.client.splicing;

import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import com.google.common.collect.Lists;
import gay.object.hexdebug.api.splicing.SplicingTableIotaClientView;
import gay.object.hexdebug.gui.splicing.SplicingTableScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface SplicingTableIotaRendererProvider {
    /**
     * Creates and returns a new renderer for the provided iota.
     * <br>
     * This is called every time the splicing table changes which iotas are currently visible, so
     * don't do anything too laggy in here.
     */
    @NotNull
    SplicingTableIotaRenderer createRenderer(
        @NotNull IotaType<?> type,
        @NotNull SplicingTableIotaClientView iota,
        int x,
        int y
    );

    /**
     * Creates and returns a new tooltip for the provided iota.
     * <br>
     * This is called every time the splicing table changes which iotas are currently visible, so
     * don't do anything too laggy in here.
     */
    @NotNull
    default SplicingTableIotaTooltip createTooltip(
        @NotNull IotaType<?> type,
        @NotNull SplicingTableIotaClientView iota
    ) {
        ArrayList<Component> advanced = new ArrayList<>();
        var typeKey = HexIotaTypes.REGISTRY.getKey(type);
        if (typeKey != null) {
            advanced.add(Component.literal(typeKey.toString()));
        }
        advanced.add(SplicingTableScreen.tooltipText("depth", iota.depth()));

        return new SplicingTableIotaTooltip(
            iota.name(),
            new ArrayList<>(),
            Lists.newArrayList(
                SplicingTableScreen.tooltipText("index", iota.index())
            ),
            advanced,
            null
        );
    }

    /**
     * Returns the background type for this renderer.
     * <br>
     * This is called every time the splicing table changes which iotas are currently visible, so
     * don't do anything too laggy in here.
     */
    @NotNull
    default SplicingTableIotaBackgroundType getBackgroundType(
        @NotNull IotaType<?> type,
        @NotNull SplicingTableIotaClientView iota
    ) {
        return SplicingTableIotaBackgroundType.GOLD;
    }
}
