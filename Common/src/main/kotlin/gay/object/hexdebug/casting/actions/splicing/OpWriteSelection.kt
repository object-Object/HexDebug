package gay.`object`.hexdebug.casting.actions.splicing

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.utils.getPositiveIntOrNull
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min

object OpWriteSelection : SpellAction {
    override val argc = 3

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val pos = args.getBlockPos(0, argc)
        val from = args.getPositiveIntOrNull(1, argc)
        val to = args.getPositiveIntOrNull(2, argc)

        env.assertPosInRangeForEditing(pos)

        val table = env.world.getBlockEntity(pos) as? SplicingTableBlockEntity
            ?: throw MishapBadBlock.of(pos, "splicing_table")

        // both null: remove selection
        // to null: select edge
        // neither null: select range (convert end index to inclusive)
        val selection = when {
            from == null -> null
            to == null -> Selection.edge(from)
            else -> Selection.range(min(from, to), max(from, to) - 1)
        }

        return SpellAction.Result(
            Spell(table, selection),
            0,
            listOf(ParticleSpray(pos.center, Vec3(1.0, 0.0, 0.0), 0.25, 3.14, 40))
        )
    }

    private data class Spell(val table: SplicingTableBlockEntity, val selection: Selection?) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            table.writeSelection(selection)
        }
    }
}