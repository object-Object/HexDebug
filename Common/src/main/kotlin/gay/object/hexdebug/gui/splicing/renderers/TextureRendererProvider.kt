package gay.`object`.hexdebug.gui.splicing.renderers

import at.petrak.hexcasting.api.casting.iota.IotaType
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRenderer
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererParser
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererProvider
import gay.`object`.hexdebug.api.splicing.SplicingTableIotaClientView
import gay.`object`.hexdebug.gui.splicing.setColor
import gay.`object`.hexdebug.utils.getAsResourceLocation
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper
import java.awt.Color

open class TextureRendererProvider(
    val texture: ResourceLocation,
    val xOffset: Int,
    val yOffset: Int,
    val uOffset: Int,
    val vOffset: Int,
    val width: Int,
    val height: Int,
    val textureWidth: Int,
    val textureHeight: Int,
    val useIotaColor: Boolean = true,
) : SplicingTableIotaRendererProvider {
    override fun createRenderer(type: IotaType<*>, iota: SplicingTableIotaClientView): SplicingTableIotaRenderer {
        return SplicingTableIotaRenderer { guiGraphics, x, y ->
            if (useIotaColor) guiGraphics.setColor(Color(type.color(), true))
            guiGraphics.blit(texture, x + xOffset, y + yOffset, uOffset.toFloat(), vOffset.toFloat(), width, height, textureWidth, textureHeight)
            if (useIotaColor) guiGraphics.setColor(1f, 1f, 1f, 1f)
        }
    }

    companion object {
        val PARSER = SplicingTableIotaRendererParser<TextureRendererProvider> { _, json, parent ->
            TextureRendererProvider(
                texture = json.getAsResourceLocation("texture", parent?.texture),
                xOffset = GsonHelper.getAsInt(json, "xOffset", parent?.xOffset ?: 0),
                yOffset = GsonHelper.getAsInt(json, "yOffset", parent?.yOffset ?: 0),
                uOffset = GsonHelper.getAsInt(json, "uOffset", parent?.uOffset ?: 0),
                vOffset = GsonHelper.getAsInt(json, "vOffset", parent?.vOffset ?: 0),
                width = GsonHelper.getAsInt(json, "width", parent?.width ?: 18),
                height = GsonHelper.getAsInt(json, "height", parent?.height ?: 21),
                textureWidth = GsonHelper.getAsInt(json, "textureWidth", parent?.textureWidth ?: 18),
                textureHeight = GsonHelper.getAsInt(json, "textureHeight", parent?.textureHeight ?: 21),
                useIotaColor = GsonHelper.getAsBoolean(json, "useIotaColor", true),
            )
        }
    }
}
