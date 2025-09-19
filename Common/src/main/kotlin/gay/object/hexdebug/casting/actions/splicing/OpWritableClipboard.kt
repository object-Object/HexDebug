package gay.`object`.hexdebug.casting.actions.splicing

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity

object OpWritableClipboard : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val pos = args.getBlockPos(0, OpReadSelection.argc)
        env.assertPosInRange(pos)

        val table = env.world.getBlockEntity(pos) as? SplicingTableBlockEntity
            ?: return false.asActionResult

        val clipboardHolder = table.clipboardHolder
            ?: return false.asActionResult

        return clipboardHolder.writeable().asActionResult
    }
}
