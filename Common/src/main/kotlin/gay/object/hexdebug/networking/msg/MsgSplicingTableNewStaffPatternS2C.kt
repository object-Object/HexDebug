package gay.`object`.hexdebug.networking.msg

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.gui.SplicingTableScreen
import gay.`object`.hexdebug.networking.HexDebugMessageCompanionS2C
import gay.`object`.hexdebug.networking.HexDebugMessageS2C
import net.minecraft.network.FriendlyByteBuf

/** Requests the server to run a splicing table action. */
data class MsgSplicingTableNewStaffPatternS2C(val resolutionType: ResolvedPatternType, val index: Int) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanionS2C<MsgSplicingTableNewStaffPatternS2C> {
        override val type = MsgSplicingTableNewStaffPatternS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableNewStaffPatternS2C(
            resolutionType = buf.readEnum(ResolvedPatternType::class.java),
            index = buf.readInt(),
        )

        override fun MsgSplicingTableNewStaffPatternS2C.encode(buf: FriendlyByteBuf) {
            buf.writeEnum(resolutionType)
            buf.writeInt(index)
        }

        override fun MsgSplicingTableNewStaffPatternS2C.applyOnClient(ctx: PacketContext) = ctx.queue {
            val info = ExecutionClientView(false, resolutionType, listOf(), null)
            SplicingTableScreen.getInstance()?.guiSpellcasting?.recvServerUpdate(info, index)
        }
    }
}
