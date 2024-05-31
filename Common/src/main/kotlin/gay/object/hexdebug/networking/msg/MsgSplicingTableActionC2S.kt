package gay.`object`.hexdebug.networking.msg

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Action
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Selection
import gay.`object`.hexdebug.networking.HexDebugMessageC2S
import gay.`object`.hexdebug.networking.HexDebugMessageCompanionC2S
import net.minecraft.network.FriendlyByteBuf

data class MsgSplicingTableActionC2S(val action: Action, val selection: Selection) : HexDebugMessageC2S {
    companion object : HexDebugMessageCompanionC2S<MsgSplicingTableActionC2S> {
        override val type = MsgSplicingTableActionC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableActionC2S(
            action = buf.readEnum(Action::class.java),
            selection = Selection(
                start = buf.readInt(),
                end = buf.readInt(),
            ),
        )

        override fun MsgSplicingTableActionC2S.encode(buf: FriendlyByteBuf) {
            buf.writeEnum(action)
            selection.apply {
                buf.writeInt(start)
                buf.writeInt(end)
            }
        }

        override fun MsgSplicingTableActionC2S.applyOnServer(ctx: PacketContext) = ctx.queue {

        }
    }
}
