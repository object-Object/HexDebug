package gay.`object`.hexdebug.gui.splicing

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.utils.asTextComponent
import at.petrak.hexcasting.api.utils.asTranslatedComponent
import at.petrak.hexcasting.client.gui.GuiSpellcasting
import at.petrak.hexcasting.common.lib.HexSounds
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableAction
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Inventory
import java.util.*
import java.util.function.BiConsumer
import kotlin.math.pow

@Suppress("SameParameterValue")
class SplicingTableScreen(
    menu: SplicingTableMenu,
    inventory: Inventory,
    title: Component,
) : AbstractContainerScreen<SplicingTableMenu>(menu, inventory, title) {
    init {
        // main gui
        imageWidth = 192
        imageHeight = 193
    }

    var selection: Selection? = null
        set(value) {
            field = value
            reloadData()
        }

    val data get() = menu.clientView

    private val hasStaff get() = menu.staffSlot.hasItem()

    var guiSpellcasting = GuiSpellcasting(
        InteractionHand.MAIN_HAND, mutableListOf(), listOf(), null, 1
    ).apply {
        mixin.`onDrawSplicingTablePattern$hexdebug` = BiConsumer { pattern, index ->
            menu.table.drawPattern(null, pattern, index, selection)
        }
    }

    private val staffMinX get() = leftPos - 196
    private val staffMinY get() = topPos + 4
    private val staffMaxX get() = leftPos - 4
    private val staffMaxY get() = topPos + 196

    private val iotaButtons = mutableListOf<Button>()
    private val edgeButtons = mutableListOf<Button>()
    private val viewButtons = mutableListOf<Button>()
    private val staffButtons = mutableListOf<Button>()
    private val predicateButtons = mutableListOf<Pair<Button, () -> Boolean>>()

    private val listReadButtons = sequenceOf(
        iotaButtons,
        edgeButtons,
        viewButtons,
        staffButtons,
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
                reloadData()
            }
        }

    override fun init() {
        super.init()

        guiSpellcasting.init(minecraft!!, width, height)
        Minecraft.getInstance().soundManager.stop(HexSounds.CASTING_AMBIANCE.location, null)

        titleLabelX = (imageWidth - font.width(title)) / 2

//        allButtons.forEach(::removeWidget)
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

        viewButtons += listOf()

        staffButtons += listOf(
            button("clear_grid") { guiSpellcasting.mixin.`clearPatterns$hexdebug`() }
                .pos(leftPos - 200, topPos - 18)
                .size(64, 16)
                .build()
        )

        predicateButtons += SplicingTableAction.entries.mapIndexed { i, action ->
            actionButton(action) {
                it.pos(leftPos + imageWidth + 46, topPos + i * 18).size(96, 16)
            }
        }
        predicateButtons += listOf(
            Button.builder("<".asTranslatedComponent) { moveView(-1) }
                .tooltip(Tooltip.create(buttonKey("view_left").asTranslatedComponent))
                .pos(leftPos, topPos - 17)
                .size(14, 14)
                .build() to { viewStartIndex > 0 },

            Button.builder(">".asTranslatedComponent) { moveView(1) }
                .tooltip(Tooltip.create(buttonKey("view_right").asTranslatedComponent))
                .pos(leftPos + 25 + iotaButtons.size * 26, topPos - 17)
                .size(14, 14)
                .build() to { viewStartIndex < data.lastIndex - IOTA_BUTTONS + 1 },

            button("select_all") { selection = Selection.range(0, data.lastIndex) }
                .pos(leftPos + imageWidth - 64, topPos - 54)
                .size(64, 16)
                .build() to { selection?.start != 0 || selection?.end != data.lastIndex },

            button("select_none") { selection = null }
                .pos(leftPos + imageWidth - 64, topPos - 36)
                .size(64, 16)
                .build() to { selection != null },
        )

        allButtons.forEach(::addRenderableWidget)

        for (offset in 0 until IOTA_BUTTONS) {
            addRenderableOnly(IotaButton(offset))
        }
        for (offset in 0 until IOTA_BUTTONS) {
            addRenderableOnly(IotaSelection(offset))
        }

        reloadData()
    }

    private fun moveView(direction: Int) {
        viewStartIndex = if (hasControlDown()) {
            // start/end
            if (direction >= 0) {
                data.lastIndex
            } else {
                0
            }
        } else if (hasShiftDown()) {
            // full screen
            viewStartIndex + direction * IOTA_BUTTONS
        } else {
            // single iota
            viewStartIndex + direction
        }
    }

    // state sync

    fun reloadData() {
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

    // staff delegation stuff

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (hasStaff && isInStaffGrid(mouseX, mouseY)) {
            guiSpellcasting.mouseClicked(mouseX, mouseY, button)
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (hasStaff && isInStaffGrid(mouseX, mouseY)) {
            guiSpellcasting.mouseDragged(mouseX, mouseY, button, dragX, dragY)
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (hasStaff && isInStaffGrid(mouseX, mouseY)) {
            guiSpellcasting.mouseReleased(mouseX, mouseY, button)
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun onClose() {
        // support pressing esc to cancel drawing a pattern
        if (hasStaff) {
            guiSpellcasting.onClose()
            if (minecraft?.screen != null) return
        }
        super.onClose()
    }

    private fun isInStaffGrid(mouseX: Double, mouseY: Double) =
        staffMinX <= mouseX && mouseX <= staffMaxX && staffMinY <= mouseY && mouseY <= staffMaxY

    override fun hasClickedOutside(
        mouseX: Double,
        mouseY: Double,
        guiLeft: Int,
        guiTop: Int,
        mouseButton: Int
    ): Boolean {
        return (
            // main gui
            super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton)
            // storage
            && (mouseX < guiLeft + 192 || mouseY < guiTop + 103 || mouseX > guiLeft + 192 + 46 || mouseY > guiTop + 103 + 68)
            // media/staff (FIXME: placeholder)
            && (mouseX < guiLeft + 192 || mouseY < guiTop + 67 || mouseX > guiLeft + 192 + 18 || mouseY > guiTop + 67 + 36)
        )
    }

    // rendering

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val hasStaff = hasStaff
        staffButtons.forEach { it.visible = hasStaff }

        renderBackground(guiGraphics)
        if (hasStaff) renderStaffGrid(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // +46 for the side slots
        blitSprite(guiGraphics, leftPos, topPos, 256, 128, imageWidth + 46, imageHeight)
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val dust = menu.media.toDouble() / MediaConstants.DUST_UNIT
        val mediaLabel = "%.2f".format(Locale.ROOT, dust).asTextComponent
        guiGraphics.drawString(font, mediaLabel, 161, 8, 4210752, false)

        // index labels
        for ((x, y, offset) in listOf(
            Triple(18, 47, 0),
            Triple(86, 47, 4),
            Triple(155, 47, 8),
        )) {
            val index = viewStartIndex + offset
            if (data.isInRange(index)) {
                drawNumber(guiGraphics, x, y, index)
            }
        }
    }

    private fun renderStaffGrid(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.blit(BORDER, leftPos - 200, topPos, 0, 0, 200, 200)
        guiGraphics.enableScissor(staffMinX, staffMinY, staffMaxX, staffMaxY)
        guiSpellcasting.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.disableScissor()
    }

    private fun drawIota(guiGraphics: GuiGraphics, offset: Int, type: IotaRenderType) {
        blitSprite(
            guiGraphics,
            x = leftPos + 15 + 18 * offset,
            y = topPos + 20,
            uOffset = 352 + 20 * type.ordinal,
            vOffset = 0,
            width = 18,
            height = 21,
        )
    }

    private fun drawRangeSelection(guiGraphics: GuiGraphics, offset: Int, type: IotaRenderType, leftEdge: Boolean, rightEdge: Boolean) {
        blitSprite(
            guiGraphics,
            x = leftPos + 15 + 18 * offset,
            y = topPos + 18,
            uOffset = 352 + 20 * type.ordinal,
            vOffset = 24,
            width = 18,
            height = 25,
        )
        if (leftEdge) {
            drawSelectionEndCap(guiGraphics, offset, SelectionEndCap.LEFT)
        }
        if (rightEdge) {
            drawSelectionEndCap(guiGraphics, offset, SelectionEndCap.RIGHT)
        }
    }

    private fun drawEdgeSelection(guiGraphics: GuiGraphics, offset: Int) {
        drawSelectionEndCap(guiGraphics, offset - 1, SelectionEndCap.RIGHT)
        drawSelectionEndCap(guiGraphics, offset, SelectionEndCap.LEFT)
    }

    private fun drawSelectionEndCap(guiGraphics: GuiGraphics, offset: Int, endCap: SelectionEndCap) {
        blitSprite(
            guiGraphics,
            x = leftPos + 14 + 18 * offset + endCap.xOffset,
            y = topPos + 24,
            uOffset = 370 + endCap.uOffset,
            vOffset = 4,
            width = 1,
            height = 13,
        )
    }

    private fun drawNumber(guiGraphics: GuiGraphics, x: Int, y: Int, number: Int) {
        var i = 0
        var rest = number.coerceIn(0, MAX_DIGIT)
        do {
            drawDigit(guiGraphics, x + (MAX_DIGIT_LEN - i - 1) * 5, y, rest % 10)
            rest /= 10
            i++
        } while (rest > 0)
    }

    private fun drawDigit(guiGraphics: GuiGraphics, x: Int, y: Int, digit: Int) {
        blitSprite(guiGraphics, x, y, 288 + 5 * digit, 0, 4, 5)
    }

    private fun blitSprite(guiGraphics: GuiGraphics, x: Int, y: Int, uOffset: Int, vOffset: Int, width: Int, height: Int) {
        guiGraphics.blit(TEXTURE, x, y, uOffset.toFloat(), vOffset.toFloat(), width, height, 512, 512)
    }

    companion object {
        val TEXTURE = HexDebug.id("textures/gui/splicing_table.png")
        // FIXME: placeholder
        val BORDER = HexDebug.id("textures/gui/splicing_table/staff/border.png")

        const val IOTA_BUTTONS = 9

        const val MAX_DIGIT_LEN = 4
        val MAX_DIGIT = 10f.pow(MAX_DIGIT_LEN).toInt() - 1

        fun getInstance() = Minecraft.getInstance().screen as? SplicingTableScreen
    }

    enum class IotaRenderType {
        GENERIC,
        PATTERN,
    }

    enum class SelectionEndCap(val xOffset: Int, val uOffset: Int) {
        LEFT(xOffset = 0, uOffset = 1),
        RIGHT(xOffset = 19, uOffset = 0),
    }

    inner class IotaButton(private val offset: Int) : Renderable {
        override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val index = viewStartIndex + offset
            if (data.isInRange(index)) {
                // FIXME: get actual type
                val type = if (index % 2 == 0) IotaRenderType.GENERIC else IotaRenderType.PATTERN

                drawIota(guiGraphics, offset, type)
            }
        }
    }

    inner class IotaSelection(private val offset: Int) : Renderable {
        override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val index = viewStartIndex + offset
            if (data.isInRange(index)) {
                // FIXME: get actual type
                val type = if (index % 2 == 0) IotaRenderType.GENERIC else IotaRenderType.PATTERN

                when (val selection = selection) {
                    is Selection.Range -> if (index in selection) {
                        drawRangeSelection(
                            guiGraphics, offset, type,
                            leftEdge = index == selection.start,
                            rightEdge = index == selection.end,
                        )
                    }
                    is Selection.Edge -> if (index == selection.index) {
                        drawEdgeSelection(guiGraphics, offset)
                    }
                    null -> {}
                }
            }
        }
    }
}
