package gay.`object`.hexdebug.networking.msg

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import net.minecraft.network.FriendlyByteBuf

/**
 * Similar to [at.petrak.hexcasting.common.msgs.MsgNewSpellPatternS2C], but is only applied if holding an Evaluator.
 * This avoids interfering with other staves' client view when stepping through a debug session.
 */
data class MsgEvaluatorClientInfoS2C(val info: ExecutionClientView) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanion<MsgEvaluatorClientInfoS2C> {
        override val type = MsgEvaluatorClientInfoS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgEvaluatorClientInfoS2C(
            ExecutionClientView(
                isStackClear = buf.readBoolean(),
                resolutionType = buf.readEnum(ResolvedPatternType::class.java),
                stackDescs = buf.readList(FriendlyByteBuf::readNbt).filterNotNull(),
                ravenmind = buf.readNullable(FriendlyByteBuf::readNbt),
            )
        )

        override fun MsgEvaluatorClientInfoS2C.encode(buf: FriendlyByteBuf) {
            info.apply {
                buf.writeBoolean(isStackClear)
                buf.writeEnum(resolutionType)
                buf.writeCollection(stackDescs, FriendlyByteBuf::writeNbt)
                buf.writeNullable(ravenmind, FriendlyByteBuf::writeNbt)
            }
        }
    }
}
