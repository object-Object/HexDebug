package gay.`object`.hexdebug.networking.handler

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView
import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.adapter.proxy.DebugProxyClient
import gay.`object`.hexdebug.config.DebuggerDisplayMode
import gay.`object`.hexdebug.config.HexDebugConfig
import gay.`object`.hexdebug.gui.splicing.SplicingTableMenu
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import gay.`object`.hexdebug.items.DebuggerItem
import gay.`object`.hexdebug.items.DebuggerItem.DebugState
import gay.`object`.hexdebug.items.EvaluatorItem
import gay.`object`.hexdebug.items.EvaluatorItem.EvalState
import gay.`object`.hexdebug.networking.msg.*
import net.minecraft.network.chat.Component

fun HexDebugMessageS2C.applyOnClient(ctx: PacketContext) {
    when (this) {
        is MsgDebugAdapterProxy -> {
            DebugProxyClient.instance?.consume(content)
        }

        is MsgDebuggerStateS2C -> {
            DebuggerItem.debugState = debuggerState
            if (debuggerState == DebugState.NOT_DEBUGGING) {
                EvaluatorItem.evalState = EvalState.DEFAULT
            }
        }

        is MsgEvaluatorStateS2C -> {
            EvaluatorItem.evalState = evalState
        }

        is MsgPrintDebuggerStatusS2C -> {
            val config = HexDebugConfig.client
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

        is MsgSyncConfigS2C -> {
            HexDebugConfig.onSyncConfig(serverConfig)
        }
    }
}
