package gay.`object`.hexdebug.networking.msg

import net.minecraft.network.FriendlyByteBuf

/** Asks the server to send a [MsgSplicingTableNewDataS2C] packet. */
class MsgSplicingTableGetDataC2S : HexDebugMessageC2S {
    companion object : HexDebugMessageCompanion<MsgSplicingTableGetDataC2S> {
        override val type = MsgSplicingTableGetDataC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableGetDataC2S()

        override fun MsgSplicingTableGetDataC2S.encode(buf: FriendlyByteBuf) {}
    }
}
