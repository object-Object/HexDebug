package gay.`object`.hexdebug.networking.msg

import at.petrak.hexcasting.api.casting.math.HexPattern
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.readSelection
import gay.`object`.hexdebug.splicing.writeSelection
import net.minecraft.network.FriendlyByteBuf

/** Requests the server to run a splicing table action. */
data class MsgSplicingTableNewStaffPatternC2S(
    val pattern: HexPattern,
    val index: Int,
    val selection: Selection?,
) : HexDebugMessageC2S {
    companion object : HexDebugMessageCompanion<MsgSplicingTableNewStaffPatternC2S> {
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
    }
}
