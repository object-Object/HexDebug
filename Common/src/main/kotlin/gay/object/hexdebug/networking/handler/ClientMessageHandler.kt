package gay.`object`.hexdebug.networking.handler

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView
import at.petrak.hexcasting.client.gui.GuiSpellcasting
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternS2C
import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.adapter.proxy.DebugProxyClient
import gay.`object`.hexdebug.config.DebuggerDisplayMode
import gay.`object`.hexdebug.config.HexDebugClientConfig
import gay.`object`.hexdebug.config.HexDebugServerConfig
import gay.`object`.hexdebug.gui.splicing.SplicingTableMenu
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import gay.`object`.hexdebug.gui.splicing.mixin
import gay.`object`.hexdebug.items.DebuggerItem
import gay.`object`.hexdebug.items.DebuggerItem.DebugState
import gay.`object`.hexdebug.items.EvaluatorItem
import gay.`object`.hexdebug.items.EvaluatorItem.EvalState
import gay.`object`.hexdebug.networking.msg.*
import gay.`object`.hexdebug.registry.HexDebugItems
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

fun HexDebugMessageS2C.applyOnClient(ctx: PacketContext) = ctx.queue {
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

        is MsgEvaluatorClientInfoS2C -> {
            (Minecraft.getInstance().screen as? GuiSpellcasting)?.let { screen ->
                // only apply the message if the screen was opened with an evaluator
                val heldItem = ctx.player.getItemInHand(screen.mixin.handOpenedWith)
                if (heldItem.`is`(HexDebugItems.EVALUATOR.value)) {
                    // just delegate to the existing handler instead of copying the functionality here
                    // we use an index of -1 because we don't want to update the resolution type of any patterns
                    MsgNewSpellPatternS2C.handle(MsgNewSpellPatternS2C(info, -1))
                }
            }
        }

        is MsgPrintDebuggerStatusS2C -> {
            val config = HexDebugClientConfig.config
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
                menu.receiveData(this)
                SplicingTableScreen.getInstance()?.reloadData()
            }
        }

        is MsgSplicingTableNewStaffPatternS2C -> {
            val info = ExecutionClientView(false, resolutionType, listOf(), null)
            SplicingTableScreen.getInstance()?.guiSpellcasting?.recvServerUpdate(info, index)

            val sound = if (resolutionType.success) HexEvalSounds.NORMAL_EXECUTE else HexEvalSounds.MISHAP
            sound.sound?.let { ctx.player.playSound(it, 1f, 1f) }
        }

        is MsgSyncConfigS2C -> {
            HexDebugServerConfig.onSyncConfig(serverConfig)
        }
    }
}
