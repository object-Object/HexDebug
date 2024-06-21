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
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.phys.Vec2
import java.awt.Color
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
            // selection/view

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

            // move view

            SpriteButton(
                x = 4,
                y = 25,
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
                x = 178,
                y = 25,
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
                x = 67,
                y = 57,
                uOffset = 256,
                vOffset = 48,
                width = 18,
                height = 16,
                triangleHeight = 4,
                isHorizontal = true,
                action = SplicingTableAction.NUDGE_LEFT,
            ),

            actionHexagonSpriteButton(
                x = 107,
                y = 57,
                uOffset = 276,
                vOffset = 48,
                width = 18,
                height = 16,
                triangleHeight = 4,
                isHorizontal = true,
                action = SplicingTableAction.NUDGE_RIGHT,
            ),

            actionHexagonSpriteButton(
                x = 67,
                y = 79,
                uOffset = 256,
                vOffset = 66,
                width = 18,
                height = 16,
                triangleHeight = 4,
                isHorizontal = true,
                action = SplicingTableAction.DELETE,
            ),


            actionHexagonSpriteButton(
                x = 107,
                y = 79,
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
                x = 144,
                y = 64,
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
                x = 165,
                y = 64,
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
                x = 144,
                y = 77,
                uOffset = 284,
                vOffset = 29,
                width = 18,
                height = 11,
                action = SplicingTableAction.UNDO,
            ),

            actionSpriteButton(
                x = 165,
                y = 77,
                uOffset = 305,
                vOffset = 29,
                width = 18,
                height = 11,
                action = SplicingTableAction.REDO,
            ),

            // clipboard

            actionSpriteButton(
                x = 27,
                y = 64,
                uOffset = 256,
                vOffset = 16,
                width = 11,
                height = 11,
                action = SplicingTableAction.COPY,
            ),

            actionSpriteButton(
                x = 40,
                y = 64,
                uOffset = 269,
                vOffset = 16,
                width = 11,
                height = 11,
                action = SplicingTableAction.CUT,
            ),

            *listOf(
                SplicingTableAction.PASTE_SPLAT to false,
                SplicingTableAction.PASTE to true,
            ).map { (action, needsShiftDown) ->
                object : SpriteButton(
                    x = 27,
                    y = 77,
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

        for (offset in 0 until IOTA_BUTTONS) {
            addRenderableWidget(IotaButton(offset))
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

    private fun button(name: String, onPress: Button.OnPress) = button(buttonKey(name).asTranslatedComponent, onPress)

    private fun button(message: Component, onPress: Button.OnPress) =
        Button.builder(message, onPress)
            .tooltip(Tooltip.create(message))

    private fun buttonText(name: String) = buttonKey(name).asTranslatedComponent

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
            && (mouseX < guiLeft + 192 || mouseY < guiTop + 103 || mouseX >= guiLeft + 192 + 46 || mouseY >= guiTop + 103 + 68)
            // media/staff (FIXME: placeholder)
            && (mouseX < guiLeft + 192 || mouseY < guiTop + 67 || mouseX >= guiLeft + 192 + 18 || mouseY >= guiTop + 67 + 36)
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
        for (label in IndexLabel.entries) {
            val index = viewStartIndex + label.offset
            if (data.isInRange(index)) {
                drawNumber(guiGraphics, label.x, label.y, index, label.color)
            }
        }
    }

    private fun renderStaffGrid(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.blit(BORDER, leftPos - 200, topPos, 0, 0, 200, 200)
        guiGraphics.enableScissor(staffMinX, staffMinY, staffMaxX, staffMaxY)
        guiSpellcasting.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.disableScissor()
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

    private fun drawNumber(guiGraphics: GuiGraphics, x: Int, y: Int, number: Int, color: Color) {

        guiGraphics.setColor(color.fred, color.fgreen, color.fblue, color.falpha)
        var i = 0
        var rest = number.coerceIn(0, MAX_DIGIT)
        do {
            drawDigit(guiGraphics, x + (MAX_DIGIT_LEN - i - 1) * 5, y, rest % 10)
            rest /= 10
            i++
        } while (rest > 0)
        guiGraphics.setColor(1f, 1f, 1f, 1f)
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

    enum class IndexLabel(val x: Int, val y: Int, val offset: Int, val color: Color) {
        LEFT(18, 47, 0, 0x886539),
        MIDDLE(86, 47, 4, 0x77637c),
        RIGHT(155, 47, 8, 0x886539);

        constructor(x: Int, y: Int, offset: Int, color: Int) : this(x, y, offset, Color(color))
    }

    abstract inner class SplicingTableButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        message: Component,
    ) : AbstractButton(leftPos + x, topPos + y, width, height, message) {
        init {
            tooltip = Tooltip.create(message)
        }

        abstract val uOffset: Int
        abstract val vOffset: Int

        open fun testVisible() = visible

        fun testHitbox(mouseX: Int, mouseY: Int) = testHitbox(mouseX.toDouble(), mouseY.toDouble())

        open fun testHitbox(mouseX: Double, mouseY: Double): Boolean =
            mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height

        override fun isMouseOver(mouseX: Double, mouseY: Double) = isActive && visible && testHitbox(mouseX, mouseY)

        override fun clicked(mouseX: Double, mouseY: Double) = isActive && visible && testHitbox(mouseX, mouseY)

        override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            visible = testVisible()
            if (visible) {
                isHovered = testHitbox(mouseX, mouseY)
                renderWidget(guiGraphics, mouseX, mouseY, partialTick)
                updateTooltip()
            }
        }

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            blitSprite(guiGraphics, x, y, uOffset, vOffset, width, height)
        }

        override fun updateWidgetNarration(output: NarrationElementOutput) = defaultButtonNarrationText(output)
    }

    /**
     - x/y/width/height refer to the total size of the hexagon, including the areas outside of the hitbox
     - triangleHeight is the distance from the edge of the main rectangle to one of the pointy sides
     - isHorizontal is true if the pointy sides point to the left/right

     For example, a horizontal hexagon:

       |----------------|  <- width

     -     /----------\
     |    /           |\
     |   /            | \
     |  |             |--|  <- triangleHeight
     |   \            | /
     |    \           |/
     -     \----------/

     ^ height
    */
    abstract inner class HexagonButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        triangleHeight: Int,
        isHorizontal: Boolean,
        message: Component,
    ) : SplicingTableButton(x, y, width, height, message) {
        private val triangleOffset = if (isHorizontal) {
            vec2(triangleHeight, 0)
        } else {
            vec2(0, triangleHeight)
        }

        private val rectX = this.x + triangleOffset.x
        private val rectY = this.y + triangleOffset.y

        private val rectWidth = width - 2 * triangleOffset.x
        private val rectHeight = height - 2 * triangleOffset.y

        private val rectTopLeft = vec2(rectX, rectY)
        private val rectTopRight = vec2(rectX + rectWidth, rectY)
        private val rectBottomLeft = vec2(rectX, rectY + rectHeight)
        private val rectBottomRight = vec2(rectX + rectWidth, rectY + rectHeight)

        private val triangle1: Triple<Vec2, Vec2, Vec2>
        private val triangle2: Triple<Vec2, Vec2, Vec2>

        init {
            if (isHorizontal) {
                val triangleY = this.y + height.toFloat() / 2f
                triangle1 = Triple(
                    rectTopLeft,
                    rectBottomLeft,
                    vec2(this.x, triangleY),
                )
                triangle2 = Triple(
                    rectTopRight,
                    rectBottomRight,
                    vec2(this.x + width, triangleY),
                )
            } else {
                val triangleX = this.x + height.toFloat() / 2f
                triangle1 = Triple(
                    rectTopLeft,
                    rectTopRight,
                    vec2(triangleX, this.y),
                )
                triangle2 = Triple(
                    rectBottomLeft,
                    rectBottomRight,
                    vec2(triangleX, this.y + height),
                )
            }
        }

        override fun testHitbox(mouseX: Double, mouseY: Double): Boolean {
            // full size check
            if (!super.testHitbox(mouseX, mouseY)) {
                return false
            }

            val mousePos = vec2(mouseX, mouseY)
            return (
                (mouseX >= rectTopLeft.x && mouseY >= rectTopLeft.y && mouseX < rectBottomRight.x && mouseY < rectBottomRight.y)
                || pointInTriangle(mousePos, triangle1)
                || pointInTriangle(mousePos, triangle2)
            )
        }
    }

    inner class IotaButton(private val offset: Int) : HexagonButton(
        x = 15 + 18 * offset,
        y = 20,
        width = 18,
        height = 21,
        triangleHeight = 5,
        isHorizontal = false,
        message = "TODO".asTextComponent,
    ) {
        val index get() = viewStartIndex + offset

        // FIXME: get actual type
        val type get() = if (index % 2 == 0) IotaRenderType.GENERIC else IotaRenderType.PATTERN

        override val uOffset get() = 352 + 20 * type.ordinal
        override val vOffset = 0

        override fun onPress() {
            onSelectIota(viewStartIndex + offset)
        }

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            if (data.isInRange(index)) {
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
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

    open inner class SpriteButton(
        x: Int,
        y: Int,
        override val uOffset: Int,
        override val vOffset: Int,
        width: Int,
        height: Int,
        message: Component,
        private val onPress: () -> Unit,
    ) : SplicingTableButton(x, y, width, height, message) {
        override fun onPress() = onPress.invoke()
    }

    inner class HexagonSpriteButton(
        x: Int,
        y: Int,
        override val uOffset: Int,
        override val vOffset: Int,
        width: Int,
        height: Int,
        triangleHeight: Int,
        isHorizontal: Boolean,
        message: Component,
        private val onPress: () -> Unit,
    ) : HexagonButton(x, y, width, height, triangleHeight, isHorizontal, message) {
        override fun onPress() = onPress.invoke()
    }
 }

fun pointInTriangle(pt: Vec2, triangle: Triple<Vec2, Vec2, Vec2>): Boolean {
    val (v1, v2, v3) = triangle
    return pointInTriangle(pt, v1, v2, v3)
}

// https://stackoverflow.com/a/2049593
fun pointInTriangle(pt: Vec2, v1: Vec2, v2: Vec2, v3: Vec2): Boolean {
    val d1 = sign(pt, v1, v2)
    val d2 = sign(pt, v2, v3)
    val d3 = sign(pt, v3, v1)

    val hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0)
    val hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0)

    return !(hasNeg && hasPos)
}

fun sign(p1: Vec2, p2: Vec2, p3: Vec2): Float {
    return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
}

fun vec2(x: Number, y: Number) = Vec2(x.toFloat(), y.toFloat())

val Color.fred get() = red.toFloat() / 255f
val Color.fgreen get() = green.toFloat() / 255f
val Color.fblue get() = blue.toFloat() / 255f
val Color.falpha get() = alpha.toFloat() / 255f
