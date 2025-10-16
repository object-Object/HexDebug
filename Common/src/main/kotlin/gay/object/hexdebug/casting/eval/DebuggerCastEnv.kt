package gay.`object`.hexdebug.casting.eval

import at.petrak.hexcasting.api.casting.eval.env.PackagedItemCastEnv
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect
import gay.`object`.hexdebug.core.api.debugging.IDebuggableCastEnv
import gay.`object`.hexdebug.utils.findMediaHolderInHand
import gay.`object`.hexdebug.utils.otherHand
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

class DebuggerCastEnv(
    caster: ServerPlayer,
    castingHand: InteractionHand,
) : PackagedItemCastEnv(caster, castingHand), IDebuggableCastEnv {
    private val item = caster.getItemInHand(castingHand).item

    override fun printMessage(message: Component) {
        super.printMessage(message)
        debugEnv?.printDebugMessage(message)
    }

    override fun sendMishapMsgToPlayer(mishap: OperatorSideEffect.DoMishap) {
        super.sendMishapMsgToPlayer(mishap)
        debugEnv?.printDebugMishap(this, mishap)
    }

    override fun extractMediaEnvironment(cost: Long, simulate: Boolean): Long {
        if (caster.isCreative) return 0

        var costLeft = cost

        // allow extracting from a debugger item in either hand, preferring the one we started casting with
        // NOTE: if holding two debuggers and the first is empty, this will take the empty one, not the other one
        val casterMediaHolder = caster.findMediaHolderInHand(castingHand, item)
            ?: caster.findMediaHolderInHand(castingHand.otherHand, item)

        // The contracts on the AD and on this function are different.
        // ADs return the amount extracted, this wants the amount left
        if (casterMediaHolder != null) {
            val extracted = casterMediaHolder.withdrawMedia(costLeft.toInt().toLong(), simulate)
            costLeft -= extracted
        }

        // debugger can always extract from inventory
        if (costLeft > 0) {
            costLeft = extractMediaFromInventory(costLeft, canOvercast(), simulate)
        }

        return costLeft
    }
}
