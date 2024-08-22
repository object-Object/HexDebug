package gay.`object`.hexdebug.gui.splicing

import at.petrak.hexcasting.api.utils.asTranslatedComponent
import at.petrak.hexcasting.client.gui.GuiSpellcasting
import at.petrak.hexcasting.common.lib.HexSounds
import com.mojang.blaze3d.systems.RenderSystem
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.gui.splicing.widgets.*
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableAction
import gay.`object`.hexdebug.splicing.toHexpatternSource
import gay.`object`.hexdebug.utils.falpha
import gay.`object`.hexdebug.utils.fblue
import gay.`object`.hexdebug.utils.fgreen
import gay.`object`.hexdebug.utils.fred
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Inventory
import java.awt.Color
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

    private val hasMediaItem get() = menu.mediaSlot.hasItem()
    private val hasStaffItem get() = menu.staffSlot.hasItem()

    var guiSpellcasting = GuiSpellcasting(
        InteractionHand.MAIN_HAND, mutableListOf(), listOf(), null, 1
    ).apply {
        mixin.`onDrawSplicingTablePattern$hexdebug` = BiConsumer { pattern, index ->
            menu.table.drawPattern(null, pattern, index, selection)
        }
    }

    // should be multiples of 32, since that's how big the edge parts are
    private val staffWidth = 32 * 6
    private val staffHeight = 32 * 6

    private val staffMinX get() = leftPos - 14 - staffWidth
    private val staffMaxX get() = leftPos - 14
    private val staffMinY get() = topPos
    private val staffMaxY get() = topPos + staffHeight

    private val iotaButtons = mutableListOf<AbstractButton>()
    private val edgeButtons = mutableListOf<AbstractButton>()
    private val viewButtons = mutableListOf<AbstractButton>()
    private val staffButtons = mutableListOf<AbstractButton>()
    private val predicateButtons = mutableListOf<Pair<AbstractButton, () -> Boolean>>()

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

        staffButtons += listOf(
            button("clear_grid") { guiSpellcasting.mixin.`clearPatterns$hexdebug`() }
                .pos(leftPos - 200, topPos - 24)
                .size(64, 16)
                .build()
        )

        viewButtons += listOf(
            button("export") { exportToSystemClipboard() }
                .pos(leftPos + imageWidth + 2, topPos)
                .size(128, 16)
                .build()
        )

        predicateButtons += listOf(
            // move view

            SpriteButton(
                x = leftPos + 4,
                y = topPos + 25,
                uOffset = 256,
                vOffset = 0,
                width = 10,
                height = 10,
                message = buttonText("view_left"),
            ) { // onPress
                moveView(-1)
            } to { // test
                viewStartIndex > 0
            },

            SpriteButton(
                x = leftPos + 178,
                y = topPos + 25,
                uOffset = 266,
                vOffset = 0,
                width = 10,
                height = 10,
                message = buttonText("view_right"),
            ) { // onPress
                moveView(1)
            } to { // test
                viewStartIndex < data.lastIndex - IOTA_BUTTONS + 1
            },

            // around main item slot

            actionHexagonSpriteButton(
                x = leftPos + 67,
                y = topPos + 57,
                uOffset = 256,
                vOffset = 48,
                width = 18,
                height = 16,
                triangleHeight = 4,
                isHorizontal = true,
                action = SplicingTableAction.NUDGE_LEFT,
            ),

            actionHexagonSpriteButton(
                x = leftPos + 107,
                y = topPos + 57,
                uOffset = 276,
                vOffset = 48,
                width = 18,
                height = 16,
                triangleHeight = 4,
                isHorizontal = true,
                action = SplicingTableAction.NUDGE_RIGHT,
            ),

            actionHexagonSpriteButton(
                x = leftPos + 67,
                y = topPos + 79,
                uOffset = 256,
                vOffset = 66,
                width = 18,
                height = 16,
                triangleHeight = 4,
                isHorizontal = true,
                action = SplicingTableAction.DELETE,
            ),

            actionHexagonSpriteButton(
                x = leftPos + 107,
                y = topPos + 79,
                uOffset = 276,
                vOffset = 66,
                width = 18,
                height = 16,
                triangleHeight = 4,
                isHorizontal = true,
                action = SplicingTableAction.DUPLICATE,
            ),

            // right side

            SpriteButton(
                x = leftPos + 144,
                y = topPos + 64,
                uOffset = 284,
                vOffset = 16,
                width = 18,
                height = 11,
                message = buttonText("select_none"),
            ) { // onPress
                selection = null
            } to { // test
                selection != null
            },

            SpriteButton(
                x = leftPos + 165,
                y = topPos + 64,
                uOffset = 305,
                vOffset = 16,
                width = 18,
                height = 11,
                message = buttonText("select_all"),
            ) { // onPress
                selection = Selection.range(0, data.lastIndex)
            } to { // test
                selection?.start != 0 || selection?.end != data.lastIndex
            },

            actionSpriteButton(
                x = leftPos + 144,
                y = topPos + 77,
                uOffset = 284,
                vOffset = 29,
                width = 18,
                height = 11,
                action = SplicingTableAction.UNDO,
            ),

            actionSpriteButton(
                x = leftPos + 165,
                y = topPos + 77,
                uOffset = 305,
                vOffset = 29,
                width = 18,
                height = 11,
                action = SplicingTableAction.REDO,
            ),

            // clipboard

            actionSpriteButton(
                x = leftPos + 27,
                y = topPos + 64,
                uOffset = 256,
                vOffset = 16,
                width = 11,
                height = 11,
                action = SplicingTableAction.CUT,
            ),

            actionSpriteButton(
                x = leftPos + 40,
                y = topPos + 64,
                uOffset = 269,
                vOffset = 16,
                width = 11,
                height = 11,
                action = SplicingTableAction.COPY,
            ),

            *listOf(
                SplicingTableAction.PASTE_SPLAT to false,
                SplicingTableAction.PASTE to true,
            ).map { (action, needsShiftDown) ->
                object : SpriteButton(
                    x = leftPos + 27,
                    y = topPos + 77,
                    uOffset = 256,
                    vOffset = 29,
                    width = 24,
                    height = 11,
                    message = action.buttonText,
                    onPress = action.onPress,
                ) {
                    override fun testVisible() = hasShiftDown() == needsShiftDown
                } to action.test
            }.toTypedArray(),
        )

        allButtons.forEach(::addRenderableWidget)

        val iotaButtons = (0 until IOTA_BUTTONS).map { offset ->
            addRenderableWidget(IotaButton(offset))
        }

        for (button in iotaButtons) {
            addRenderableOnly(IotaSelection(button))
        }

        addRenderableWidget(
            MediaBar(
                menu = menu,
                x = leftPos + 225,
                y = topPos + 169,
                uOffset = 481,
                vOffset = 328,
                width = 6,
                height = 16,
            )
        )

        reloadData()
    }

    private fun exportToSystemClipboard() {
        val export = data.list?.toHexpatternSource() ?: return
        minecraft?.keyboardHandler?.clipboard = export
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
        for (child in children()) {
            if (child is SplicingTableButton) {
                child.reload()
            }
        }
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

    private fun setActive(buttons: Sequence<AbstractButton>, active: Boolean) {
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
                val iotaView = data.list?.getOrNull(index)
                if (null != iotaView) {
                    message = index.toString().asTranslatedComponent.withStyle(*formats)
                    tooltip = Tooltip.create(iotaView.name)
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

    private fun actionSpriteButton(
        x: Int,
        y: Int,
        uOffset: Int,
        vOffset: Int,
        width: Int,
        height: Int,
        action: SplicingTableAction,
    ) = SpriteButton(
        x, y, uOffset, vOffset, width, height,
        message = action.buttonText,
        onPress = action.onPress,
    ) to action.test

    private fun actionHexagonSpriteButton(
        x: Int,
        y: Int,
        uOffset: Int,
        vOffset: Int,
        width: Int,
        height: Int,
        triangleHeight: Int,
        isHorizontal: Boolean,
        action: SplicingTableAction,
    ) = HexagonSpriteButton(
        x, y, uOffset, vOffset, width, height, triangleHeight, isHorizontal,
        message = action.buttonText,
        onPress = action.onPress,
    ) to action.test

    private val SplicingTableAction.buttonText get() = buttonText(name.lowercase())

    private val SplicingTableAction.onPress get(): () -> Unit = { menu.table.runAction(this, null, selection) }

    private val SplicingTableAction.test get(): () -> Boolean = { value.test(data, selection) }

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
        if (hasStaffItem && isInStaffGrid(mouseX, mouseY, button)) {
            guiSpellcasting.mouseClicked(mouseX, mouseY, button)
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (hasStaffItem && isInStaffGrid(mouseX, mouseY, button)) {
            guiSpellcasting.mouseDragged(mouseX, mouseY, button, dragX, dragY)
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // don't keep holding onto a line if the mouse is released outside of the grid
        guiSpellcasting.mouseReleased(mouseX, mouseY, button)
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun onClose() {
        // support pressing esc to cancel drawing a pattern
        if (hasStaffItem) {
            guiSpellcasting.onClose()
            if (minecraft?.screen != null) return
        }
        super.onClose()
    }

    private fun isInStaffGrid(mouseX: Double, mouseY: Double, button: Int) =
        staffMinX <= mouseX && mouseX <= staffMaxX && staffMinY <= mouseY && mouseY <= staffMaxY
        && hasClickedOutside(mouseX, mouseY, leftPos, topPos, button) // avoid interacting with the grid when inserting the staff item

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
            // storage/media
            && (mouseX < guiLeft + 192 || mouseY < guiTop + 103 || mouseX >= guiLeft + 192 + 46 || mouseY >= guiTop + 103 + 87)
            // staff
            && (mouseX < guiLeft - 22 || mouseY < guiTop + 167 || mouseX >= guiLeft - 22 + 20 || mouseY >= guiTop + 167 + 20)
        )
    }

    // rendering

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        staffButtons.forEach { it.visible = hasStaffItem }

        renderBackground(guiGraphics)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // main gui + right storage slots
        blitSprite(guiGraphics, x = leftPos, y = topPos, uOffset = 256, vOffset = 128, width = imageWidth + 46, height = imageHeight)

        if (data.isListReadable) {
            // fancy media lines next to list slot
            blitSprite(guiGraphics, x = leftPos + 56, y = topPos + 75, uOffset = 312, vOffset = 52, width = 80, height = 2)
        }

        // media slot
        if (!hasMediaItem) {
            // dust background
            blitSprite(guiGraphics, x = leftPos + 205, y = topPos + 169, uOffset = 461, vOffset = 328, width = 16, height = 16)
        }
        if (menu.media > 0) {
            // sparkly stars
            blitSprite(guiGraphics, x = leftPos + 193, y = topPos + 170, uOffset = 449, vOffset = 328, width = 10, height = 14)
        }

        // staff grid
        if (hasStaffItem) {
            // background
            RenderSystem.enableBlend()
            guiGraphics.setColor(1f, 1f, 1f, 0.25f)
            blitRepeating(guiGraphics, x = staffMinX, y = staffMinY, uOffset = 240, vOffset = 0, width = 8, height = 8, xTiles = staffWidth / 8, yTiles = staffHeight / 8)
            guiGraphics.setColor(1f, 1f, 1f, 1f)
            RenderSystem.disableBlend()

            // pattern grid
            renderGuiSpellcasting(guiGraphics, mouseX, mouseY, partialTick)

            // top
            blitRepeating(guiGraphics, x = staffMinX, y = staffMinY - 7, uOffset = 208, vOffset = 1, width = 32, height = 10, xTiles = staffWidth / 32, yTiles = 1)
            // bottom
            blitRepeating(guiGraphics, x = staffMinX, y = staffMaxY - 3, uOffset = 208, vOffset = 45, width = 32, height = 10, xTiles = staffWidth / 32, yTiles = 1)
            // left
            blitRepeating(guiGraphics, x = staffMinX - 7, y = staffMinY, uOffset = 201, vOffset = 12, width = 10, height = 32, xTiles = 1, yTiles = staffHeight / 32)
            // right
            blitRepeating(guiGraphics, x = staffMaxX - 3, y = staffMinY, uOffset = 237, vOffset = 12, width = 10, height = 32, xTiles = 1, yTiles = staffHeight / 32)

            // top left
            blitSprite(guiGraphics, x = staffMinX - 7, y = staffMinY - 8, uOffset = 176, vOffset = 0, width = 8, height = 9)
            // top right
            blitSprite(guiGraphics, x = staffMaxX - 1, y = staffMinY - 7, uOffset = 184, vOffset = 1, width = 9, height = 8)
            // bottom left
            blitSprite(guiGraphics, x = staffMinX - 8, y = staffMaxY - 1, uOffset = 175, vOffset = 9, width = 9, height = 8)
            // bottom right
            blitSprite(guiGraphics, x = staffMaxX - 1, y = staffMaxY - 1, uOffset = 184, vOffset = 9, width = 8, height = 9)

            // staff slot without icon
            blitSprite(guiGraphics, x = leftPos - 24, y = topPos + 165, uOffset = 232, vOffset = 293, width = 23, height = 24)
        } else {
            // staff slot with icon
            blitSprite(guiGraphics, x = leftPos - 24, y = topPos + 165, uOffset = 232, vOffset = 328, width = 23, height = 24)
        }
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // iota index labels
        for (label in IndexLabel.entries) {
            val index = viewStartIndex + label.offset
            if (data.isInRange(index)) {
                drawNumber(guiGraphics, label.x, label.y, index, label.color)
            }
        }
    }

    private fun renderGuiSpellcasting(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.enableScissor(staffMinX, staffMinY, staffMaxX, staffMaxY)
        guiSpellcasting.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.disableScissor()
    }

    companion object {
        val TEXTURE = HexDebug.id("textures/gui/splicing_table.png")

        const val IOTA_BUTTONS = 9

        const val MAX_DIGIT_LEN = 4
        val MAX_DIGIT = 10f.pow(MAX_DIGIT_LEN).toInt() - 1

        fun getInstance() = Minecraft.getInstance().screen as? SplicingTableScreen

        fun drawNumber(guiGraphics: GuiGraphics, x: Int, y: Int, number: Int, color: Color) {
            guiGraphics.setColor(color)
            var i = 0
            var rest = number.coerceIn(0, MAX_DIGIT)
            do {
                drawDigit(guiGraphics, x + (MAX_DIGIT_LEN - i - 1) * 5, y, rest % 10)
                rest /= 10
                i++
            } while (rest > 0)
            guiGraphics.setColor(1f, 1f, 1f, 1f)
        }

        fun drawDigit(guiGraphics: GuiGraphics, x: Int, y: Int, digit: Int) {
            blitSprite(guiGraphics, x, y, 288 + 5 * digit, 0, 4, 5)
        }

        fun blitRepeating(
            guiGraphics: GuiGraphics,
            x: Int,
            y: Int,
            uOffset: Int,
            vOffset: Int,
            width: Int,
            height: Int,
            xTiles: Int,
            yTiles: Int,
        ) {
            for (yIndex in 0 until yTiles) {
                for (xIndex in 0 until xTiles) {
                    blitSprite(
                        guiGraphics,
                        x = x + width * xIndex,
                        y = y + height * yIndex,
                        uOffset = uOffset,
                        vOffset = vOffset,
                        width = width,
                        height = height,
                    )
                }
            }
        }

        fun blitSprite(guiGraphics: GuiGraphics, x: Int, y: Int, uOffset: Int, vOffset: Int, width: Int, height: Int) {
            guiGraphics.blit(TEXTURE, x, y, uOffset.toFloat(), vOffset.toFloat(), width, height, 512, 512)
        }

        // TODO: remove when we have icons for the remaining buttons
        fun button(name: String, onPress: Button.OnPress): Button.Builder {
            val message = buttonKey(name).asTranslatedComponent
            return Button.builder(message, onPress)
                .tooltip(Tooltip.create(message))
        }

        fun buttonText(name: String, vararg args: Any) = buttonKey(name).asTranslatedComponent(*args)
        fun tooltipText(name: String, vararg args: Any) = tooltipKey(name).asTranslatedComponent(*args)

        fun buttonKey(name: String) = splicingTableKey("button.$name")
        fun tooltipKey(name: String) = splicingTableKey("tooltip.$name")
        fun splicingTableKey(name: String) = "text.hexdebug.splicing_table.$name"
    }

    inner class IotaButton(val offset: Int) : BaseIotaButton(
        x = leftPos + 15 + 18 * offset,
        y = topPos + 20,
    ) {
        override val index get() = viewStartIndex + offset

        override val iotaView get() = data.list?.getOrNull(index)

        override fun onPress() {
            onSelectIota(index)
        }

        init {
            reload()
        }
    }

    inner class IotaSelection(button: IotaButton) : Renderable {
        private val offset by button::offset
        private val index by button::index
        private val backgroundType by button::backgroundType

        override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            if (!data.isInRange(index)) return
            val backgroundType = backgroundType ?: return
            when (val selection = selection) {
                is Selection.Range -> if (index in selection) {
                    drawRangeSelection(
                        guiGraphics, offset, backgroundType,
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

        private fun drawRangeSelection(guiGraphics: GuiGraphics, offset: Int, type: IotaBackgroundType, leftEdge: Boolean, rightEdge: Boolean) {
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
    }
}

fun GuiGraphics.setColor(color: Color) = setColor(color.fred, color.fgreen, color.fblue, color.falpha)
