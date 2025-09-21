package gay.`object`.hexdebug.blocks.focusholder

import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.block.HexBlockEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.blocks.base.BaseContainer
import gay.`object`.hexdebug.blocks.base.ContainerSlotDelegate
import gay.`object`.hexdebug.registry.HexDebugBlockEntities
import gay.`object`.hexdebug.utils.isNotEmpty
import gay.`object`.hexdebug.utils.setPropertyIfChanged
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.ContainerHelper
import net.minecraft.world.level.block.state.BlockState

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

    override fun writeIota(iota: Iota?, simulate: Boolean): Boolean {
        val success = iotaHolder?.writeIota(iota, simulate) ?: false
        if (!simulate && success) {
            sync() // sync container to clients, eg. to update scrying lens readout
        }
        return success
    }

    override fun writeable() = iotaHolder?.writeable() ?: false

    override fun setChanged() {
        super.setChanged()
        setPropertyIfChanged(FocusHolderBlock.HAS_ITEM, isNotEmpty)
    }
}
