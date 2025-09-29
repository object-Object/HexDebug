package gay.object.hexdebug.api.client.splicing;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public final class SplicingTableIotaTooltipBuilder {
    private final Component name;
    private final ArrayList<Component> body;
    private final ArrayList<Component> details;
    private final ArrayList<Component> advanced;
    @Nullable
    private Component narration;

    public SplicingTableIotaTooltipBuilder(@NotNull Component name) {
        this.name = name;
        body = new ArrayList<>();
        details = new ArrayList<>();
        advanced = new ArrayList<>();
    }

    @NotNull
    public Component getName() {
        return name;
    }

    @NotNull
    public ArrayList<Component> getBodyLines() {
        return body;
    }

    @NotNull
    public ArrayList<Component> getDetailsLines() {
        return details;
    }

    @NotNull
    public ArrayList<Component> getAdvancedLines() {
        return advanced;
    }

    @Nullable
    public Component getNarration() {
        return narration;
    }

    /** Append a line to the tooltip's body. */
    @NotNull
    public SplicingTableIotaTooltipBuilder addBodyLine(@NotNull Component line) {
        body.add(line);
        return this;
    }

    /** Append a line to the tooltip's details. */
    @NotNull
    public SplicingTableIotaTooltipBuilder addDetailsLine(@NotNull Component line) {
        details.add(line);
        return this;
    }

    /** Append a line to the tooltip's "advanced tooltips" section. */
    @NotNull
    public SplicingTableIotaTooltipBuilder addAdvancedLine(@NotNull Component line) {
        advanced.add(line);
        return this;
    }

    /** Set the tooltip's narration. */
    @NotNull
    public SplicingTableIotaTooltipBuilder setNarration(@Nullable Component narration) {
        this.narration = narration;
        return this;
    }

    @NotNull
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
