package gay.`object`.hexdebug.casting.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import gay.`object`.hexdebug.utils.findDataHolder

// temporary implementation of FallingColors/HexMod#412 since there's no 1.20 gloop
object OpReadBlock : ConstMediaAction {
    override val argc = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment,
    ): List<Iota> {
        val pos = args.getBlockPos(0, argc)

        env.assertPosInRange(pos)

        val datumHolder = findDataHolder(env.world, pos)
            ?: throw MishapBadBlock.of(pos, "iota.read")

        val datum = datumHolder.readIota(env.world)
            ?: datumHolder.emptyIota()
            ?: throw MishapBadBlock.of(pos, "iota.read")

        return listOf(datum)
    }
}
