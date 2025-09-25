package gay.`object`.hexdebug.gui.splicing

import java.awt.Color

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
