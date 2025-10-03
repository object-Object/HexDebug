package gay.`object`.hexdebug.casting.actions.splicing

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.Mishap

class OpReadableSpellbookIndex(private val useListItem: Boolean) : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val pos = args.getBlockPos(0, argc)
        env.assertPosInRange(pos)

        return try {
            OpReadSpellbookIndex.getSpellbook(env, pos, useListItem)
            true
        } catch (e: Mishap) {
            false
        }.asActionResult
    }
}
