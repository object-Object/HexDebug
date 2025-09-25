package gay.object.hexdebug.api.client.splicing;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public record SplicingTableIotaTooltip(
    @NotNull Component name,
    @NotNull ArrayList<Component> body,
    @NotNull ArrayList<Component> details,
    @NotNull ArrayList<Component> advanced,
    @Nullable Component narration
) {
    public Tooltip build() {
        var lines = new ArrayList<Component>();
        lines.add(name);
        lines.addAll(body);
        for (var line : details) {
            lines.add(line.copy().withStyle(ChatFormatting.GRAY));
        }
        if (Minecraft.getInstance().options.advancedItemTooltips) {
            for (var line : advanced) {
                lines.add(line.copy().withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        var tooltipText = ComponentUtils.formatList(lines, Component.literal("\n"));
        if (narration != null) {
            return Tooltip.create(tooltipText, narration);
        }
        return Tooltip.create(tooltipText);
    }
}
