package gay.`object`.hexdebug.gui.splicing.renderers

import at.petrak.hexcasting.api.casting.eval.SpecialPatterns
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.client.render.drawLineSeq
import at.petrak.hexcasting.client.render.findDupIndices
import at.petrak.hexcasting.client.render.getCenteredPattern
import at.petrak.hexcasting.client.render.makeZappy
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import gay.`object`.hexdebug.api.client.splicing.*
import gay.`object`.hexdebug.api.splicing.SplicingTableIotaClientView
import gay.`object`.hexdebug.config.HexDebugClientConfig
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import gay.`object`.hexdebug.utils.deserializePattern
import gay.`object`.hexdebug.utils.getWrapping
import gay.`object`.hexdebug.utils.simpleString
import net.minecraft.client.renderer.GameRenderer

object PatternRendererProvider : SplicingTableIotaRendererProvider {
    override fun createRenderer(
        type: IotaType<*>,
        iota: SplicingTableIotaClientView,
        x: Int,
        y: Int,
    ): SplicingTableIotaRenderer {
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

        val outer = if (
            HexDebugClientConfig.config.enableSplicingTableRainbowBrackets
            && (pattern.sigsEqual(SpecialPatterns.INTROSPECTION) || pattern.sigsEqual(SpecialPatterns.RETROSPECTION))
        ) {
            HexDebugClientConfig.config.rainbowBracketColors.getWrapping(iota.depth)
        } else {
            0xd2c8c8
        } or 0xff_000000.toInt()

        return SplicingTableIotaRenderer { guiGraphics, _, _, _ ->
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

            // ARGB
            val innerLight = 0xc8_aba2a2.toInt()
            val innerDark = 0xc8_322b33.toInt()

            drawLineSeq(mat, zappyPoints, width = 2f, z = 0f, tail = outer, head = outer)
            drawLineSeq(mat, zappyPoints, width = 2f * 0.4f, z = 0f, tail = innerDark, head = innerLight)

            ps.popPose()
        }
    }

    override fun getTooltipBuilder(
        type: IotaType<*>,
        iota: SplicingTableIotaClientView,
    ): SplicingTableIotaTooltipBuilder {
        val pattern = iota.deserializePattern()!!
        return super.getTooltipBuilder(type, iota)
            .addAdvancedLine(SplicingTableScreen.tooltipText("signature", pattern.simpleString()))
    }

    override fun getBackgroundType(
        type: IotaType<*>,
        iota: SplicingTableIotaClientView,
    ) = SplicingTableIotaBackgroundType.SLATE

    val PARSER = SplicingTableIotaRendererParser.of(this)
}
