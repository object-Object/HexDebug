package gay.`object`.hexdebug.casting.actions.splicing

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import at.petrak.hexcasting.api.casting.orNull
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity

object OpReadSelection : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val pos = args.getBlockPos(0, argc)
        env.assertPosInRange(pos)

        val table = env.world.getBlockEntity(pos) as? SplicingTableBlockEntity
            ?: throw MishapBadBlock.of(pos, "splicing_table")

        return listOf(
            table.selection?.start.doubleOrNull(),
            table.selection?.end?.plus(1).doubleOrNull(), // return the exclusive end index
        )
    }
}

private fun Int?.doubleOrNull() = this?.let { DoubleIota(it.toDouble()) }.orNull()
