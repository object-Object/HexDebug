package gay.`object`.hexdebug.gui

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Action
import gay.`object`.hexdebug.blocks.splicing.Selection
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
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
    var selection: Selection? = null

    private val player = inventory.player

    private val iotaButtons = mutableListOf<Button>()
    private val listReadButtons = mutableListOf<Button>()
    private val listWriteButtons = mutableListOf<Button>()
    private val clipboardReadButtons = mutableListOf<Button>()
    private val clipboardWriteButtons = mutableListOf<Button>()

    private val buttons = sequenceOf(
        iotaButtons,
        listReadButtons,
        listWriteButtons,
        clipboardReadButtons,
        clipboardWriteButtons,
    ).flatten()

    private var listIndex = 0
        set(value) {
            field = value
            iotaButtons.forEachIndexed { index, button ->
                val title = Component.translatable(buttonKey("iota"), value + index)
                button.message = title
                button.tooltip = Tooltip.create(title)
            }
        }

    override fun init() {
        super.init()
        titleLabelX = (imageWidth - font.width(title)) / 2

        iotaButtons += (0 until 9).map(::iotaButton)

        listReadButtons += listOf(
            Button.builder(Component.literal("<")) { if (listIndex > 0) listIndex-- }
                .tooltip(Tooltip.create(Component.translatable(buttonKey("view_left"))))
                .pos(leftPos, topPos)
                .size(16, 16)
                .build(),
            Button.builder(Component.literal(">")) { if (listIndex < 10) listIndex++ } // FIXME: upper bound
                .tooltip(Tooltip.create(Component.translatable(buttonKey("view_right"))))
                .pos(leftPos + 18 + 9 * 26, topPos)
                .size(16, 16)
                .build(),
        )

        listWriteButtons += listOf(
            actionButton(Action.NUDGE_LEFT)
                .bounds(leftPos, topPos + 18, 16, 16)
                .build(),
        )

        buttons.forEach(::addRenderableWidget)
    }

    // button helpers

    private fun iotaButton(offset: Int) =
        button(Component.translatable(buttonKey("iota"), offset)) {
            HexDebug.LOGGER.info("Clicked iota $offset")
        }
            .pos(leftPos + 18 + offset * 26, topPos)
            .size(24, 16)
            .build()

    private fun actionButton(action: Action) =
        button(action.name.lowercase()) {
            menu.table.runAction(action, selection)
        }

    private fun button(name: String, onPress: Button.OnPress) = button(Component.translatable(buttonKey(name)), onPress)

    private fun button(message: Component, onPress: Button.OnPress) =
        Button.builder(message, onPress)
            .tooltip(Tooltip.create(message))

    private fun buttonKey(name: String) = "text.hexdebug.splicing_table.button.$name"

    // rendering

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

    companion object {
        // FIXME: placeholder
        val TEXTURE = ResourceLocation("textures/gui/container/dispenser.png")
    }
}
