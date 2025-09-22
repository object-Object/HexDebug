package gay.`object`.hexdebug.casting.eval

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.pigment.FrozenPigment
import at.petrak.hexcasting.api.utils.extractMedia
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity
import gay.`object`.hexdebug.config.HexDebugServerConfig
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import kotlin.math.min

class SplicingTableCastEnv(
    caster: ServerPlayer,
    private val table: SplicingTableBlockEntity,
) : PlayerBasedCastEnv(caster, InteractionHand.MAIN_HAND) {
    private var sound = HexEvalSounds.NOTHING

    val blockPos: BlockPos get() = table.blockPos

    val facing: Direction get() = table.blockState.getValue(BlockStateProperties.FACING)

    override fun postExecution(result: CastResult) {
        super.postExecution(result)
        sound = sound.greaterOf(result.sound)
    }

    override fun postCast(image: CastingImage) {
        super.postCast(image)
        sound.sound?.let {
            world.playSound(null, blockPos, it, SoundSource.PLAYERS, 1f, 1f)
        }
    }

    override fun mishapSprayPos(): Vec3 = blockPos.center

    override fun extractMediaEnvironment(cost: Long, simulate: Boolean): Long {
        var costLeft = cost

        // first, use the buffer
        if (table.media > 0) {
            val mediaToTake = min(costLeft, table.media)
            costLeft -= mediaToTake
            if (!simulate) {
                table.media -= mediaToTake
            }
        }

        // then, pull straight from the item in the media slot
        if (costLeft > 0) {
            table.mediaHolder?.let {
                costLeft -= extractMedia(it, cost = costLeft, simulate = simulate)
            }
        }

        return costLeft
    }

    override fun isVecInRangeEnvironment(vec: Vec3): Boolean {
        val sentinel = HexAPI.instance().getSentinel(caster)
        if (
            sentinel != null
            && sentinel.extendsRange()
            && caster.level().dimension() == sentinel.dimension()
            && isVecInRadius(vec, sentinel.position, SENTINEL_RADIUS)
        ) {
            return true
        }

        return isVecInRadius(vec, blockPos.center, HexDebugServerConfig.config.splicingTableAmbit)
    }

    override fun getCastingHand(): InteractionHand = castingHand

    override fun getPigment(): FrozenPigment {
        return table.pigment ?: HexAPI.instance().getColorizer(caster)
    }

    override fun setPigment(pigment: FrozenPigment?): FrozenPigment? {
        table.pigment = pigment
        return pigment
    }

    companion object {
        private fun isVecInRadius(a: Vec3, b: Vec3, radius: Double) =
            a.distanceToSqr(b) <= radius * radius + 0.00000000001
    }
}
