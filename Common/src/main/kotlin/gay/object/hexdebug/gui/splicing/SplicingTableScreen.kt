package gay.`object`.hexdebug.gui.splicing

import at.petrak.hexcasting.api.utils.asTranslatedComponent
import at.petrak.hexcasting.client.gui.GuiSpellcasting
import com.mojang.blaze3d.systems.RenderSystem
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.config.HexDebugConfig
import gay.`object`.hexdebug.gui.splicing.widgets.*
import gay.`object`.hexdebug.splicing.IOTA_BUTTONS
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableAction
import gay.`object`.hexdebug.splicing.toHexpatternSource
import gay.`object`.hexdebug.utils.falpha
import gay.`object`.hexdebug.utils.fblue
import gay.`object`.hexdebug.utils.fgreen
import gay.`object`.hexdebug.utils.fred
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Inventory
import java.awt.Color
import java.util.function.BiConsumer
import kotlin.math.*

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

    val data get() = menu.clientView
    val selection get() = menu.selection
    val viewStartIndex get() = menu.viewStartIndex

    private val hasMediaItem get() = menu.mediaSlot.hasItem()
    val hasStaffItem get() = menu.staffSlot.hasItem()

    private val hasMediaForAction get() = menu.media >= HexDebugConfig.server.splicingTableMediaCost

    private var prevSelection: Selection? = selection
    private var prevViewStartIndex = viewStartIndex
    private var prevMedia = menu.media

    var guiSpellcasting = GuiSpellcasting(
        InteractionHand.MAIN_HAND, mutableListOf(), listOf(), null, 1
    ).apply {
        mixin.`onDrawSplicingTablePattern$hexdebug` = BiConsumer { pattern, index ->
            menu.table.drawPattern(null, pattern, index)
        }
    }

    // should be multiples of 32, since that's how big the edge parts are
    private val staffWidth = 32 * 7
    private val staffHeight = 32 * 7

    // 32 * 6 is the height that matches the main GUI
    // so offset up if taller or down if shorter
    private val staffOffsetY: Int = ((32 * 6) - staffHeight) / 2

    // relative to top left (for hasClickedOutside)
    private val staffMaxXOffset = -14
    private val staffMinXOffset = staffMaxXOffset - staffWidth
    private val staffMinYOffset = staffOffsetY
    private val staffMaxYOffset = staffMinYOffset + staffHeight

    private val staffSlotWidth = 23
    private val staffSlotHeight = 24

    private val staffSlotMinXOffset = -24
    private val staffSlotMaxXOffset = staffSlotMinXOffset + staffSlotWidth
    private val staffSlotMinYOffset = 165
    private val staffSlotMaxYOffset = staffSlotMinYOffset + staffSlotHeight

    private val storageMinXOffset = 192
    private val storageMaxXOffset = storageMinXOffset + 46
    private val storageMinYOffset = 103
    private val storageMaxYOffset = storageMinYOffset + 87

    // absolute
    val staffMinX get() = leftPos + staffMinXOffset
    val staffMaxX get() = leftPos + staffMaxXOffset
    val staffMinY get() = topPos + staffMinYOffset
    val staffMaxY get() = topPos + staffMaxYOffset

    val staffSlotMinX get() = leftPos + staffSlotMinXOffset
    val staffSlotMaxX get() = leftPos + staffSlotMaxXOffset
    val staffSlotMinY get() = topPos + staffSlotMinYOffset
    val staffSlotMaxY get() = topPos + staffSlotMaxYOffset

    val storageMinX get() = leftPos + storageMinXOffset
    val storageMaxX get() = leftPos + storageMaxXOffset
    val storageMinY get() = topPos + storageMinYOffset
    val storageMaxY get() = topPos + storageMaxYOffset

    val exportButtonX get() = leftPos + 194
    val exportButtonY get() = topPos + 18
    val exportButtonWidth = 24
    val exportButtonHeight = 24

    val castButtonX get() = leftPos + 194
    val castButtonY get() = topPos + 64
    val castButtonWidth = 24
    val castButtonHeight = 24

    private val predicateButtons = mutableListOf<Pair<AbstractButton, () -> Boolean>>()

    private var clearGridButton: SpriteButton? = null

    private var castingCooldown = 0

    override fun init() {
        super.init()

        guiSpellcasting.init(minecraft!!, width, height)

        titleLabelX = (imageWidth - font.width(title)) / 2

        predicateButtons.clear()

        clearGridButton = object : SpriteButton(
            x = (staffMinX + staffMaxX) / 2 - 19,
            y = staffMaxY - 10,
            uOffset = 192,
            vOffset = 293,
            width = 38,
            height = 25,
            message = buttonText("clear_grid"),
            onPress = {
                guiSpellcasting.mixin.`clearPatterns$hexdebug`()
            },
        ) {
            override val uOffsetHovered get() = uOffset - 48
            override val vOffsetHovered get() = vOffset

            override val uOffsetDisabled get() = uOffset
            override val vOffsetDisabled get() = vOffset

            override fun testVisible() = hasStaffItem

            override fun testHitbox(mouseX: Double, mouseY: Double) =
                mouseX >= x + 2 && mouseY >= y + 2 && mouseX < x + width - 2 && mouseY < y + height - 3
        }.also(::addRenderableWidget)

        predicateButtons += listOf(
            // export hexpattern
            object : SpriteButton(
                x = exportButtonX,
                y = exportButtonY,
                uOffset = 432,
                vOffset = 392,
                width = exportButtonWidth,
                height = exportButtonHeight,
                message = buttonText("export"),
                onPress = {
                    exportToSystemClipboard()
                },
            ) {
                override val uOffsetHovered get() = uOffset
                override val vOffsetHovered get() = vOffset + 32

                override val uOffsetDisabled get() = uOffset
                override val vOffsetDisabled get() = vOffset + 64
            } to { // test
                true
            },

            // move view

            *listOf(
                SplicingTableAction.VIEW_LEFT to { !hasShiftDown() && !hasControlDown() },
                SplicingTableAction.VIEW_LEFT_PAGE to { hasShiftDown() && !hasControlDown() },
                SplicingTableAction.VIEW_LEFT_FULL to { hasControlDown() },
            ).map { (action, shouldBeVisible) ->
                object : SpriteButton(
                    x = leftPos + 4,
                    y = topPos + 25,
                    uOffset = 256,
                    vOffset = 0,
                    width = 10,
                    height = 10,
                    message = action.buttonText,
                    onPress = action.onPress,
                ) {
                    override fun testVisible() = shouldBeVisible()
                } to action.test
            }.toTypedArray(),

            *listOf(
                SplicingTableAction.VIEW_RIGHT to { !hasShiftDown() && !hasControlDown() },
                SplicingTableAction.VIEW_RIGHT_PAGE to { hasShiftDown() && !hasControlDown() },
                SplicingTableAction.VIEW_RIGHT_FULL to { hasControlDown() },
            ).map { (action, shouldBeVisible) ->
                object : SpriteButton(
                    x = leftPos + 178,
                    y = topPos + 25,
                    uOffset = 266,
                    vOffset = 0,
                    width = 10,
                    height = 10,
                    message = action.buttonText,
                    onPress = action.onPress,
                ) {
                    override fun testVisible() = shouldBeVisible()
                } to action.test
            }.toTypedArray(),

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

            actionSpriteButton(
                x = leftPos + 144,
                y = topPos + 64,
                uOffset = 284,
                vOffset = 16,
                width = 18,
                height = 11,
                action = SplicingTableAction.SELECT_NONE,
            ),

            actionSpriteButton(
                x = leftPos + 165,
                y = topPos + 64,
                uOffset = 305,
                vOffset = 16,
                width = 18,
                height = 11,
                action = SplicingTableAction.SELECT_ALL,
            ),

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

        for ((button, _) in predicateButtons) {
            addRenderableWidget(button)
        }

        val iotaButtons = (0 until IOTA_BUTTONS).map { offset ->
            addRenderableWidget(IotaButton(offset))
        }

        for (button in iotaButtons) {
            addRenderableOnly(IotaRangeSelection(button))
        }

        for (offset in 0 until IOTA_BUTTONS + 1) {
            addRenderableWidget(IotaEdgeSelection(offset))
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

        // cast hex (enlightened)
        addRenderableWidget(
            object : SpriteButton(
                x = castButtonX,
                y = castButtonY,
                uOffset = 460,
                vOffset = 392,
                width = castButtonWidth,
                height = castButtonHeight,
                message = buttonText("cast"),
                onPress = {
                    menu.table.castHex(null)
                    castingCooldown = maxCastingCooldown
                },
            ) {
                private val canCastIgnoringCooldown get() = data.hasHex && hasMediaForAction

                override val uOffsetHovered get() = uOffset
                override val vOffsetHovered get() = vOffset + 32

                override val uOffsetDisabled get() = uOffset
                override val vOffsetDisabled get() = vOffset + 64

                override fun testVisible() = data.isEnlightened

                override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                    active = canCastIgnoringCooldown && castingCooldown <= 0
                    super.render(guiGraphics, mouseX, mouseY, partialTick)
                }

                override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                    if (castingCooldown > 0) {
                        val (uOffset, vOffset) = when {
                            canCastIgnoringCooldown -> Pair(uOffset, vOffset)
                            else -> Pair(uOffsetDisabled, vOffsetDisabled)
                        }
                        blitSprite(guiGraphics, x, y, uOffset, vOffset, width, height)

                        val cooldownPercent = ((castingCooldown - minecraft!!.frameTime) / maxCastingCooldown.toFloat()).coerceIn(0f, 1f)
                        if (cooldownPercent > 0f) {
                            val minY = y + floor(height * (1f - cooldownPercent)).toInt()
                            val maxY = minY + ceil(height * cooldownPercent).toInt()
                            guiGraphics.fill(RenderType.guiOverlay(), x, minY, x + width, maxY, Int.MAX_VALUE)
                        }
                    } else {
                        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
                    }
                }
            }
        )

        reloadData()
    }

    private fun exportToSystemClipboard() {
        val export = data.list?.toHexpatternSource() ?: return
        minecraft?.keyboardHandler?.clipboard = export
    }

    // state sync

    // hack: since we're storing these in the container data, we can't send our own packet to trigger a reload, otherwise it desyncs
    // so just check every tick
    private fun pollForChanges() {
        if (selection != prevSelection || viewStartIndex != prevViewStartIndex) {
            prevSelection = selection
            prevViewStartIndex = viewStartIndex
            prevMedia = menu.media
            reloadData()
        } else if (prevMedia != menu.media) {
            prevMedia = menu.media
            updateActiveButtons()
        }
    }

    fun reloadData() {
        updateActiveButtons()
        for (child in children()) {
            if (child is SplicingTableButton) {
                child.reload()
            }
        }
    }

    private fun updateActiveButtons() {
        for ((button, predicate) in predicateButtons) {
            button.active = data.isListReadable && predicate()
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

    private val SplicingTableAction.onPress get(): () -> Unit = { menu.table.runAction(this, null) }

    private val SplicingTableAction.test get(): () -> Boolean =
        { value.test(data, selection, viewStartIndex) && (!value.consumesMedia || hasMediaForAction) }

    // GUI functionality

    private fun isEdgeInRange(index: Int) =
        // cell to the right is in range
        data.isInRange(index)
        // cell to the left is in range
        || data.isInRange(index - 1)
        // allow selecting leftmost edge of empty list
        || (index == 0 && data.list?.size == 0)

    private fun onSelectIota(index: Int) {
        menu.table.selectIndex(
            player = null,
            index = index,
            hasShiftDown = hasShiftDown(),
            isIota = true,
        )
    }

    private fun onSelectEdge(index: Int) {
        menu.table.selectIndex(
            player = null,
            index = index,
            hasShiftDown = hasShiftDown(),
            isIota = false,
        )
    }

    // TODO: limit scroll to certain regions? (let's see if anyone complains first)
    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        if (super.mouseScrolled(mouseX, mouseY, delta)) return true

        val adjustedDelta = if (HexDebugConfig.client.invertSplicingTableScrollDirection) {
            delta * -1
        } else {
            delta
        }

        val action = if (adjustedDelta > 0) {
            when {
                hasControlDown() -> SplicingTableAction.VIEW_LEFT_FULL
                hasShiftDown() -> SplicingTableAction.VIEW_LEFT_PAGE
                else -> SplicingTableAction.VIEW_LEFT
            }
        } else {
            when {
                hasControlDown() -> SplicingTableAction.VIEW_RIGHT_FULL
                hasShiftDown() -> SplicingTableAction.VIEW_RIGHT_PAGE
                else -> SplicingTableAction.VIEW_RIGHT
            }
        }

        if (!action.test()) return false

        // hack: limit scrolls per tick to 4 (arbitrary) to prevent DoS
        // (this could be easily solved by adding a new packet, but lazy)
        repeat(min(delta.absoluteValue.roundToInt(), 4)) {
            menu.table.runAction(action, null)
        }

        return true
    }

    // staff delegation stuff

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (hasStaffItem && isInStaffGrid(mouseX, mouseY, button)) {
            guiSpellcasting.mouseClicked(mouseX, mouseY, button)
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        if (hasStaffItem && isInStaffGrid(mouseX, mouseY, button = null)) {
            guiSpellcasting.mouseMoved(mouseX, mouseY)
        }
        super.mouseMoved(mouseX, mouseY)
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

    private fun isInStaffGrid(mouseX: Double, mouseY: Double, button: Int?) =
        staffMinX <= mouseX && mouseX <= staffMaxX && staffMinY <= mouseY && mouseY <= staffMaxY
        // avoid interacting with the grid when inserting the staff item...
        && (button == null || isOutsideStaffItemSlot(mouseX, mouseY, leftPos, topPos))
        // or when clicking the clear grid button
        && !(clearGridButton?.testHitbox(mouseX, mouseY) ?: false)

    private fun isOutsideStaffItemSlot(mouseX: Double, mouseY: Double, guiLeft: Int, guiTop: Int) =
        mouseX < guiLeft + staffSlotMinXOffset
        || mouseY < guiTop + staffSlotMinYOffset
        || mouseX >= guiLeft + staffSlotMaxXOffset
        || mouseY >= guiTop + staffSlotMaxYOffset

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
            && (
                mouseX < guiLeft + storageMinXOffset
                || mouseY < guiTop + storageMinYOffset
                || mouseX >= guiLeft + storageMaxXOffset
                || mouseY >= guiTop + storageMaxYOffset
            )
            // staff item slot
            && isOutsideStaffItemSlot(mouseX, mouseY, guiLeft, guiTop)
        )
    }

    // rendering

    override fun containerTick() {
        super.containerTick()
        pollForChanges()
        if (castingCooldown > 0) {
            castingCooldown -= 1
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
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
        if (hasMediaForAction) {
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
            blitSprite(guiGraphics, x = staffSlotMinX, y = staffSlotMinY, uOffset = 232, vOffset = 293, width = staffSlotWidth, height = staffSlotHeight)
        } else {
            // staff slot with icon
            blitSprite(guiGraphics, x = staffSlotMinX, y = staffSlotMinY, uOffset = 232, vOffset = 328, width = staffSlotWidth, height = staffSlotHeight)
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

        const val MAX_DIGIT_LEN = 4
        val MAX_DIGIT = 10f.pow(MAX_DIGIT_LEN).toInt() - 1

        private val maxCastingCooldown get() = HexDebugConfig.server.splicingTableCastingCooldown

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

        override fun testHitbox(mouseX: Double, mouseY: Double): Boolean {
            // skip hitbox if hovering over an edge selection
            // FIXME: hack
            return super.testHitbox(mouseX, mouseY) && mouseX >= x + 2 && mouseX < x + width - 2
        }

        init {
            reload()
        }
    }

    // TODO: hover texture?
    inner class IotaRangeSelection(button: IotaButton) : Renderable {
        private val offset by button::offset
        private val index by button::index

        override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val selection = selection
            if (data.isInRange(index) && selection is Selection.Range && index in selection) {
                RenderSystem.enableBlend()
                blitSprite(
                    guiGraphics,
                    x = leftPos + 15 + 18 * offset,
                    y = topPos + 18,
                    uOffset = 352,
                    vOffset = 24,
                    width = 18,
                    height = 25,
                )
                if (index == selection.start) {
                    drawSelectionEndCap(guiGraphics, offset, SelectionEndCap.LEFT)
                }
                if (index == selection.end) {
                    drawSelectionEndCap(guiGraphics, offset, SelectionEndCap.RIGHT)
                }
                RenderSystem.disableBlend()
            }
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

    inner class IotaEdgeSelection(private val offset: Int) : SplicingTableButton(
        x = leftPos + 13 + 18 * offset,
        y = topPos + 23,
        width = 4,
        height = 15,
        message = null,
    ) {
        override val uOffset = 375
        override val vOffset = 29

        override val uOffsetHovered get() = uOffset
        override val vOffsetHovered get() = vOffset + 405

        override val uOffsetDisabled get() = uOffset
        override val vOffsetDisabled get() = vOffset

        private val index get() = viewStartIndex + offset

        override fun testVisible() = isEdgeInRange(index)

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            if (isHovered || index == (selection as? Selection.Edge)?.index) {
                RenderSystem.enableBlend()
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
                RenderSystem.disableBlend()
            }
        }

        override fun onPress() {
            onSelectEdge(index)
        }
    }
}

fun GuiGraphics.setColor(color: Color) = setColor(color.fred, color.fgreen, color.fblue, color.falpha)
