package gay.`object`.hexdebug.networking

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.items.ItemEvaluator
import gay.`object`.hexdebug.items.ItemEvaluator.EvalState
import net.minecraft.network.FriendlyByteBuf
import java.util.function.Supplier

data class MsgEvaluatorStateS2C(private val evalState: EvalState) {
    constructor(buf: FriendlyByteBuf) : this(
        buf.readEnum(EvalState::class.java),
    )

    fun encode(buf: FriendlyByteBuf) {
        buf.writeEnum(evalState)
    }

    fun apply(supplier: Supplier<PacketContext>) = supplier.get().also { ctx ->
        ctx.queue {
            HexDebug.LOGGER.debug("Client received packet: {}", this)
            ItemEvaluator.evalState = evalState
        }
    }
}
