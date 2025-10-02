package gay.`object`.hexdebug.gui.splicing.renderers

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.utils.downcast
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRenderer
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererParser
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererProvider
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaTooltipBuilder
import gay.`object`.hexdebug.api.splicing.SplicingTableIotaClientView
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import gay.`object`.hexdebug.utils.getAsIotaRendererProvider
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.nbt.ListTag

class ListRenderer(
    type: IotaType<*>,
    iota: SplicingTableIotaClientView,
    x: Int,
    y: Int,
    private val inner: SplicingTableIotaRenderer?,
) : SplicingTableIotaRenderer(type, iota, x, y) {
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        inner?.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun buildTooltip(): SplicingTableIotaTooltipBuilder {
        val listTag = iota.data!!.downcast(ListTag.TYPE)
        return super.buildTooltip()
            .addAdvancedLine(SplicingTableScreen.tooltipText("length", listTag.size))
    }
}

class ListRendererProvider(private val inner: SplicingTableIotaRendererProvider) : SplicingTableIotaRendererProvider {
    override fun createRenderer(
        type: IotaType<*>,
        iota: SplicingTableIotaClientView,
        x: Int,
        y: Int
    ): SplicingTableIotaRenderer {
        return ListRenderer(type, iota, x, y, inner.createRenderer(type, iota, x, y))
    }

    companion object {
        val PARSER = SplicingTableIotaRendererParser<ListRendererProvider> { _, jsonObject, _ ->
            ListRendererProvider(
                inner = jsonObject.getAsIotaRendererProvider("renderer"),
            )
        }
    }
}
