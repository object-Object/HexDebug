package gay.`object`.hexdebug.networking.msg

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Selection
import gay.`object`.hexdebug.gui.SplicingTableScreen
import gay.`object`.hexdebug.networking.HexDebugMessageCompanionS2C
import gay.`object`.hexdebug.networking.HexDebugMessageS2C
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf

/** The result of running a splicing table action on the server. */
data class MsgSplicingTableNewSelectionS2C(val newSelection: Selection?) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanionS2C<MsgSplicingTableNewSelectionS2C> {
        override val type = MsgSplicingTableNewSelectionS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableNewSelectionS2C(
            newSelection = Selection(
                start = buf.readInt(),
                end = buf.readInt(),
            ).takeIf { it.start >= 0 || it.end >= 0 },
        )

        override fun MsgSplicingTableNewSelectionS2C.encode(buf: FriendlyByteBuf) {
            newSelection.also {
                buf.writeInt(it?.start ?: -1)
                buf.writeInt(it?.end ?: -1)
            }
        }

        override fun MsgSplicingTableNewSelectionS2C.applyOnClient(ctx: PacketContext) = ctx.queue {
            (Minecraft.getInstance().screen as? SplicingTableScreen)?.also {
                it.selection = newSelection
            }
        }
    }
}
