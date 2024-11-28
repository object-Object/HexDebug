package gay.`object`.hexdebug.casting.eval

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.MishapEnvironment
import at.petrak.hexcasting.api.pigment.FrozenPigment
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.function.Predicate

/**
 * A concrete subclass of CastingEnvironment with stubs for all methods.
 *
 * This is only used for looking up and localizing patterns.
 */
class FakeCastEnv(level: ServerLevel) : CastingEnvironment(level) {
    override fun getCastingEntity(): LivingEntity? = null

    override fun getMishapEnvironment(): MishapEnvironment = TODO()

    override fun mishapSprayPos(): Vec3 = TODO()

    override fun extractMediaEnvironment(cost: Long, simulate: Boolean): Long = cost

    override fun isVecInRangeEnvironment(vec: Vec3?): Boolean = false

    override fun hasEditPermissionsAtEnvironment(pos: BlockPos?): Boolean = false

    override fun getCastingHand(): InteractionHand = InteractionHand.MAIN_HAND

    override fun getUsableStacks(mode: StackDiscoveryMode?): MutableList<ItemStack> = mutableListOf()

    override fun getPrimaryStacks(): MutableList<HeldItemInfo> = mutableListOf()

    override fun replaceItem(stackOk: Predicate<ItemStack>?, replaceWith: ItemStack?, hand: InteractionHand?): Boolean = false

    override fun getPigment(): FrozenPigment = FrozenPigment.DEFAULT.get()

    override fun setPigment(pigment: FrozenPigment?): FrozenPigment? = null

    override fun produceParticles(particles: ParticleSpray?, colorizer: FrozenPigment?) {}

    override fun printMessage(message: Component?) {}
}