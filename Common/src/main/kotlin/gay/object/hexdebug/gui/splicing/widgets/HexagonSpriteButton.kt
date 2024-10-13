package gay.`object`.hexdebug.gui.splicing.widgets

import net.minecraft.network.chat.Component

class HexagonSpriteButton(
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
