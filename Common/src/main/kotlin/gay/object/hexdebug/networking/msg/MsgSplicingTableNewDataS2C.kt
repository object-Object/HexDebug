package gay.`object`.hexdebug.networking.msg

import gay.`object`.hexdebug.splicing.SplicingTableClientView
import net.minecraft.network.FriendlyByteBuf

/** The result of running a splicing table action on the server. */
data class MsgSplicingTableNewDataS2C(val data: SplicingTableClientView) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanion<MsgSplicingTableNewDataS2C> {
        override val type = MsgSplicingTableNewDataS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableNewDataS2C(
            data = SplicingTableClientView(
                list = buf.readNullable { it.readList(FriendlyByteBuf::readNbt) }?.filterNotNull(),
                clipboard = buf.readNullable(FriendlyByteBuf::readNbt),
                isListWritable = buf.readBoolean(),
                isClipboardWritable = buf.readBoolean(),
                undoSize = buf.readInt(),
                undoIndex = buf.readInt(),
            ),
        )

        override fun MsgSplicingTableNewDataS2C.encode(buf: FriendlyByteBuf) {
            data.apply {
                buf.writeNullable(list) { buf, list -> buf.writeCollection(list, FriendlyByteBuf::writeNbt) }
                buf.writeNullable(clipboard, FriendlyByteBuf::writeNbt)
                buf.writeBoolean(isListWritable)
                buf.writeBoolean(isClipboardWritable)
                buf.writeInt(undoSize)
                buf.writeInt(undoIndex)
            }
        }
    }
}
