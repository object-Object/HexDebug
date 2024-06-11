package gay.`object`.hexdebug.networking.msg

import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableAction
import gay.`object`.hexdebug.splicing.readSelection
import gay.`object`.hexdebug.splicing.writeSelection
import net.minecraft.network.FriendlyByteBuf

/** Requests the server to run a splicing table action. */
data class MsgSplicingTableActionC2S(val action: SplicingTableAction, val selection: Selection?) : HexDebugMessageC2S {
    companion object : HexDebugMessageCompanion<MsgSplicingTableActionC2S> {
        override val type = MsgSplicingTableActionC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableActionC2S(
            action = buf.readEnum(SplicingTableAction::class.java),
            selection = buf.readSelection(),
        )

        override fun MsgSplicingTableActionC2S.encode(buf: FriendlyByteBuf) {
            buf.writeEnum(action)
            buf.writeSelection(selection)
        }
    }
}
