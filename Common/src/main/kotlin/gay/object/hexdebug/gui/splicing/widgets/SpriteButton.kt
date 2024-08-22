package gay.`object`.hexdebug.gui.splicing.widgets

import net.minecraft.network.chat.Component

open class SpriteButton(
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
