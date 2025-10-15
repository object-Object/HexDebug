package gay.`object`.hexdebug.networking.msg

import gay.`object`.hexdebug.items.DebuggerItem.DebugState
import net.minecraft.network.FriendlyByteBuf

data class MsgDebuggerStateS2C(val debugStates: Map<Int, DebugState>) : HexDebugMessageS2C {
    constructor(threadId: Int, debugState: DebugState) : this(mapOf(threadId to debugState))

    companion object : HexDebugMessageCompanion<MsgDebuggerStateS2C> {
        override val type = MsgDebuggerStateS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgDebuggerStateS2C(
            buf.readMap(FriendlyByteBuf::readInt) { it.readEnum(DebugState::class.java) },
        )

        override fun MsgDebuggerStateS2C.encode(buf: FriendlyByteBuf) {
            buf.writeMap(debugStates, FriendlyByteBuf::writeInt, FriendlyByteBuf::writeEnum)
        }
    }
}
