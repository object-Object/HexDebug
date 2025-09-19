package gay.`object`.hexdebug.networking.msg

import gay.`object`.hexdebug.splicing.SplicingTableAction
import net.minecraft.network.FriendlyByteBuf

/** Requests the server to run a splicing table action. */
data class MsgSplicingTableActionC2S(val action: SplicingTableAction) : HexDebugMessageC2S {
    companion object : HexDebugMessageCompanion<MsgSplicingTableActionC2S> {
        override val type = MsgSplicingTableActionC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableActionC2S(
            action = buf.readEnum(SplicingTableAction::class.java),
        )

        override fun MsgSplicingTableActionC2S.encode(buf: FriendlyByteBuf) {
            buf.writeEnum(action)
        }
    }
}
