package gay.object.hexdebug.api.client.splicing;

import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface SplicingTableIotaRenderer {
    void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);
}
