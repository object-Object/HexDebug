package gay.`object`.hexdebug.gui.splicing.widgets

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import com.mojang.blaze3d.systems.RenderSystem
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaBackgroundType
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRenderer
import gay.`object`.hexdebug.api.splicing.SplicingTableIotaClientView
import gay.`object`.hexdebug.resources.splicing.SplicingTableIotasResourceReloadListener
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

abstract class BaseIotaButton(x: Int, y: Int) : HexagonButton(
    x = x,
    y = y,
    width = 18,
    height = 21,
    triangleHeight = 5,
    isHorizontal = false,
    message = Component.empty(),
) {
    abstract val iotaView: SplicingTableIotaClientView?

    private var renderer: SplicingTableIotaRenderer? = null
    private var backgroundType: SplicingTableIotaBackgroundType? = null

    override val uOffset get() = 352 + 20 * (backgroundType?.ordinal ?: 0)
    override val vOffset = 0

    override val uOffsetDisabled get() = uOffset
    override val vOffsetDisabled get() = vOffset

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (iotaView != null && backgroundType != null) {
            RenderSystem.enableBlend()
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
            RenderSystem.disableBlend()

            renderer?.render(guiGraphics, mouseX, mouseY, partialTick)
        }
    }

    override fun reload() {
        super.reload()

        active = false
        renderer = null
        backgroundType = null
        tooltip = null

        // don't enable the button or display anything if this index is out of range
        val iotaView = iotaView ?: return

        val iotaType = IotaType.getTypeFromTag(iotaView.tag) ?: HexIotaTypes.GARBAGE

        active = true

        val provider = iotaType
            ?.let { SplicingTableIotasResourceReloadListener.PROVIDERS[it] }
            ?: SplicingTableIotasResourceReloadListener.FALLBACK
            ?: return

        renderer = provider.createRenderer(iotaType, iotaView, x, y)
        backgroundType = provider.getBackgroundType(iotaType, iotaView)
        tooltip = provider.createTooltip(iotaType, iotaView).build()
    }
}
