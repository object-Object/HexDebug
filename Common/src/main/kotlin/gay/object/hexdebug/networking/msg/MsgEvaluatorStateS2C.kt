package gay.`object`.hexdebug.networking.msg

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.items.ItemEvaluator
import gay.`object`.hexdebug.items.ItemEvaluator.EvalState
import gay.`object`.hexdebug.networking.HexDebugMessageCompanionS2C
import gay.`object`.hexdebug.networking.HexDebugMessageS2C
import net.minecraft.network.FriendlyByteBuf

data class MsgEvaluatorStateS2C(val evalState: EvalState) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanionS2C<MsgEvaluatorStateS2C> {
        override val type = MsgEvaluatorStateS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgEvaluatorStateS2C(
            buf.readEnum(EvalState::class.java),
        )

        override fun MsgEvaluatorStateS2C.encode(buf: FriendlyByteBuf) {
            buf.writeEnum(evalState)
        }

        override fun MsgEvaluatorStateS2C.applyOnClient(ctx: PacketContext) = ctx.queue {
            ItemEvaluator.evalState = evalState
        }
    }
}
