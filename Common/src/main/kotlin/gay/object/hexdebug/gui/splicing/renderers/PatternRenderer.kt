package gay.`object`.hexdebug.gui.splicing.renderers

import at.petrak.hexcasting.api.casting.eval.SpecialPatterns
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.client.render.drawLineSeq
import at.petrak.hexcasting.client.render.findDupIndices
import at.petrak.hexcasting.client.render.getCenteredPattern
import at.petrak.hexcasting.client.render.makeZappy
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaBackgroundType
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRenderer
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererParser
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaTooltipBuilder
import gay.`object`.hexdebug.api.splicing.SplicingTableIotaClientView
import gay.`object`.hexdebug.config.HexDebugClientConfig
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import gay.`object`.hexdebug.utils.getWrapping
import gay.`object`.hexdebug.utils.letPushPose
import gay.`object`.hexdebug.utils.simpleString
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer

class PatternRenderer(
    type: IotaType<*>,
    iota: SplicingTableIotaClientView,
    x: Int,
    y: Int,
) : SplicingTableIotaRenderer(type, iota, x, y) {
    private val pattern = PatternIota.deserialize(iota.data).pattern

    private val patternWidth = 16f
    private val patternHeight = 13f

    private val dots = getCenteredPattern(
        pattern = pattern,
        width = patternWidth,
        height = patternHeight,
        minSize = 8f,
    ).second

    private val zappyPoints = makeZappy(
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

    private val outer = if (
        HexDebugClientConfig.config.enableSplicingTableRainbowBrackets
        && (pattern.sigsEqual(SpecialPatterns.INTROSPECTION) || pattern.sigsEqual(SpecialPatterns.RETROSPECTION))
    ) {
        HexDebugClientConfig.config.rainbowBracketColors.getWrapping(iota.depth)
    } else {
        0xd2c8c8
    } or 0xff_000000.toInt()

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.pose().letPushPose { ps ->
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
        }
    }

    override fun buildTooltip(): SplicingTableIotaTooltipBuilder {
        return super.buildTooltip()
            .addAdvancedLine(SplicingTableScreen.tooltipText("signature", pattern.simpleString()))
    }

    override fun getBackgroundType() = SplicingTableIotaBackgroundType.SLATE

    companion object {
        val PARSER = SplicingTableIotaRendererParser.simple(::PatternRenderer)
    }
}
