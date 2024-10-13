package gay.`object`.hexdebug.gui.splicing.widgets

import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

abstract class SplicingTableButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Component,
) : AbstractButton(x, y, width, height, message) {
    init {
        tooltip = Tooltip.create(message)
    }

    abstract val uOffset: Int
    abstract val vOffset: Int

    open val uOffsetHovered get() = uOffset + 0
    open val vOffsetHovered get() = vOffset + 405

    open val uOffsetDisabled get() = uOffset - 136
    open val vOffsetDisabled get() = vOffset + 405

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
        val (uOffset, vOffset) = when {
            !isActive -> Pair(uOffsetDisabled, vOffsetDisabled)
            isHovered -> Pair(uOffsetHovered, vOffsetHovered)
            else -> Pair(uOffset, vOffset)
        }
        SplicingTableScreen.blitSprite(guiGraphics, x, y, uOffset, vOffset, width, height)
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) = defaultButtonNarrationText(output)

    open fun reload() {}
}
