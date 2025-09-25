package gay.`object`.hexdebug

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.client.ScryingLensOverlayRegistry
import at.petrak.hexcasting.api.utils.asTextComponent
import at.petrak.hexcasting.api.utils.gray
import at.petrak.hexcasting.api.utils.plusAssign
import gay.`object`.hexdebug.HexDebug.LOGGER
import gay.`object`.hexdebug.adapter.proxy.DebugProxyClient
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRenderers
import gay.`object`.hexdebug.blocks.focusholder.FocusHolderBlock
import gay.`object`.hexdebug.config.HexDebugClientConfig
import gay.`object`.hexdebug.config.HexDebugServerConfig
import gay.`object`.hexdebug.gui.splicing.renderers.ListRendererProvider
import gay.`object`.hexdebug.gui.splicing.renderers.PatternRendererProvider
import gay.`object`.hexdebug.gui.splicing.renderers.TextureRendererProvider
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.resources.splicing.SplicingTableIotasResourceReloadListener
import gay.`object`.hexdebug.utils.styledHoverName
import gay.`object`.hexdebug.utils.toComponent
import me.shedaniel.autoconfig.AutoConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import com.mojang.datafixers.util.Pair as MojangPair

typealias ScryingLensLine = MojangPair<ItemStack, Component>

object HexDebugClient {
    private const val MAX_IOTA_DISPLAY_LINES = 4

    fun init() {
        LOGGER.info("Hiding cognitohazards in your client...")
        HexDebugServerConfig.initClient()
        HexDebugClientConfig.init()
        DebugProxyClient.init()
        addScryingLensOverlays()
        registerSplicingTableIotaRenderers()
        registerClientResourceReloadListener(HexDebug.id("splicing_iotas"), SplicingTableIotasResourceReloadListener)
    }

    fun getConfigScreen(parent: Screen): Screen {
        return AutoConfig.getConfigScreen(HexDebugClientConfig.GlobalConfig::class.java, parent).get()
    }

    private fun addScryingLensOverlays() {
        ScryingLensOverlayRegistry.addDisplayer(HexDebugBlocks.FOCUS_HOLDER.id) { lines, _, pos, _, level, _ ->
            FocusHolderBlock.getBlockEntity(level, pos)?.apply {
                if (iotaStack.isEmpty) return@apply

                // item + name
                lines += getItemLine(iotaStack)

                // contained iota, if any
                readIotaTag()?.let {
                    lines += getIotaLine(Items.PAPER, it)
                }
            }
        }
    }

    private fun getItemLine(stack: ItemStack) = MojangPair(stack, stack.styledHoverName)

    @Suppress("SameParameterValue")
    private fun getIotaLine(item: Item, tag: CompoundTag) = getIotaLine(ItemStack(item), tag)

    private fun getIotaLine(stack: ItemStack, tag: CompoundTag): ScryingLensLine {
        val mc = Minecraft.getInstance()
        val font = mc.font

        // see HexAdditionalRenderers
        val maxWidth = (mc.window.guiScaledWidth / 2f * 0.8f).toInt()

        val fullDisplay = IotaType.getDisplay(tag)
        val displayLines = font.splitter.splitLines(fullDisplay, maxWidth, Style.EMPTY)

        val truncatedDisplay = Component.empty()
        for ((i, line) in displayLines.withIndex()) {
            if (i > 0) {
                truncatedDisplay += " ".asTextComponent
            }
            if (i >= MAX_IOTA_DISPLAY_LINES) {
                truncatedDisplay += "...".asTextComponent.gray
                break
            }
            truncatedDisplay += line.toComponent()
        }

        return MojangPair(stack, truncatedDisplay)
    }

    private fun registerSplicingTableIotaRenderers() {
        SplicingTableIotaRenderers.register(HexDebug.id("list"), ListRendererProvider.PARSER)
        SplicingTableIotaRenderers.register(HexDebug.id("pattern"), PatternRendererProvider.PARSER)
        SplicingTableIotaRenderers.register(HexDebug.id("texture"), TextureRendererProvider.PARSER)
    }
}
