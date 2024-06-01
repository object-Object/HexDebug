package gay.`object`.hexdebug.gui

import at.petrak.hexcasting.api.casting.iota.NullIota
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Action
import gay.`object`.hexdebug.blocks.splicing.Selection
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
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
        set(value) {
            if (field != value) {
                field = value
                updateIotaButtons()
            }
        }

    // FIXME: placeholder
    private val iotaList = List(18) { NullIota() }

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

    private var viewStartIndex = 0
        set(value) {
            val clamped = value.coerceIn(0..iotaList.lastIndex - iotaButtons.size + 1)
            if (field != clamped) {
                field = clamped
                updateIotaButtons()
            }
        }

    override fun init() {
        super.init()
        titleLabelX = (imageWidth - font.width(title)) / 2

        iotaButtons += (0 until 9).map { index ->
            Button.builder(Component.empty()) { onSelect(viewStartIndex + index) }
                .pos(leftPos + 18 + index * 26, topPos - 18)
                .size(24, 16)
                .build()
        }
        updateIotaButtons()

        listReadButtons += listOf(
            Button.builder(Component.literal("<")) { viewStartIndex-- }
                .tooltip(Tooltip.create(Component.translatable(buttonKey("view_left"))))
                .pos(leftPos, topPos - 18)
                .size(16, 16)
                .build(),
            Button.builder(Component.literal(">")) { viewStartIndex++ }
                .tooltip(Tooltip.create(Component.translatable(buttonKey("view_right"))))
                .pos(leftPos + 18 + iotaButtons.size * 26, topPos - 18)
                .size(16, 16)
                .build(),
        )

        listWriteButtons += listOf(
            actionButton(Action.NUDGE_LEFT)
                .bounds(leftPos, topPos, 16, 16)
                .build(),
        )

        buttons.forEach(::addRenderableWidget)
    }

    // button helpers

    private fun updateIotaButtons() {
        iotaButtons.forEachIndexed { index, button ->
            val listIndex = viewStartIndex + index
            val formats = if (isSelected(listIndex)) {
                arrayOf(ChatFormatting.BOLD, ChatFormatting.UNDERLINE)
            } else {
                arrayOf()
            }
            button.apply {
                if (listIndex in iotaList.indices) {
                    message = Component.literal(listIndex.toString()).withStyle(*formats)
                    tooltip = Tooltip.create(Component.translatable(buttonKey("iota"), listIndex))
                } else {
                    message = Component.empty()
                    tooltip = null
                }
            }
        }
    }

    private fun actionButton(action: Action) =
        button(action.name.lowercase()) {
            menu.table.runAction(action, selection)
        }

    private fun button(name: String, onPress: Button.OnPress) = button(Component.translatable(buttonKey(name)), onPress)

    private fun button(message: Component, onPress: Button.OnPress) =
        Button.builder(message, onPress)
            .tooltip(Tooltip.create(message))

    private fun buttonKey(name: String) = "text.hexdebug.splicing_table.button.$name"

    // GUI functionality

    private fun isSelected(listIndex: Int) = selection?.let { listIndex in it.range } ?: false

    private fun onSelect(listIndex: Int) {
        if (listIndex !in iotaList.indices) return

        val selection = selection
        this.selection = if (selection != null && listIndex in selection.range) {
            null
        } else if (selection == null || selection.size > 1) {
            Selection.withSize(listIndex, 1)
        } else if (listIndex < selection.start) {
            Selection.Range.of(listIndex, selection.start - 1 + selection.size)
        } else {
            selection.copy(end = listIndex)
        }
    }

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
