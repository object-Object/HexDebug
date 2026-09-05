package gay.`object`.hexdebug.utils

import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.world.item.ItemStack

val INTROSPECTION = HexPattern.fromAngles("qqq", HexDir.WEST)
val RETROSPECTION = HexPattern.fromAngles("eee", HexDir.EAST)

fun isIotaHolder(stack: ItemStack) = IXplatAbstractions.INSTANCE.findDataHolder(stack) != null
