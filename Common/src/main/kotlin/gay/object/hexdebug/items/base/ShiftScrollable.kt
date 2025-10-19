package gay.`object`.hexdebug.items.base

import at.petrak.hexcasting.api.utils.*
import gay.`object`.hexdebug.config.HexDebugServerConfig
import gay.`object`.hexdebug.core.api.HexDebugCoreAPI
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

fun rotateThreadId(caster: ServerPlayer, stack: ItemStack, increase: Boolean): Component {
    val threadId = (getThreadId(stack) + (if (increase) 1 else -1))
        .coerceIn(0 until HexDebugServerConfig.config.maxDebugThreads)
    stack.putInt(THREAD_ID_TAG, threadId)
    return displayThread(caster, threadId)
}

fun displayThread(caster: ServerPlayer?, threadId: Int): Component {
    val threadText = threadId.toString().asTextComponent.white
    val envName = caster?.let { HexDebugCoreAPI.INSTANCE.getDebugEnv(it, threadId) }?.name
    val component = if (envName != null) {
        "hexdebug.tooltip.thread.active".asTranslatedComponent(threadText, envName)
    } else {
        "hexdebug.tooltip.thread.inactive".asTranslatedComponent(threadText)
    }
    return component.gray
}
