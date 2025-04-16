package gay.`object`.hexdebug.utils

import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.world.item.ItemStack

fun isIotaHolder(stack: ItemStack) = IXplatAbstractions.INSTANCE.findDataHolder(stack) != null
