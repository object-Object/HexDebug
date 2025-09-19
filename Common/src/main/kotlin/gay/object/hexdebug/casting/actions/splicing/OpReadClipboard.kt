package gay.`object`.hexdebug.casting.actions.splicing

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity

object OpReadClipboard : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val pos = args.getBlockPos(0, argc)
        env.assertPosInRange(pos)

        val table = env.world.getBlockEntity(pos) as? SplicingTableBlockEntity
            ?: throw MishapBadBlock.of(pos, "splicing_table")

        val clipboardHolder = table.clipboardHolder
            ?: throw MishapBadBlock.of(pos, "splicing_table.clipboard.read")

        val datum = clipboardHolder.readIota(env.world)
            ?: clipboardHolder.emptyIota()
            ?: throw MishapBadBlock.of(pos, "splicing_table.clipboard.read")

        return listOf(datum)
    }
}
