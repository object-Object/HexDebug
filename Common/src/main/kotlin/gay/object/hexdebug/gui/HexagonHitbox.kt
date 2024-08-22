package gay.`object`.hexdebug.gui

import gay.`object`.hexdebug.utils.pointInRect
import gay.`object`.hexdebug.utils.pointInTriangle
import gay.`object`.hexdebug.utils.vec2
import net.minecraft.world.phys.Vec2

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
class HexagonHitbox(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    triangleHeight: Int,
    isHorizontal: Boolean,
) {
    private val triangleOffset = if (isHorizontal) {
        vec2(triangleHeight, 0)
    } else {
        vec2(0, triangleHeight)
    }

    private val rectX = x + triangleOffset.x
    private val rectY = y + triangleOffset.y

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
            val triangleY = y + height.toFloat() / 2f
            triangle1 = Triple(
                rectTopLeft,
                rectBottomLeft,
                vec2(x, triangleY),
            )
            triangle2 = Triple(
                rectTopRight,
                rectBottomRight,
                vec2(x + width, triangleY),
            )
        } else {
            val triangleX = x + height.toFloat() / 2f
            triangle1 = Triple(
                rectTopLeft,
                rectTopRight,
                vec2(triangleX, y),
            )
            triangle2 = Triple(
                rectBottomLeft,
                rectBottomRight,
                vec2(triangleX, y + height),
            )
        }
    }

    fun test(mouseX: Double, mouseY: Double): Boolean {
        val mousePos = vec2(mouseX, mouseY)
        return pointInRect(mousePos, rectTopLeft, rectBottomRight) || pointInTriangle(mousePos, triangle1) || pointInTriangle(mousePos, triangle2)
    }
}
