package gay.`object`.hexdebug.networking.msg

import net.minecraft.network.FriendlyByteBuf

class MsgSplicingTableCastHexC2S : HexDebugMessageC2S {
    companion object : HexDebugMessageCompanion<MsgSplicingTableCastHexC2S> {
        override val type = MsgSplicingTableCastHexC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableCastHexC2S()

        override fun MsgSplicingTableCastHexC2S.encode(buf: FriendlyByteBuf) {}
    }
}
