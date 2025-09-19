package gay.`object`.hexdebug.casting.actions.splicing

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.getPositiveInt
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity
import net.minecraft.world.phys.Vec3

object OpWriteViewIndex : SpellAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val pos = args.getBlockPos(0, argc)
        val index = args.getPositiveInt(1, argc)

        env.assertPosInRangeForEditing(pos)

        val table = env.world.getBlockEntity(pos) as? SplicingTableBlockEntity
            ?: throw MishapBadBlock.of(pos, "splicing_table")

        return SpellAction.Result(
            Spell(table, index),
            0,
            listOf(ParticleSpray(pos.center, Vec3(1.0, 0.0, 0.0), 0.25, 3.14, 40))
        )
    }

    private data class Spell(val table: SplicingTableBlockEntity, val index: Int) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            table.writeViewStartIndex(index)
            table.sync()
        }
    }
}
