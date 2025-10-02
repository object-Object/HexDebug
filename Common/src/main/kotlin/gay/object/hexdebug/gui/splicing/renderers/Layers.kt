package gay.`object`.hexdebug.gui.splicing.renderers

import at.petrak.hexcasting.api.casting.iota.IotaType
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRenderer
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererParser
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererProvider
import gay.`object`.hexdebug.api.splicing.SplicingTableIotaClientView
import gay.`object`.hexdebug.utils.getAsIotaRendererProvider
import gay.`object`.hexdebug.utils.pushPose
import gay.`object`.hexdebug.utils.scale
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.util.GsonHelper

class LayersRenderer(
    type: IotaType<*>,
    iota: SplicingTableIotaClientView,
    x: Int,
    y: Int,
    private val layers: List<RendererLayer<SplicingTableIotaRenderer>>,
) : SplicingTableIotaRenderer(type, iota, x, y) {
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val ps = guiGraphics.pose()
        for (layer in layers) {
            ps.pushPose {
                ps.translate(x.toFloat(), y.toFloat(), 0f)
                if (layer.scale != 1f) {
                    ps.translate(18f / 2f, 21f / 2f, 0f)
                    ps.scale(layer.scale)
                    ps.translate(-18f / 2f, -21f / 2f, 0f)
                }
                layer.renderer.render(guiGraphics, mouseX, mouseY, partialTick)
            }
        }
    }

    override fun createTooltip(): Tooltip {
        return layers.lastOrNull { it.useTooltip }
            ?.renderer
            ?.createTooltip()
            ?: super.createTooltip()
    }
}

class LayersRendererProvider(
    private val layers: List<RendererLayer<SplicingTableIotaRendererProvider>>,
) : SplicingTableIotaRendererProvider {
    override fun createRenderer(
        type: IotaType<*>,
        iota: SplicingTableIotaClientView,
        x: Int,
        y: Int
    ): SplicingTableIotaRenderer? {
        val rendererLayers = layers.mapNotNull { layer ->
            layer.mapNotNull { it.createRenderer(type, iota, 0, 0) }
        }
        if (rendererLayers.isEmpty()) return null
        return LayersRenderer(type, iota, x, y, rendererLayers)
    }

    companion object {
        val PARSER = SplicingTableIotaRendererParser<LayersRendererProvider> { _, jsonObject, _ ->
            LayersRendererProvider(
                layers = GsonHelper.getAsJsonArray(jsonObject, "layers").map { layer ->
                    val layerObject = GsonHelper.convertToJsonObject(layer, "layers")
                    RendererLayer(
                        renderer = layerObject.getAsIotaRendererProvider("renderer"),
                        scale = GsonHelper.getAsFloat(layerObject, "scale", 1f),
                        useTooltip = GsonHelper.getAsBoolean(layerObject, "useTooltip", true),
                    )
                },
            )
        }
    }
}

data class RendererLayer<T>(
    val renderer: T,
    val scale: Float,
    val useTooltip: Boolean,
) {
    fun <U> mapNotNull(f: (T) -> U?): RendererLayer<U>? {
        return RendererLayer(
            renderer = f(renderer) ?: return null,
            scale = scale,
            useTooltip = useTooltip,
        )
    }
}
