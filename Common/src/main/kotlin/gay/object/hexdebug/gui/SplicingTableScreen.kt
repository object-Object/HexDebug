package gay.`object`.hexdebug.gui

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.utils.asTranslatedComponent
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableAction
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
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
            updateButtons()
        }

    val data get() = menu.clientView

    private val iotaButtons = mutableListOf<Button>()
    private val edgeButtons = mutableListOf<Button>()
    private val viewButtons = mutableListOf<Button>()
    private val predicateButtons = mutableListOf<Pair<Button, () -> Boolean>>()

    private val listReadButtons = sequenceOf(
        iotaButtons,
        edgeButtons,
        viewButtons,
    ).flatten()

    private val allButtons = sequenceOf(
        listReadButtons,
        predicateButtons.asSequence().map { it.first },
    ).flatten()

    private var viewStartIndex = 0
        set(value) {
            val clamped = if (data.list?.let { it.size > IOTA_BUTTONS } == true) {
                value.coerceIn(0..data.lastIndex - IOTA_BUTTONS + 1)
            } else 0
            if (field != clamped) {
                field = clamped
                updateButtons()
            }
        }

    override fun init() {
        super.init()
        titleLabelX = (imageWidth - font.width(title)) / 2

        allButtons.forEach(::removeWidget)
        iotaButtons.clear()
        edgeButtons.clear()
        viewButtons.clear()
        predicateButtons.clear()

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

        viewButtons += iotaButtons + edgeButtons

        predicateButtons += SplicingTableAction.entries.mapIndexed { i, action ->
            actionButton(action) {
                it.pos(leftPos + imageWidth, topPos + i * 18).size(96, 16)
            }
        } + listOf(
            Button.builder("<".asTranslatedComponent) { viewStartIndex-- }
                .tooltip(Tooltip.create(buttonKey("view_left").asTranslatedComponent))
                .pos(leftPos, topPos - 17)
                .size(14, 14)
                .build() to { viewStartIndex > 0 },

            Button.builder(">".asTranslatedComponent) { viewStartIndex++ }
                .tooltip(Tooltip.create(buttonKey("view_right").asTranslatedComponent))
                .pos(leftPos + 25 + iotaButtons.size * 26, topPos - 17)
                .size(14, 14)
                .build() to { viewStartIndex < data.lastIndex - IOTA_BUTTONS + 1 },

            button("select_all") { selection = Selection.range(0, data.lastIndex) }
                .pos(leftPos + imageWidth - 64, topPos + 18)
                .size(64, 16)
                .build() to { selection?.start != 0 || selection?.end != data.lastIndex },

            button("select_none") { selection = null }
                .pos(leftPos + imageWidth - 64, topPos + 18 * 2)
                .size(64, 16)
                .build() to { selection != null },
        )

        allButtons.forEach(::addRenderableWidget)

        updateButtons()
    }

    // state sync

    fun updateButtons() {
        if (!data.isListReadable) {
            // these conditions are necessary to avoid an infinite loop
            if (selection != null) selection = null
            if (viewStartIndex != 0) viewStartIndex = 0
        }
        updateActiveButtons()
        updateIotaButtons()
    }

    private fun updateActiveButtons() {
        val data = data
        if (data.isListReadable) {
            setActive(listReadButtons, true)
            for ((button, predicate) in predicateButtons) {
                button.active = predicate()
            }
        } else {
            setActive(allButtons, false)
        }
    }

    private fun setActive(buttons: Sequence<Button>, active: Boolean) {
        for (button in buttons) {
            button.active = active
        }
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
                    message = index.toString().asTranslatedComponent.withStyle(*formats)
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

    // button factories

    private fun actionButton(action: SplicingTableAction, fn: (Button.Builder) -> Button.Builder) = Pair(
            fn(
                button(action.name.lowercase()) {
                    menu.table.runAction(action, null, selection)
                }
            ).build(),
        ) { action.value.test(data, selection) }

    private fun button(name: String, onPress: Button.OnPress) = button(buttonKey(name).asTranslatedComponent, onPress)

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

        fun getInstance() = Minecraft.getInstance().screen as? SplicingTableScreen
    }
}
