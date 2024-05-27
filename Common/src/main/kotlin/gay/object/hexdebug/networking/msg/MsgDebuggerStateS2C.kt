package gay.`object`.hexdebug.networking.msg

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.items.ItemDebugger
import gay.`object`.hexdebug.items.ItemDebugger.DebugState
import gay.`object`.hexdebug.items.ItemEvaluator
import gay.`object`.hexdebug.items.ItemEvaluator.EvalState
import gay.`object`.hexdebug.networking.HexDebugMessageCompanionS2C
import gay.`object`.hexdebug.networking.HexDebugMessageS2C
import net.minecraft.network.FriendlyByteBuf

data class MsgDebuggerStateS2C(val debuggerState: DebugState) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanionS2C<MsgDebuggerStateS2C> {
        override val type = MsgDebuggerStateS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgDebuggerStateS2C(
            buf.readEnum(DebugState::class.java),
        )

        override fun MsgDebuggerStateS2C.encode(buf: FriendlyByteBuf) {
            buf.writeEnum(debuggerState)
        }

        override fun MsgDebuggerStateS2C.applyOnClient(ctx: PacketContext) = ctx.queue {
            ItemDebugger.debugState = debuggerState
            if (debuggerState == DebugState.NOT_DEBUGGING) {
                ItemEvaluator.evalState = EvalState.DEFAULT
            }
        }
    }
}
