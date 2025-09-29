package gay.`object`.hexdebug.gui.splicing.renderers

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.utils.downcast
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRenderer
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererParser
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaTooltipBuilder
import gay.`object`.hexdebug.api.splicing.SplicingTableIotaClientView
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import net.minecraft.nbt.ListTag

object ListRendererProvider : TextureRendererProvider(
    texture = HexDebug.id("textures/gui/splicing_table.png"),
    xOffset = 2,
    yOffset = 3,
    uOffset = 369,
    vOffset = 73,
    width = 14,
    height = 14,
    textureWidth = 512,
    textureHeight = 512,
) {
    override fun createRenderer(
        type: IotaType<*>,
        iota: SplicingTableIotaClientView,
        x: Int,
        y: Int
    ): SplicingTableIotaRenderer {
        return ListRenderer(type, iota, x, y)
    }

    class ListRenderer(
        type: IotaType<*>,
        iota: SplicingTableIotaClientView,
        x: Int,
        y: Int,
    ) : TextureRenderer(type, iota, x, y) {
        override fun buildTooltip(): SplicingTableIotaTooltipBuilder {
            val listTag = iota.data!!.downcast(ListTag.TYPE)
            return super.buildTooltip()
                .addAdvancedLine(SplicingTableScreen.tooltipText("length", listTag.size))
        }
    }

    val PARSER = SplicingTableIotaRendererParser.simple(::ListRenderer)
}
