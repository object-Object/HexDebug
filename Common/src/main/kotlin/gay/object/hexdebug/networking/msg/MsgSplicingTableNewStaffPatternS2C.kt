package gay.`object`.hexdebug.networking.msg

import at.petrak.hexcasting.api.spell.casting.ResolvedPatternType
import net.minecraft.network.FriendlyByteBuf

/** Requests the server to run a splicing table action. */
data class MsgSplicingTableNewStaffPatternS2C(
    val resolutionType: ResolvedPatternType,
    val index: Int,
) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanion<MsgSplicingTableNewStaffPatternS2C> {
        override val type = MsgSplicingTableNewStaffPatternS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableNewStaffPatternS2C(
            resolutionType = buf.readEnum(ResolvedPatternType::class.java),
            index = buf.readInt(),
        )

        override fun MsgSplicingTableNewStaffPatternS2C.encode(buf: FriendlyByteBuf) {
            buf.writeEnum(resolutionType)
            buf.writeInt(index)
        }
    }
}
