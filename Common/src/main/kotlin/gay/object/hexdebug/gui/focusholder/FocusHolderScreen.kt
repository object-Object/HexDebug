package gay.`object`.hexdebug.gui.focusholder

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class FocusHolderScreen(
    menu: FocusHolderMenu,
    inventory: Inventory,
    title: Component,
) : AbstractContainerScreen<FocusHolderMenu>(menu, inventory, title) {
    override fun init() {
        super.init()
        titleLabelX = (imageWidth - font.width(title)) / 2
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(guiGraphics)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight)
    }

    companion object {
        // FIXME: placeholder
        val TEXTURE = ResourceLocation("textures/gui/container/dispenser.png")
    }
}
