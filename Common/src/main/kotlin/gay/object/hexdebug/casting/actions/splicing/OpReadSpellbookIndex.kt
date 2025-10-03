package gay.`object`.hexdebug.casting.actions.splicing

import at.petrak.hexcasting.api.block.HexBlockEntity
import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import at.petrak.hexcasting.common.items.storage.ItemSpellbook
import at.petrak.hexcasting.common.lib.HexItems
import gay.`object`.hexdebug.blocks.focusholder.FocusHolderBlockEntity
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack

class OpReadSpellbookIndex(private val useListItem: Boolean) : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val pos = args.getBlockPos(0, argc)
        env.assertPosInRange(pos)

        val (_, stack) = getSpellbook(env, pos, useListItem)

        return ItemSpellbook.getPage(stack, 1).asActionResult
    }

    companion object {
        fun getSpellbook(
            env: CastingEnvironment,
            pos: BlockPos,
            useListItem: Boolean,
        ): Pair<HexBlockEntity, ItemStack> {
            val hexBlockEntity = env.world.getBlockEntity(pos) as? HexBlockEntity

            val (stack, notSpellbookMishap) = when (hexBlockEntity) {
                is SplicingTableBlockEntity -> when (useListItem) {
                    true -> hexBlockEntity.listStack to "splicing_table_or_focus_holder.spellbook"
                    false -> hexBlockEntity.clipboardStack to "splicing_table.clipboard.spellbook"
                }
                is FocusHolderBlockEntity -> when (useListItem) {
                    true -> hexBlockEntity.iotaStack to "splicing_table_or_focus_holder.spellbook"
                    false -> throw MishapBadBlock.of(pos, "splicing_table")
                }
                else -> throw MishapBadBlock.of(pos, "splicing_table_or_focus_holder")
            }

            if (!stack.`is`(HexItems.SPELLBOOK) || ItemSpellbook.arePagesEmpty(stack)) {
                throw MishapBadBlock.of(pos, notSpellbookMishap)
            }

            return hexBlockEntity to stack
        }
    }
}
