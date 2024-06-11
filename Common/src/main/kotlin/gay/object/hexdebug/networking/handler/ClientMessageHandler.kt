package gay.`object`.hexdebug.networking.handler

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView
import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.adapter.proxy.DebugProxyClient
import gay.`object`.hexdebug.config.DebuggerDisplayMode
import gay.`object`.hexdebug.config.HexDebugConfig
import gay.`object`.hexdebug.gui.SplicingTableMenu
import gay.`object`.hexdebug.gui.SplicingTableScreen
import gay.`object`.hexdebug.items.ItemDebugger
import gay.`object`.hexdebug.items.ItemDebugger.DebugState
import gay.`object`.hexdebug.items.ItemEvaluator
import gay.`object`.hexdebug.items.ItemEvaluator.EvalState
import gay.`object`.hexdebug.networking.msg.*
import net.minecraft.network.chat.Component

fun HexDebugMessageS2C.applyOnClient(ctx: PacketContext) {
    when (this) {
        is MsgDebugAdapterProxy -> {
            DebugProxyClient.instance?.consume(content)
        }

        is MsgDebuggerStateS2C -> {
            ItemDebugger.debugState = debuggerState
            if (debuggerState == DebugState.NOT_DEBUGGING) {
                ItemEvaluator.evalState = EvalState.DEFAULT
            }
        }

        is MsgEvaluatorStateS2C -> {
            ItemEvaluator.evalState = evalState
        }

        is MsgPrintDebuggerStatusS2C -> {
            val config = HexDebugConfig.get().client
            val shouldPrint = when (config.debuggerDisplayMode) {
                DebuggerDisplayMode.DISABLED -> false
                DebuggerDisplayMode.NOT_CONNECTED -> !isConnected
                DebuggerDisplayMode.ENABLED -> true
            }

            if (shouldPrint) {
                ctx.player.displayClientMessage(
                    Component.translatable(
                        "text.hexdebug.debugger_stopped",
                        if (config.showDebugClientLineNumber) line else index,
                        iota,
                    ),
                    true,
                )
            }
        }

        is MsgSplicingTableNewDataS2C -> {
            SplicingTableMenu.getInstance(ctx.player)?.also { menu ->
                menu.clientView = data
                SplicingTableScreen.getInstance()?.updateButtons()
            }
        }

        is MsgSplicingTableNewSelectionS2C -> {
            SplicingTableScreen.getInstance()?.selection = selection
        }

        is MsgSplicingTableNewStaffPatternS2C -> {
            val info = ExecutionClientView(false, resolutionType, listOf(), null)
            SplicingTableScreen.getInstance()?.guiSpellcasting?.recvServerUpdate(info, index)
        }
    }
}