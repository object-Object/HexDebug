package gay.`object`.hexdebug.casting.actions.splicing

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import at.petrak.hexcasting.common.items.storage.ItemSpellbook
import at.petrak.hexcasting.common.lib.HexItems
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity

class OpReadSpellbookIndex(private val useListItem: Boolean) : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val pos = args.getBlockPos(0, argc)
        env.assertPosInRange(pos)

        val table = env.world.getBlockEntity(pos) as? SplicingTableBlockEntity
            ?: throw MishapBadBlock.of(pos, "splicing_table")

        val stack = if (useListItem) table.listStack else table.clipboardStack
        if (!stack.`is`(HexItems.SPELLBOOK) || ItemSpellbook.arePagesEmpty(stack)) {
            throw MishapBadBlock.of(pos, "splicing_table.${if (useListItem) "list" else "clipboard"}.spellbook")
        }

        return ItemSpellbook.getPage(stack, 1).asActionResult
    }
}
