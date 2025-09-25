package gay.`object`.hexdebug.gui.splicing.renderers

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.client.render.drawLineSeq
import at.petrak.hexcasting.client.render.findDupIndices
import at.petrak.hexcasting.client.render.getCenteredPattern
import at.petrak.hexcasting.client.render.makeZappy
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import gay.`object`.hexdebug.api.client.splicing.*
import gay.`object`.hexdebug.api.splicing.SplicingTableIotaClientView
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import gay.`object`.hexdebug.utils.deserializePattern
import gay.`object`.hexdebug.utils.simpleString
import net.minecraft.client.renderer.GameRenderer

object PatternRendererProvider : SplicingTableIotaRendererProvider {
    override fun createRenderer(type: IotaType<*>, iota: SplicingTableIotaClientView): SplicingTableIotaRenderer {
        val pattern = iota.deserializePattern()!!

        val patternWidth = 16f
        val patternHeight = 13f

        val (_, dots) = getCenteredPattern(
            pattern = pattern,
            width = patternWidth,
            height = patternHeight,
            minSize = 8f,
        )

        val zappyPoints = makeZappy(
            barePoints = dots,
            dupIndices = findDupIndices(pattern.positions()),
            hops = 1,
            variance = 0f,
            speed = 0f,
            flowIrregular = 0f,
            readabilityOffset = 0f,
            lastSegmentLenProportion = 1f,
            seed = 0.0,
        )

        return SplicingTableIotaRenderer { guiGraphics, x, y ->
            val ps = guiGraphics.pose()

            ps.pushPose()

            RenderSystem.enableBlend()
            RenderSystem.setShader(GameRenderer::getPositionColorShader)
            RenderSystem.disableCull()
            RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            )

            ps.translate(x + 1f, y + 4f, 100f)
            ps.translate(patternWidth / 2f, patternHeight / 2f, 0f)

            val mat = ps.last().pose()

            val outer = 0xff_d2c8c8.toInt()
            val innerLight = 0xc8_aba2a2.toInt()
            val innerDark = 0xc8_322b33.toInt()

            drawLineSeq(mat, zappyPoints, width = 2f, z = 0f, tail = outer, head = outer)
            drawLineSeq(mat, zappyPoints, width = 2f * 0.4f, z = 0f, tail = innerDark, head = innerLight)

            ps.popPose()
        }
    }

    override fun createTooltip(
        type: IotaType<*>,
        iota: SplicingTableIotaClientView,
        index: Int,
    ): SplicingTableIotaTooltip {
        val tooltip = super.createTooltip(type, iota, index)
        val pattern = iota.deserializePattern()!!
        tooltip.advanced += SplicingTableScreen.tooltipText("signature", pattern.simpleString())
        return tooltip
    }

    override fun getBackgroundType(
        type: IotaType<*>,
        iota: SplicingTableIotaClientView,
    ) = SplicingTableIotaBackgroundType.SLATE

    val PARSER = SplicingTableIotaRendererParser.of(this)
}
