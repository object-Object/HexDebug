package gay.`object`.hexdebug.gui.splicing.widgets

import at.petrak.hexcasting.api.spell.iota.IotaType
import at.petrak.hexcasting.api.utils.asTextComponent
import at.petrak.hexcasting.api.utils.darkGray
import at.petrak.hexcasting.api.utils.downcast
import at.petrak.hexcasting.api.utils.gray
import at.petrak.hexcasting.client.drawLineSeq
import at.petrak.hexcasting.client.findDupIndices
import at.petrak.hexcasting.client.getCenteredPattern
import at.petrak.hexcasting.client.makeZappy
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import gay.`object`.hexdebug.gui.splicing.IotaBackgroundType
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen.Companion.blitSprite
import gay.`object`.hexdebug.gui.splicing.setColor
import gay.`object`.hexdebug.splicing.IotaClientView
import gay.`object`.hexdebug.utils.joinToComponent
import gay.`object`.hexdebug.utils.simpleString
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.phys.Vec2
import java.awt.Color

abstract class BaseIotaButton(
    x: Int,
    y: Int,
) : HexagonButton(
    x = x,
    y = y,
    width = 18,
    height = 21,
    triangleHeight = 5,
    isHorizontal = false,
    message = Component.empty(),
) {
    abstract val index: Int
    abstract val iotaView: IotaClientView?

    private val patternWidth = 16f
    private val patternHeight = 13f

    var backgroundType: IotaBackgroundType? = null
        private set

    private var zappyPoints: List<Vec2>? = null
    private var typeUVOffset: Pair<Int, Int>? = null
    private var typeColor: Color? = null

    override val uOffset get() = 352 + 20 * (backgroundType?.ordinal ?: 0)
    override val vOffset = 0

    override val uOffsetDisabled get() = uOffset
    override val vOffsetDisabled get() = vOffset

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (iotaView != null && backgroundType != null) {
            RenderSystem.enableBlend()
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
            RenderSystem.disableBlend()

            val zappyPoints = zappyPoints
            val typeUVOffset = typeUVOffset

            if (zappyPoints != null) {
                renderPattern(guiGraphics, zappyPoints)
            } else if (typeUVOffset != null) {
                val (uOffset, vOffset) = typeUVOffset
                guiGraphics.setColor(typeColor ?: Color.WHITE)
                blitSprite(guiGraphics, x = x + 2, y = y + 3, uOffset = uOffset, vOffset = vOffset, width = 14, height = 14)
                guiGraphics.setColor(1f, 1f, 1f, 1f)
            }
        }
    }

    private fun renderPattern(guiGraphics: GuiGraphics, zappyPoints: List<Vec2>) {
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

    override fun reload() {
        super.reload()

        active = false
        backgroundType = null
        tooltip = null
        zappyPoints = null
        typeUVOffset = null
        typeColor = null

        // don't enable the button or display anything if this index is out of range
        val iotaView = iotaView ?: return

        val iotaType = IotaType.getTypeFromTag(iotaView.tag)
        val iotaData = iotaView.tag.get(HexIotaTypes.KEY_DATA)

        active = true

        backgroundType = when (iotaType) {
            HexIotaTypes.PATTERN -> IotaBackgroundType.PATTERN
            else -> IotaBackgroundType.GENERIC
        }

        val details = mutableListOf(
            SplicingTableScreen.tooltipText("index", index),
        )
        val advanced = mutableListOf<MutableComponent>()

        // if it's an invalid iota, just set the background type and tooltip, then return
        // (so that we don't need null checks later)
        if (iotaType == null || iotaData == null) {
            tooltip = createTooltip(getBrokenIotaName(), details, advanced)
            typeColor = Color.LIGHT_GRAY
            return
        }

        val iotaTypeId = HexIotaTypes.REGISTRY.getKey(iotaType)
        if (iotaTypeId != null) {
            advanced += iotaTypeId.toString().asTextComponent
        }

        typeColor = Color(iotaType.color(), true)

        // type-specific rendering
        typeUVOffset = when (iotaType) {
            HexIotaTypes.DOUBLE -> getTypeUVOffset(0, 0)
            HexIotaTypes.VEC3 -> getTypeUVOffset(1, 0)
            HexIotaTypes.ENTITY -> getTypeUVOffset(2, 0)
            HexIotaTypes.BOOLEAN -> getTypeUVOffset(1, 1)
            HexIotaTypes.LIST -> {
                val listTag = iotaData.downcast(ListTag.TYPE)
                advanced += SplicingTableScreen.tooltipText("length", listTag.size)
                getTypeUVOffset(2, 1)
            }
            HexIotaTypes.NULL -> getTypeUVOffset(0, 2)
            HexIotaTypes.GARBAGE -> getTypeUVOffset(1, 2)
            HexIotaTypes.CONTINUATION -> getTypeUVOffset(2, 2)

            // custom pattern rendering
            HexIotaTypes.PATTERN -> iotaView.pattern?.let { pattern ->
                advanced += SplicingTableScreen.tooltipText("signature", pattern.simpleString())

                val (_, dots) = getCenteredPattern(
                    pattern = pattern,
                    width = patternWidth,
                    height = patternHeight,
                    minSize = 8f,
                )
                zappyPoints = makeZappy(
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

                null
            }

            else -> when (iotaTypeId?.toString()) {
                // addon patterns (so we don't need to actually depend on the addons)
                "moreiotas:string" -> getTypeUVOffset(3, 0)
                "moreiotas:matrix" -> getTypeUVOffset(3, 1)
                "hexal:iota_type",
                "hexal:entity_type",
                "hexal:item_type",
                "moreiotas:iota_type",
                "moreiotas:entity_type",
                "moreiotas:item_type" -> getTypeUVOffset(3, 2)
                // generic type icon
                else -> getTypeUVOffset(0, 1)
            }
        }

        tooltip = createTooltip(iotaView.name, details, advanced)
    }

    private fun getBrokenIotaName() = IotaType.getDisplay(CompoundTag()) // "a broken iota"

    private fun createTooltip(
        name: Component,
        details: Collection<MutableComponent>,
        advanced: Collection<MutableComponent>,
    ): Tooltip {
        var lines = sequenceOf(name)
        lines += details.asSequence().map { it.gray }
        if (Minecraft.getInstance().options.advancedItemTooltips) {
            lines += advanced.asSequence().map { it.darkGray }
        }
        return Tooltip.create(lines.toList().joinToComponent("\n"))
    }

    private fun getTypeUVOffset(xIndex: Int, yIndex: Int) = Pair(337 + 16 * xIndex, 57 + 16 * yIndex)
}
