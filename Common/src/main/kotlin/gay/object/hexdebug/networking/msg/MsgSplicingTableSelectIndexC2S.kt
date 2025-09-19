package gay.`object`.hexdebug.networking.msg

import net.minecraft.network.FriendlyByteBuf

data class MsgSplicingTableSelectIndexC2S(
    val index: Int,
    val hasShiftDown: Boolean,
    val isIota: Boolean,
) : HexDebugMessageC2S {
    companion object : HexDebugMessageCompanion<MsgSplicingTableSelectIndexC2S> {
        override val type = MsgSplicingTableSelectIndexC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableSelectIndexC2S(
            index = buf.readInt(),
            hasShiftDown = buf.readBoolean(),
            isIota = buf.readBoolean(),
        )

        override fun MsgSplicingTableSelectIndexC2S.encode(buf: FriendlyByteBuf) {
            buf.writeInt(index)
            buf.writeBoolean(hasShiftDown)
            buf.writeBoolean(isIota)
        }
    }
}
