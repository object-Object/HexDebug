package gay.`object`.hexdebug.gui.splicing.widgets

import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.utils.asTextComponent
import at.petrak.hexcasting.api.utils.asTranslatedComponent
import at.petrak.hexcasting.api.utils.mediaBarColor
import at.petrak.hexcasting.api.utils.styledWith
import gay.`object`.hexdebug.config.HexDebugConfig
import gay.`object`.hexdebug.gui.splicing.SplicingTableMenu
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.ceil

// FIXME: no tooltip hover over rightmost pixel ???
class MediaBar(
    menu: SplicingTableMenu,
    x: Int,
    y: Int,
    private val uOffset: Int,
    private val vOffset: Int,
    width: Int,
    height: Int,
) : AbstractWidget(x, y, width, height, Component.empty()) {
    private val media by menu::media
    private val maxMedia get() = HexDebugConfig.server.splicingTableMaxMedia
    private val fullness get() = if (maxMedia > 0) media.toDouble() / maxMedia else 0.0

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (media > 0 && maxMedia > 0) {
            val visibleHeight = ceil(height * fullness).toInt().coerceIn(0, height)
            val yOffset = height - visibleHeight
            SplicingTableScreen.blitSprite(
                guiGraphics,
                x = x,
                y = y + yOffset,
                uOffset = uOffset,
                vOffset = vOffset + yOffset,
                width = width,
                height = visibleHeight,
            )
        }
    }

    override fun updateTooltip() {
        // stolen from ItemMediaHolder
        // FIXME: i don't think this is the right way to do this :/
        tooltip = if (maxMedia > 0) {
            val color = TextColor.fromRgb(mediaBarColor(media, maxMedia))

            val mediamount = DUST_AMOUNT.format(media.toDouble() / MediaConstants.DUST_UNIT)
                .asTextComponent
                .styledWith { it.withColor(HEX_COLOR) }

            val percentFull = (PERCENTAGE.format(100f * fullness) + "%")
                .asTextComponent
                .styledWith { it.withColor(color) }

            val maxCapacity = "hexcasting.tooltip.media"
                .asTranslatedComponent(DUST_AMOUNT.format(maxMedia.toDouble() / MediaConstants.DUST_UNIT))
                .styledWith { it.withColor(HEX_COLOR) }

            Tooltip.create(
                "hexcasting.tooltip.media_amount.advanced"
                    .asTranslatedComponent(mediamount, maxCapacity, percentFull)
            )
        } else null
        super.updateTooltip()
    }

    // TODO: implement?
    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {}
}

// stolen from ItemMediaHolder
private val HEX_COLOR: TextColor = TextColor.fromRgb(0xb38ef3)
private val PERCENTAGE = DecimalFormat("####").apply { roundingMode = RoundingMode.DOWN }
private val DUST_AMOUNT = DecimalFormat("###,###.##")
