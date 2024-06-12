package gay.`object`.hexdebug.networking.msg

import gay.`object`.hexdebug.items.EvaluatorItem.EvalState
import net.minecraft.network.FriendlyByteBuf

data class MsgEvaluatorStateS2C(val evalState: EvalState) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanion<MsgEvaluatorStateS2C> {
        override val type = MsgEvaluatorStateS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgEvaluatorStateS2C(
            buf.readEnum(EvalState::class.java),
        )

        override fun MsgEvaluatorStateS2C.encode(buf: FriendlyByteBuf) {
            buf.writeEnum(evalState)
        }
    }
}
