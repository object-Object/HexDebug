package gay.`object`.hexdebug.gui.splicing

import at.petrak.hexcasting.api.casting.eval.SpecialPatterns
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.utils.*
import at.petrak.hexcasting.client.gui.GuiSpellcasting
import at.petrak.hexcasting.client.render.drawLineSeq
import at.petrak.hexcasting.client.render.findDupIndices
import at.petrak.hexcasting.client.render.getCenteredPattern
import at.petrak.hexcasting.client.render.makeZappy
import at.petrak.hexcasting.common.lib.HexSounds
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity
import gay.`object`.hexdebug.splicing.IotaClientView
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableAction
import gay.`object`.hexdebug.utils.joinToComponent
import gay.`object`.hexdebug.utils.simpleString
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.TextColor
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.phys.Vec2
import java.awt.Color
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.function.BiConsumer
import kotlin.math.ceil
import kotlin.math.max
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
                action = SplicingTableAction.CUT,
            ),

            actionSpriteButton(
                x = 40,
                y = 64,
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

        val iotaButtons = (0 until IOTA_BUTTONS).map { offset ->
            addRenderableWidget(IotaButton(offset))
        }

        for (button in iotaButtons) {
            addRenderableOnly(IotaSelection(button))
        }

        addRenderableWidget(
            MediaBar(
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

    private fun button(name: String, onPress: Button.OnPress) = button(buttonKey(name).asTranslatedComponent, onPress)

    private fun button(message: Component, onPress: Button.OnPress) =
        Button.builder(message, onPress)
            .tooltip(Tooltip.create(message))

    private fun buttonText(name: String, vararg args: Any) = buttonKey(name).asTranslatedComponent(*args)
    private fun tooltipText(name: String, vararg args: Any) = tooltipKey(name).asTranslatedComponent(*args)

    private fun buttonKey(name: String) = splicingTableKey("button.$name")
    private fun tooltipKey(name: String) = splicingTableKey("tooltip.$name")
    private fun splicingTableKey(name: String) = "text.hexdebug.splicing_table.$name"

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

            // fuel bar

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

    private fun drawNumber(guiGraphics: GuiGraphics, x: Int, y: Int, number: Int, color: Color) {

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

    private fun drawDigit(guiGraphics: GuiGraphics, x: Int, y: Int, digit: Int) {
        blitSprite(guiGraphics, x, y, 288 + 5 * digit, 0, 4, 5)
    }

    private fun blitRepeating(
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

    private fun blitSprite(guiGraphics: GuiGraphics, x: Int, y: Int, uOffset: Int, vOffset: Int, width: Int, height: Int) {
        guiGraphics.blit(TEXTURE, x, y, uOffset.toFloat(), vOffset.toFloat(), width, height, 512, 512)
    }

    companion object {
        val TEXTURE = HexDebug.id("textures/gui/splicing_table.png")

        const val IOTA_BUTTONS = 9

        const val MAX_DIGIT_LEN = 4
        val MAX_DIGIT = 10f.pow(MAX_DIGIT_LEN).toInt() - 1

        fun getInstance() = Minecraft.getInstance().screen as? SplicingTableScreen
    }

    enum class IotaBackgroundType {
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

    // FIXME: no tooltip hover over rightmost pixel ???
    inner class MediaBar(
        x: Int,
        y: Int,
        private val uOffset: Int,
        private val vOffset: Int,
        width: Int,
        height: Int,
    ) : AbstractWidget(x, y, width, height, Component.empty()) {
        private val media get() = menu.media
        private val maxMedia get() = SplicingTableBlockEntity.maxMedia
        private val fullness get() = if (maxMedia > 0) media.toDouble() / maxMedia else 0.0

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            if (media > 0 && maxMedia > 0) {
                val visibleHeight = ceil(height * fullness).toInt().coerceIn(0, height)
                val yOffset = height - visibleHeight
                blitSprite(
                    guiGraphics,
                    x = x,
                    y = y + yOffset,
                    uOffset = uOffset,
                    vOffset = vOffset + yOffset,
                    width = width,
                    height = visibleHeight,
                )
            }
        }

        override fun updateTooltip() {
            // stolen from ItemMediaHolder
            // FIXME: i don't think this is the right way to do this :/
            tooltip = if (maxMedia > 0) {
                val color = TextColor.fromRgb(mediaBarColor(media, maxMedia))

                val mediamount = DUST_AMOUNT.format(media.toDouble() / MediaConstants.DUST_UNIT)
                    .asTextComponent
                    .styledWith { it.withColor(HEX_COLOR) }

                val percentFull = (PERCENTAGE.format(100f * fullness) + "%")
                    .asTextComponent
                    .styledWith { it.withColor(color) }

                val maxCapacity = "hexcasting.tooltip.media"
                    .asTranslatedComponent(DUST_AMOUNT.format(maxMedia.toDouble() / MediaConstants.DUST_UNIT))
                    .styledWith { it.withColor(HEX_COLOR) }

                Tooltip.create(
                    "hexcasting.tooltip.media_amount.advanced"
                        .asTranslatedComponent(mediamount, maxCapacity, percentFull)
                )
            } else null
            super.updateTooltip()
        }

        // TODO: implement?
        override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {}
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

        open fun reload() {}
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

    inner class IotaButton(val offset: Int) : HexagonButton(
        x = 15 + 18 * offset,
        y = 20,
        width = 18,
        height = 21,
        triangleHeight = 5,
        isHorizontal = false,
        message = "TODO".asTextComponent,
    ) {
        val index get() = viewStartIndex + offset

        private val patternWidth = 16f
        private val patternHeight = 13f

        var backgroundType: IotaBackgroundType? = null
            private set

        private var zappyPoints: List<Vec2>? = null
        private var typeUVOffset: Pair<Int, Int>? = null
        private var typeColor: Color? = null

        override val uOffset get() = 352 + 20 * (backgroundType?.ordinal ?: 0)
        override val vOffset = 0

        override fun onPress() {
            onSelectIota(viewStartIndex + offset)
        }

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            if (data.isInRange(index) && backgroundType != null) {
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)

                val zappyPoints = zappyPoints
                val typeUVOffset = typeUVOffset

                if (zappyPoints != null) {
                    renderPattern(guiGraphics, zappyPoints)
                } else if (typeUVOffset != null) {
                    val (uOffset, vOffset) = typeUVOffset
                    guiGraphics.setColor(typeColor ?: Color.WHITE)
                    blitSprite(guiGraphics, x = x + 2, y = y + 3, uOffset = uOffset, vOffset = vOffset, width = 14, height = 14)
                    guiGraphics.setColor(1f, 1f, 1f, 1f)
                }
            }
        }

        private fun renderPattern(guiGraphics: GuiGraphics, zappyPoints: List<Vec2>) {
            val ps = guiGraphics.pose()

            ps.pushPose()

            RenderSystem.enableBlend()
            RenderSystem.setShader(GameRenderer::getPositionColorShader)
            RenderSystem.disableCull()
            RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            )

            ps.translate(x + 1f, y + 4f, 100f)
            ps.translate(patternWidth / 2f, patternHeight / 2f, 0f)

            val mat = ps.last().pose()

            val outer = 0xff_d2c8c8.toInt()
            val innerLight = 0xc8_aba2a2.toInt()
            val innerDark = 0xc8_322b33.toInt()

            drawLineSeq(mat, zappyPoints, width = 2f, z = 0f, tail = outer, head = outer)
            drawLineSeq(mat, zappyPoints, width = 2f * 0.4f, z = 0f, tail = innerDark, head = innerLight)

            ps.popPose()
        }

        override fun reload() {
            active = false
            backgroundType = null
            tooltip = null
            zappyPoints = null
            typeUVOffset = null
            typeColor = null

            // don't enable the button or display anything if this index is out of range
            val iotaView = data.list?.getOrNull(index) ?: return

            val iotaType = IotaType.getTypeFromTag(iotaView.tag)
            val iotaData = iotaView.tag.get(HexIotaTypes.KEY_DATA)

            active = true

            backgroundType = when (iotaType) {
                HexIotaTypes.PATTERN -> IotaBackgroundType.PATTERN
                else -> IotaBackgroundType.GENERIC
            }

            val details = mutableListOf(
                tooltipText("index", index),
            )
            val advanced = mutableListOf<MutableComponent>()

            // if it's an invalid iota, just set the background type and tooltip, then return
            // (so that we don't need null checks later)
            if (iotaType == null || iotaData == null) {
                tooltip = createTooltip(getBrokenIotaName(), details, advanced)
                typeColor = Color.LIGHT_GRAY
                return
            }

            val iotaTypeId = HexIotaTypes.REGISTRY.getKey(iotaType)
            if (iotaTypeId != null) {
                advanced += iotaTypeId.toString().asTextComponent
            }

            typeColor = Color(iotaType.color(), true)

            // type-specific rendering
            typeUVOffset = when (iotaType) {
                HexIotaTypes.DOUBLE -> getTypeUVOffset(0, 0)
                HexIotaTypes.VEC3 -> getTypeUVOffset(1, 0)
                HexIotaTypes.ENTITY -> getTypeUVOffset(2, 0)
                HexIotaTypes.BOOLEAN -> getTypeUVOffset(1, 1)
                HexIotaTypes.LIST -> getTypeUVOffset(2, 1)
                HexIotaTypes.NULL -> getTypeUVOffset(0, 2)
                HexIotaTypes.GARBAGE -> getTypeUVOffset(1, 2)
                HexIotaTypes.CONTINUATION -> getTypeUVOffset(2, 2)

                // custom pattern rendering
                HexIotaTypes.PATTERN -> iotaView.pattern?.let { pattern ->
                    advanced += tooltipText("signature", pattern.simpleString())

                    val (_, dots) = getCenteredPattern(
                        pattern = pattern,
                        width = patternWidth,
                        height = patternHeight,
                        minSize = 8f,
                    )
                    zappyPoints = makeZappy(
                        barePoints = dots,
                        dupIndices = findDupIndices(pattern.positions()),
                        hops = 1,
                        variance = 0f,
                        speed = 0f,
                        flowIrregular = 0f,
                        readabilityOffset = 0f,
                        lastSegmentLenProportion = 1f,
                        seed = 0.0,
                    )

                    null
                }

                else -> when (iotaTypeId?.toString()) {
                    // addon patterns (so we don't need to actually depend on the addons)
                    "moreiotas:string" -> getTypeUVOffset(3, 0)
                    "moreiotas:matrix" -> getTypeUVOffset(3, 1)
                    "hexal:iota_type",
                    "hexal:entity_type",
                    "hexal:item_type",
                    "moreiotas:iota_type",
                    "moreiotas:entity_type",
                    "moreiotas:item_type" -> getTypeUVOffset(3, 2)
                    // generic type icon
                    else -> getTypeUVOffset(0, 1)
                }
            }

            tooltip = createTooltip(iotaView.name, details, advanced)
        }

        private fun getBrokenIotaName() = IotaType.getDisplay(CompoundTag()) // "a broken iota"

        private fun createTooltip(
            name: Component,
            details: Collection<MutableComponent>,
            advanced: Collection<MutableComponent>,
        ): Tooltip {
            var lines = sequenceOf(name)
            lines += details.asSequence().map { it.gray }
            if (Minecraft.getInstance().options.advancedItemTooltips) {
                lines += advanced.asSequence().map { it.darkGray }
            }
            return Tooltip.create(lines.toList().joinToComponent("\n"))
        }

        private fun getTypeUVOffset(xIndex: Int, yIndex: Int) = Pair(337 + 16 * xIndex, 57 + 16 * yIndex)

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

fun GuiGraphics.setColor(color: Color) = setColor(color.fred, color.fgreen, color.fblue, color.falpha)

// stolen from ItemMediaHolder
val HEX_COLOR = TextColor.fromRgb(0xb38ef3)
val PERCENTAGE = DecimalFormat("####").apply { roundingMode = RoundingMode.DOWN }
val DUST_AMOUNT = DecimalFormat("###,###.##")

private fun List<IotaClientView>.toHexpatternSource(): String {
    var depth = 0
    return joinToString("\n") {
        if (it.pattern == SpecialPatterns.RETROSPECTION) depth--
        val indent = " ".repeat(max(0, 4 * depth))
        if (it.pattern == SpecialPatterns.INTROSPECTION) depth++
        indent + it.hexpatternSource
    }
}
