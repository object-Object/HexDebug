package gay.`object`.hexdebug.networking.msg

import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.readSelection
import gay.`object`.hexdebug.splicing.writeSelection
import net.minecraft.network.FriendlyByteBuf

/** The result of running a splicing table action on the server. */
data class MsgSplicingTableNewSelectionS2C(val selection: Selection?) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanion<MsgSplicingTableNewSelectionS2C> {
        override val type = MsgSplicingTableNewSelectionS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableNewSelectionS2C(
            selection = buf.readSelection(),
        )

        override fun MsgSplicingTableNewSelectionS2C.encode(buf: FriendlyByteBuf) {
            buf.writeSelection(selection)
        }
    }
}
