package gay.`object`.hexdebug.networking.msg

import gay.`object`.hexdebug.items.ItemDebugger.DebugState
import net.minecraft.network.FriendlyByteBuf

data class MsgDebuggerStateS2C(val debuggerState: DebugState) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanion<MsgDebuggerStateS2C> {
        override val type = MsgDebuggerStateS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgDebuggerStateS2C(
            buf.readEnum(DebugState::class.java),
        )

        override fun MsgDebuggerStateS2C.encode(buf: FriendlyByteBuf) {
            buf.writeEnum(debuggerState)
        }
    }
}
