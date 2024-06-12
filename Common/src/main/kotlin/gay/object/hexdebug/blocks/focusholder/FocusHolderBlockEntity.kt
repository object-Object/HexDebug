package gay.`object`.hexdebug.blocks.focusholder

import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.utils.asTranslatedComponent
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.blocks.base.BaseContainer
import gay.`object`.hexdebug.blocks.base.ContainerSlotDelegate
import gay.`object`.hexdebug.gui.focusholder.FocusHolderMenu
import gay.`object`.hexdebug.registry.HexDebugBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class FocusHolderBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(HexDebugBlockEntities.FOCUS_HOLDER.value, pos, state),
    BaseContainer, MenuProvider, ADIotaHolder
{
    override val stacks = BaseContainer.withSize(1)

    private var iotaStack by ContainerSlotDelegate(0)

    private val iotaHolder get() = IXplatAbstractions.INSTANCE.findDataHolder(iotaStack)

    override fun load(tag: CompoundTag) {
        super.load(tag)
        ContainerHelper.loadAllItems(tag, stacks)
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        ContainerHelper.saveAllItems(tag, stacks)
    }

    override fun getDisplayName() = blockState.block.descriptionId.asTranslatedComponent

    override fun createMenu(i: Int, inventory: Inventory, player: Player) = FocusHolderMenu(i, inventory, this)

    override fun readIotaTag() = iotaHolder?.readIotaTag()

    override fun writeIota(iota: Iota?, simulate: Boolean) = iotaHolder?.writeIota(iota, simulate) ?: false
}
