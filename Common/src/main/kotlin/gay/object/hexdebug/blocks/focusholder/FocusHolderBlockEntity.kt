package gay.`object`.hexdebug.blocks.focusholder

import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.block.HexBlockEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.utils.asTextComponent
import at.petrak.hexcasting.api.utils.gray
import at.petrak.hexcasting.api.utils.plusAssign
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.blocks.base.BaseContainer
import gay.`object`.hexdebug.blocks.base.ContainerSlotDelegate
import gay.`object`.hexdebug.registry.HexDebugBlockEntities
import gay.`object`.hexdebug.utils.styledHoverName
import gay.`object`.hexdebug.utils.toComponent
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.ContainerHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.state.BlockState
import kotlin.collections.MutableList
import kotlin.collections.plusAssign
import kotlin.collections.withIndex
import com.mojang.datafixers.util.Pair as MojangPair

class FocusHolderBlockEntity(pos: BlockPos, state: BlockState) :
    HexBlockEntity(HexDebugBlockEntities.FOCUS_HOLDER.value, pos, state),
    BaseContainer, ADIotaHolder
{
    override val stacks = BaseContainer.withSize(1)

    var iotaStack by ContainerSlotDelegate(0)

    private val iotaHolder get() = IXplatAbstractions.INSTANCE.findDataHolder(iotaStack)

    override fun loadModData(tag: CompoundTag) {
        stacks.clear() // without this, removing the item on the server doesn't remove it on the client
        ContainerHelper.loadAllItems(tag, stacks)
    }

    override fun saveModData(tag: CompoundTag) {
        ContainerHelper.saveAllItems(tag, stacks)
    }

    override fun readIotaTag() = iotaHolder?.readIotaTag()

    override fun writeIota(iota: Iota?, simulate: Boolean) = iotaHolder?.writeIota(iota, simulate) ?: false

    fun addScryingLensLines(lines: MutableList<MojangPair<ItemStack, Component>>) {
        if (iotaStack.isEmpty) return

        lines += MojangPair(iotaStack, iotaStack.styledHoverName)

        readIotaTag()?.let { tag ->
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

            lines += MojangPair(ItemStack(Items.PAPER), truncatedDisplay)
        }
    }

    companion object {
        const val MAX_IOTA_DISPLAY_LINES = 4
    }
}
