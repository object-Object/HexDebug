package gay.`object`.hexdebug.utils

import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

fun isIotaHolder(stack: ItemStack) = IXplatAbstractions.INSTANCE.findDataHolder(stack) != null

fun findDataHolder(world: Level, pos: BlockPos): ADIotaHolder? {
    return world.getBlockState(pos).block as? ADIotaHolder
        ?: world.getBlockEntity(pos) as? ADIotaHolder
}
