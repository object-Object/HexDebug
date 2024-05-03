package gay.`object`.hexdebug.casting.eval

import at.petrak.hexcasting.api.casting.eval.env.PackagedItemCastEnv
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

class DebugItemCastEnv(
    caster: ServerPlayer,
    castingHand: InteractionHand,
) : PackagedItemCastEnv(caster, castingHand), IDebugCastEnv {
    override fun printMessage(message: Component) {
        super.printMessage(message)
        DebugAdapterManager[caster]?.print(message.string + "\n")
    }
}
