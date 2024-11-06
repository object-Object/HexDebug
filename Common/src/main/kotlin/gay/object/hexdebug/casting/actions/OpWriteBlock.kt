package gay.`object`.hexdebug.casting.actions

import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import at.petrak.hexcasting.api.casting.mishaps.MishapOthersName
import gay.`object`.hexdebug.utils.findDataHolder
import net.minecraft.world.phys.Vec3

// temporary implementation of FallingColors/HexMod#412 since there's no 1.20 gloop
object OpWriteBlock : SpellAction {
    override val argc = 2

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment,
    ): SpellAction.Result {
        val pos = args.getBlockPos(0, argc)
        val datum = args[1]

        env.assertPosInRange(pos)

        val datumHolder = findDataHolder(env.world, pos)
            ?: throw MishapBadBlock.of(pos, "iota.write")

        if (!datumHolder.writeIota(datum, true))
            throw MishapBadBlock.of(pos, "iota.write")

        val trueName = MishapOthersName.getTrueNameFromDatum(datum, null)
        if (trueName != null)
            throw MishapOthersName(trueName)

        return SpellAction.Result(
            Spell(datum, datumHolder),
            0,
            listOf(ParticleSpray(pos.center, Vec3(1.0, 0.0, 0.0), 0.25, 3.14, 40))
        )
    }

    private data class Spell(val datum: Iota, val datumHolder: ADIotaHolder) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            datumHolder.writeIota(datum, false)
        }
    }
}
