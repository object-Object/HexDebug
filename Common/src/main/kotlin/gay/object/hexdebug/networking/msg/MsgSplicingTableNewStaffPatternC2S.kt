package gay.`object`.hexdebug.networking.msg

import at.petrak.hexcasting.api.casting.math.HexPattern
import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.gui.SplicingTableMenu
import gay.`object`.hexdebug.networking.HexDebugMessageC2S
import gay.`object`.hexdebug.networking.HexDebugMessageCompanionC2S
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.readSelection
import gay.`object`.hexdebug.splicing.writeSelection
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer

/** Requests the server to run a splicing table action. */
data class MsgSplicingTableNewStaffPatternC2S(val pattern: HexPattern, val index: Int, val selection: Selection?) : HexDebugMessageC2S {
    companion object : HexDebugMessageCompanionC2S<MsgSplicingTableNewStaffPatternC2S> {
        override val type = MsgSplicingTableNewStaffPatternC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableNewStaffPatternC2S(
            pattern = HexPattern.fromNBT(buf.readNbt()!!), // TODO: is this safe?
            index = buf.readInt(),
            selection = buf.readSelection(),
        )

        override fun MsgSplicingTableNewStaffPatternC2S.encode(buf: FriendlyByteBuf) {
            buf.writeNbt(pattern.serializeToNBT())
            buf.writeInt(index)
            buf.writeSelection(selection)
        }

        override fun MsgSplicingTableNewStaffPatternC2S.applyOnServer(ctx: PacketContext) = ctx.queue {
            val player = ctx.player as ServerPlayer
            val menu = SplicingTableMenu.getInstance(player) ?: return@queue
            val (newSelection, resolutionType) = menu.table.drawPattern(player, pattern, index, selection)
            MsgSplicingTableNewSelectionS2C(newSelection).sendToPlayer(player)
            MsgSplicingTableNewStaffPatternS2C(resolutionType, index).sendToPlayer(player)
        }
    }
}
