package gay.`object`.hexdebug.networking.msg

import gay.`object`.hexdebug.splicing.IotaClientView
import gay.`object`.hexdebug.splicing.SplicingTableClientView
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf

/** The result of running a splicing table action on the server. */
data class MsgSplicingTableNewDataS2C(val data: SplicingTableClientView) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanion<MsgSplicingTableNewDataS2C> {
        override val type = MsgSplicingTableNewDataS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableNewDataS2C(
            data = SplicingTableClientView(
                list = buf.readNullable {
                    buf.readList {
                        IotaClientView(
                            tag = buf.readNbt() ?: CompoundTag(),
                            name = buf.readComponent(),
                            hexpatternSource = buf.readUtf(),
                        )
                    }
                },
                clipboard = buf.readNbt(),
                isListWritable = buf.readBoolean(),
                isClipboardWritable = buf.readBoolean(),
                undoSize = buf.readInt(),
                undoIndex = buf.readInt(),
            ),
        )

        override fun MsgSplicingTableNewDataS2C.encode(buf: FriendlyByteBuf) {
            data.apply {
                buf.writeNullable(list) { _, list ->
                    buf.writeCollection(list) { _, it ->
                        buf.writeNbt(it.tag)
                        buf.writeComponent(it.name)
                        buf.writeUtf(it.hexpatternSource)
                    }
                }
                buf.writeNbt(clipboard)
                buf.writeBoolean(isListWritable)
                buf.writeBoolean(isClipboardWritable)
                buf.writeInt(undoSize)
                buf.writeInt(undoIndex)
            }
        }
    }
}
