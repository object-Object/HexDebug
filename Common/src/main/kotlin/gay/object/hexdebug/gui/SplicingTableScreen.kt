package gay.`object`.hexdebug.gui

import at.petrak.hexcasting.api.casting.iota.IotaType
import gay.`object`.hexdebug.splicing.SplicingTableAction
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
            updateActiveButtons()
            updateIotaButtons()
        }

    private val player = inventory.player

    private val iotaButtons = mutableListOf<Button>()
    private val edgeButtons = mutableListOf<Button>()

    private val listReadButtons = mutableListOf<Button>()
    private val listWriteButtons = mutableListOf<Button>()
    private val clipboardReadButtons = mutableListOf<Button>()
    private val clipboardWriteButtons = mutableListOf<Button>()

    private val allButtons = sequenceOf(
        listReadButtons,
        listWriteButtons,
        clipboardReadButtons,
        clipboardWriteButtons,
    ).flatten()

    private var viewStartIndex = 0
        set(value) {
            val clamped = if (data.list?.let { it.size > IOTA_BUTTONS } == true) {
                value.coerceIn(0..data.lastIndex - IOTA_BUTTONS + 1)
            } else 0
            if (field != clamped) {
                field = clamped
                updateIotaButtons()
            }
        }

    override fun init() {
        super.init()
        titleLabelX = (imageWidth - font.width(title)) / 2

        allButtons.forEach(::removeWidget)
        iotaButtons.clear()
        edgeButtons.clear()
        listReadButtons.clear()
        listWriteButtons.clear()
        clipboardReadButtons.clear()
        clipboardWriteButtons.clear()

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

        listReadButtons += listOf(
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

        listWriteButtons += listOf(
            actionButton(SplicingTableAction.NUDGE_LEFT)
                .bounds(leftPos, topPos, 32, 16)
                .build(),
            actionButton(SplicingTableAction.NUDGE_RIGHT)
                .bounds(leftPos + 34, topPos, 32, 16)
                .build(),
        )

        allButtons.forEach(::addRenderableWidget)
        updateIotaButtons()
        updateActiveButtons()
    }

    // button helpers

    private fun updateActiveButtons() {
        val data = data
        if (data.isListReadable) {
            setActive(listReadButtons, true)
            setActive(listWriteButtons, data.isListWritable)
            setActive(clipboardReadButtons, data.isClipboardReadable)
            setActive(clipboardWriteButtons, data.isClipboardWritable)
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
                val iota = data.list?.getOrNull(index)
                if (null != iota) {
                    message = Component.literal(index.toString()).withStyle(*formats)
                    tooltip = Tooltip.create(IotaType.getDisplay(iota))
                } else {
                    message = Component.empty()
                    tooltip = null
                    active = false
                }
            }
        }
        edgeButtons.forEachIndexed { offset, button ->
            val index = viewStartIndex + offset
            button.apply {
                setAlpha(if (isEdgeSelected(index)) 1f else 0.3f)
                if (!(data.isInRange(index) || data.isInRange(index - 1))) {
                    active = false
                }
            }
        }
    }

    private fun actionButton(action: SplicingTableAction) =
        button(action.name.lowercase()) {
            menu.table.runAction(action, selection)
        }

    private fun button(name: String, onPress: Button.OnPress) = button(Component.translatable(buttonKey(name)), onPress)

    private fun button(message: Component, onPress: Button.OnPress) =
        Button.builder(message, onPress)
            .tooltip(Tooltip.create(message))

    private fun buttonKey(name: String) = "text.hexdebug.splicing_table.button.$name"

    // GUI functionality

    private fun isIotaSelected(index: Int) = selection?.let { index in it } ?: false

    private fun isOnlyIotaSelected(index: Int) = selection?.let { it.size == 1 && it.from == index } ?: false

    private fun isEdgeSelected(index: Int) = selection?.let { it.start == index && it.end == null } ?: false

    private fun onSelectIota(index: Int) {
        if (!data.isInRange(index)) return

        val selection = selection
        this.selection = if (isOnlyIotaSelected(index)) {
            null
        } else if (Screen.hasShiftDown() && selection != null) {
            if (selection is Selection.Edge && index < selection.from) {
                Selection.of(selection.from - 1, index)
            } else {
                Selection.of(selection.from, index)
            }
        } else {
            Selection.withSize(index, 1)
        }
    }

    private fun onSelectEdge(index: Int) {
        if (!(data.isInRange(index) || data.isInRange(index - 1))) return

        val selection = selection
        this.selection = if (isEdgeSelected(index)) {
            null
        } else if (Screen.hasShiftDown() && selection != null) {
            if (selection is Selection.Edge && index < selection.from) {
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
