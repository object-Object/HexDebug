package gay.`object`.hexdebug.gui

import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Action
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Selection
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

@Environment(EnvType.CLIENT)
class SplicingTableScreen(
    menu: SplicingTableMenu,
    inventory: Inventory,
    title: Component,
) : AbstractContainerScreen<SplicingTableMenu>(menu, inventory, title) {
    private val player = inventory.player

    private var selection: Selection? = null

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

    // disable inventory label
    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 4210752, false)
    }

    private fun Action.run() {
        selection?.also { menu.table.runAction(this, it) }
    }

    companion object {
        // FIXME: placeholder
        val TEXTURE = ResourceLocation("textures/gui/container/dispenser.png")
    }
}
