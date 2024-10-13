package gay.`object`.hexdebug

import at.petrak.hexcasting.api.client.ScryingLensOverlayRegistry
import at.petrak.hexcasting.api.utils.asTextComponent
import at.petrak.hexcasting.api.utils.gray
import at.petrak.hexcasting.api.utils.plusAssign
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import gay.`object`.hexdebug.adapter.proxy.DebugProxyClient
import gay.`object`.hexdebug.blocks.focusholder.FocusHolderBlock
import gay.`object`.hexdebug.config.HexDebugConfig
import gay.`object`.hexdebug.config.HexDebugConfig.GlobalConfig
import gay.`object`.hexdebug.registry.HexDebugBlocks
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
        HexDebugConfig.initClient()
        DebugProxyClient.init()
        addScryingLensOverlays()
    }

    fun getConfigScreen(parent: Screen): Screen {
        return AutoConfig.getConfigScreen(GlobalConfig::class.java, parent).get()
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

        val fullDisplay = HexIotaTypes.getDisplay(tag)
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
}
