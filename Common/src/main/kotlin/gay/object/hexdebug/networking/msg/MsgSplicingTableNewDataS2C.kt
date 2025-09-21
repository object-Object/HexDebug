package gay.`object`.hexdebug.networking.msg

import at.petrak.hexcasting.api.casting.math.HexPattern
import gay.`object`.hexdebug.splicing.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf

/** The result of running a splicing table action on the server. */
data class MsgSplicingTableNewDataS2C(
    val data: SplicingTableClientView,
    // these are sent in the container data, but we also need to send them in this packet to avoid weird flickering
    val selection: Selection?,
    val viewStartIndex: Int,
) : HexDebugMessageS2C {
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
                            pattern = buf.readNbt()?.let(HexPattern::fromNBT)
                        )
                    }
                },
                clipboard = buf.readNbt(),
                isListWritable = buf.readBoolean(),
                isClipboardWritable = buf.readBoolean(),
                isEnlightened = buf.readBoolean(),
                hasHex = buf.readBoolean(),
                undoSize = buf.readInt(),
                undoIndex = buf.readInt(),
            ),
            selection = buf.readSelection(),
            viewStartIndex = buf.readInt(),
        )

        override fun MsgSplicingTableNewDataS2C.encode(buf: FriendlyByteBuf) {
            data.apply {
                buf.writeNullable(list) { _, list ->
                    buf.writeCollection(list) { _, it ->
                        buf.writeNbt(it.tag)
                        buf.writeComponent(it.name)
                        buf.writeUtf(it.hexpatternSource)
                        buf.writeNbt(it.pattern?.serializeToNBT())
                    }
                }
                buf.writeNbt(clipboard)
                buf.writeBoolean(isListWritable)
                buf.writeBoolean(isClipboardWritable)
                buf.writeBoolean(isEnlightened)
                buf.writeBoolean(hasHex)
                buf.writeInt(undoSize)
                buf.writeInt(undoIndex)
                buf.writeSelection(selection)
                buf.writeInt(viewStartIndex)
            }
        }
    }
}
