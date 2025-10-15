package gay.`object`.hexdebug.items.base

import at.petrak.hexcasting.api.utils.asTranslatedComponent
import at.petrak.hexcasting.api.utils.getInt
import at.petrak.hexcasting.api.utils.putInt
import gay.`object`.hexdebug.config.HexDebugServerConfig
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

interface ShiftScrollable {
    fun canShiftScroll(isCtrl: Boolean): Boolean

    fun handleShiftScroll(sender: ServerPlayer, stack: ItemStack, delta: Double, isCtrl: Boolean)
}

// for items that can ctrl+shift+scroll through debug threads

private const val THREAD_ID_TAG = "thread_id"

fun getThreadId(stack: ItemStack) = stack.getInt(THREAD_ID_TAG)

fun rotateThreadId(stack: ItemStack, increase: Boolean): Component {
    val threadId = (getThreadId(stack) + (if (increase) 1 else -1))
        .coerceIn(0 until HexDebugServerConfig.config.maxDebugThreads)
    stack.putInt(THREAD_ID_TAG, threadId)
    return "hexdebug.tooltip.thread_id".asTranslatedComponent(threadId)
}
