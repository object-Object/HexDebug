package gay.`object`.hexdebug.gui

import gay.`object`.hexdebug.splicing.ISplicingTable.Action
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableClientView
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
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
            field = value
            updateIotaButtons()
        }

    var data = SplicingTableClientView.empty()
        set(value) {
            field = value
            updateIotaButtons()
            updateActiveButtons()
        }

    private val player = inventory.player

    private val iotaButtons = mutableListOf<Button>()
    private val edgeButtons = mutableListOf<Button>()

    private val iotasReadButtons = mutableListOf<Button>()
    private val iotasWriteButtons = mutableListOf<Button>()
    private val clipboardReadButtons = mutableListOf<Button>()
    private val clipboardWriteButtons = mutableListOf<Button>()

    private val allButtons = sequenceOf(
        iotasReadButtons,
        iotasWriteButtons,
        clipboardReadButtons,
        clipboardWriteButtons,
    ).flatten()

    private var viewStartIndex = 0
        set(value) {
            val clamped = value.coerceIn(0..data.lastIotaIndex - IOTA_BUTTONS + 1)
            if (field != clamped) {
                field = clamped
                updateIotaButtons()
            }
        }

    override fun init() {
        super.init()
        titleLabelX = (imageWidth - font.width(title)) / 2

        iotaButtons += (0 until IOTA_BUTTONS).map { offset ->
            Button.builder(Component.empty()) { onSelectIota(viewStartIndex + offset) }
                .pos(leftPos + 20 + offset * 26, topPos - 18)
                .size(22, 16)
                .build()
        }
        edgeButtons += (0..IOTA_BUTTONS).map { offset ->
            Button.builder(Component.empty()) { onSelectEdge(viewStartIndex + offset) }
                .pos(leftPos + 16 + offset * 26, topPos - 18)
                .size(4, 16)
                .build()
        }

        iotasReadButtons += listOf(
            Button.builder(Component.literal("<")) { viewStartIndex-- }
                .tooltip(Tooltip.create(Component.translatable(buttonKey("view_left"))))
                .pos(leftPos, topPos - 17)
                .size(14, 14)
                .build(),
            Button.builder(Component.literal(">")) { viewStartIndex++ }
                .tooltip(Tooltip.create(Component.translatable(buttonKey("view_right"))))
                .pos(leftPos + 25 + iotaButtons.size * 26, topPos - 17)
                .size(14, 14)
                .build(),
        ) + iotaButtons + edgeButtons

        iotasWriteButtons += listOf(
            actionButton(Action.NUDGE_LEFT)
                .bounds(leftPos, topPos, 16, 16)
                .build(),
        )

        allButtons.forEach(::addRenderableWidget)
        updateIotaButtons()
        updateActiveButtons()
    }

    // button helpers

    // FIXME: writing should not be dependent on reading
    private fun updateActiveButtons() {
        val data = data
        if (data.hasIotas) {
            setActive(iotasReadButtons, true)
            setActive(iotasWriteButtons, data.isWritable)
            setActive(clipboardReadButtons, data.hasClipboard)
            setActive(clipboardWriteButtons, data.hasClipboard && data.isClipboardWritable)
        } else {
            setActive(allButtons.asIterable(), false)
        }
    }

    private fun setActive(buttons: Iterable<Button>, active: Boolean) {
        buttons.forEach { it.active = active }
    }

    private fun updateIotaButtons() {
        iotaButtons.forEachIndexed { offset, button ->
            val index = viewStartIndex + offset
            val formats = if (isIotaSelected(index)) {
                arrayOf(ChatFormatting.BOLD, ChatFormatting.UNDERLINE)
            } else {
                arrayOf()
            }
            button.apply {
                if (data.isInRange(index)) {
                    message = Component.literal(index.toString()).withStyle(*formats)
                    tooltip = Tooltip.create(Component.translatable(buttonKey("iota"), index))
                } else {
                    message = Component.empty()
                    tooltip = null
                }
            }
        }
        edgeButtons.forEachIndexed { offset, button ->
            val index = viewStartIndex + offset
            button.setAlpha(if (isEdgeSelected(index)) 1f else 0.3f)
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

    private fun isIotaSelected(index: Int) = selection?.let { index in it.range } ?: false

    private fun isOnlyIotaSelected(index: Int) = selection?.let { it.size == 1 && it.from == index } ?: false

    private fun isEdgeSelected(index: Int) = selection?.let { it.start == index && it.end == null } ?: false

    private fun onSelectIota(index: Int) {
        if (!data.isInRange(index)) return

        val selection = selection
        this.selection = if (isOnlyIotaSelected(index)) {
            null
        } else if (Screen.hasShiftDown() && selection != null) {
            if (selection.isEdge && index < selection.from) {
                Selection.of(selection.from - 1, index)
            } else {
                Selection.of(selection.from, index)
            }
        } else {
            Selection.withSize(index, 1)
        }
    }

    private fun onSelectEdge(index: Int) {
        if (!data.isInRange(index)) return

        val selection = selection
        this.selection = if (isEdgeSelected(index)) {
            null
        } else if (Screen.hasShiftDown() && selection != null) {
            if (selection.isEdge && index < selection.from) {
                Selection.of(selection.from - 1, index)
            } else if (index > selection.from) {
                Selection.of(selection.from, index - 1)
            } else {
                Selection.of(selection.from, index)
            }
        } else {
            Selection.edge(index)
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

        const val IOTA_BUTTONS = 9
    }
}
