package gay.`object`.hexdebug.gui.splicing.widgets

import gay.`object`.hexdebug.gui.HexagonHitbox
import net.minecraft.network.chat.Component

abstract class HexagonButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    triangleHeight: Int,
    isHorizontal: Boolean,
    message: Component,
) : SplicingTableButton(x, y, width, height, message) {
    private val hitbox = HexagonHitbox(
        x = x,
        y = y,
        width = width,
        height = height,
        triangleHeight = triangleHeight,
        isHorizontal = isHorizontal,
    )

    override fun testHitbox(mouseX: Double, mouseY: Double) =
        super.testHitbox(mouseX, mouseY) && hitbox.test(mouseX, mouseY)
}
