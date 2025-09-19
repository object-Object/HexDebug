package gay.`object`.hexdebug.casting.actions.splicing

import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import at.petrak.hexcasting.api.casting.mishaps.MishapOthersName
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity
import net.minecraft.world.phys.Vec3

object OpWriteClipboard : SpellAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val pos = args.getBlockPos(0, argc)
        val datum = args[1]

        env.assertPosInRangeForEditing(pos)

        val table = env.world.getBlockEntity(pos) as? SplicingTableBlockEntity
            ?: throw MishapBadBlock.of(pos, "splicing_table")

        val clipboardHolder = table.clipboardHolder
            ?: throw MishapBadBlock.of(pos, "splicing_table.clipboard.write")

        if (!clipboardHolder.writeIota(datum, true)) {
            throw MishapBadBlock.of(pos, "splicing_table.clipboard.write")
        }

        MishapOthersName.getTrueNameFromDatum(datum, null)?.let {
            throw MishapOthersName(it)
        }

        return SpellAction.Result(
            Spell(table, clipboardHolder, datum),
            0,
            listOf(ParticleSpray(pos.center, Vec3(1.0, 0.0, 0.0), 0.25, 3.14, 40))
        )
    }

    private data class Spell(val table: SplicingTableBlockEntity, val clipboardHolder: ADIotaHolder, val datum: Iota) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            clipboardHolder.writeIota(datum, false)
            table.sync()
        }
    }
}
