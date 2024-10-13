package gay.`object`.hexdebug.networking.msg

import at.petrak.hexcasting.api.spell.casting.ControllerInfo
import at.petrak.hexcasting.api.spell.casting.ResolvedPatternType
import net.minecraft.network.FriendlyByteBuf

/**
 * Similar to [at.petrak.hexcasting.common.network.MsgNewSpellPatternAck], but is only applied if holding an Evaluator.
 * This avoids interfering with other staves' client view when stepping through a debug session.
 */
data class MsgEvaluatorClientInfoS2C(val info: ControllerInfo) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanion<MsgEvaluatorClientInfoS2C> {
        override val type = MsgEvaluatorClientInfoS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgEvaluatorClientInfoS2C(
            ControllerInfo(
                isStackClear = buf.readBoolean(),
                resolutionType = buf.readEnum(ResolvedPatternType::class.java),
                stack = buf.readList(FriendlyByteBuf::readNbt).filterNotNull(),
                parenthesized = buf.readList(FriendlyByteBuf::readNbt).filterNotNull(),
                ravenmind = buf.readNullable(FriendlyByteBuf::readNbt),
                parenCount = buf.readInt(),
            )
        )

        override fun MsgEvaluatorClientInfoS2C.encode(buf: FriendlyByteBuf) {
            info.apply {
                buf.writeBoolean(isStackClear)
                buf.writeEnum(resolutionType)
                buf.writeCollection(stack, FriendlyByteBuf::writeNbt)
                buf.writeCollection(parenthesized, FriendlyByteBuf::writeNbt)
                buf.writeNullable(ravenmind, FriendlyByteBuf::writeNbt)
                buf.writeInt(parenCount)
            }
        }
    }
}
