package gay.`object`.hexdebug.registry

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.common.casting.actions.spells.OpMakePackagedSpell
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexActions
import gay.`object`.hexdebug.casting.actions.OpBreakpoint
import gay.`object`.hexdebug.casting.actions.OpIsDebugging
import gay.`object`.hexdebug.casting.actions.OpNextEvalIndex
import gay.`object`.hexdebug.casting.actions.splicing.*
import gay.`object`.hexdebug.casting.iotas.CognitohazardIota

@Suppress("unused")
object HexDebugActions : HexDebugRegistrar<ActionRegistryEntry>(HexRegistries.ACTION, { HexActions.REGISTRY }) {
    val COGNITOHAZARD = make("const/cognitohazard", HexDir.NORTH_WEST, "wdeaqqdqeedqadqeedqaeadeaqqeadeaqqdqdeaqqeaeedqaw") {
        Action.makeConstantOp(CognitohazardIota())
    }
    val IS_DEBUGGING = make("const/debugging", HexDir.EAST, "qqqqqewaa", OpIsDebugging)
    val NEXT_EVAL_INDEX = make("next_eval_index", HexDir.SOUTH_WEST, "dedqdeqwaa", OpNextEvalIndex)

    val BREAKPOINT_BEFORE = make("breakpoint/before", HexDir.SOUTH_WEST, "awqdeew", OpBreakpoint(true))
    val BREAKPOINT_AFTER = make("breakpoint/after", HexDir.EAST, "wqqaewd", OpBreakpoint(false))

    // kotlin, why do i have to cast this??
    val CRAFT_DEBUGGER = make("craft/debugger", HexDir.SOUTH_WEST, "aaewwwwwaqwawqwadawqwwwawwwqwwwaw") {
        OpMakePackagedSpell(HexDebugItems.DEBUGGER.value as ItemPackagedHex, 10 * MediaConstants.CRYSTAL_UNIT)
    }

    // splicing table

    val READ_SELECTION = make("splicing/selection/read", HexDir.NORTH_WEST, "wqaeaqweeeedq", OpReadSelection)
    val WRITE_SELECTION = make("splicing/selection/write", HexDir.SOUTH_WEST, "wedqdewqqqqae", OpWriteSelection)

    val READ_VIEW_INDEX = make("splicing/view_index/read", HexDir.NORTH_WEST, "wqaeaqwdwaqaw", OpReadViewIndex)
    val WRITE_VIEW_INDEX = make("splicing/view_index/write", HexDir.SOUTH_WEST, "wedqdewawdedw", OpWriteViewIndex)

    val READ_LIST_SPELLBOOK_INDEX = make("splicing/list/spellbook_index/read", HexDir.NORTH_WEST, "wqaeaqwedqddq", OpReadSpellbookIndex(true))
    val WRITE_LIST_SPELLBOOK_INDEX = make("splicing/list/spellbook_index/write", HexDir.SOUTH_WEST, "wedqdewqaeaae", OpWriteSpellbookIndex(true))
    val READABLE_LIST_SPELLBOOK_INDEX = make("splicing/list/spellbook_index/readable", HexDir.NORTH_WEST, "wqaeaqwedqddqw", OpReadableSpellbookIndex(true))

    val READ_CLIPBOARD = make("splicing/clipboard/read", HexDir.NORTH_WEST, "wqaeaqweeeedw", OpReadClipboard)
    val WRITE_CLIPBOARD = make("splicing/clipboard/write", HexDir.SOUTH_WEST, "wedqdewqqqqaw", OpWriteClipboard)
    val READABLE_CLIPBOARD = make("splicing/clipboard/readable", HexDir.NORTH_WEST, "wqaeaqweeeedww", OpReadableClipboard)
    val WRITABLE_CLIPBOARD = make("splicing/clipboard/writable", HexDir.SOUTH_WEST, "wedqdewqqqqaww", OpWritableClipboard)

    val READ_CLIPBOARD_SPELLBOOK_INDEX = make("splicing/clipboard/spellbook_index/read", HexDir.NORTH_WEST, "wqaeaqwdeaaea", OpReadSpellbookIndex(false))
    val WRITE_CLIPBOARD_SPELLBOOK_INDEX = make("splicing/clipboard/spellbook_index/write", HexDir.SOUTH_WEST, "wedqdewaqddqd", OpWriteSpellbookIndex(false))
    val READABLE_CLIPBOARD_SPELLBOOK_INDEX = make("splicing/clipboard/spellbook_index/readable", HexDir.NORTH_WEST, "wqaeaqwdeaaeae", OpReadableSpellbookIndex(false))

    val READ_ENLIGHTENED_HEX = make("splicing/enlightened/hex/read", HexDir.NORTH_WEST, "wqaeaqwqqqwqwqqwq", OpReadEnlightenedHex)
    val WRITE_ENLIGHTENED_HEX = make("splicing/enlightened/hex/write", HexDir.NORTH_WEST, "wqaeaqwqqqwqwqqwwqqeaeqqeqqeaeqqw", OpWriteEnlightenedHex)

    private fun make(name: String, startDir: HexDir, signature: String, action: Action) =
        make(name, startDir, signature) { action }

    private fun make(name: String, startDir: HexDir, signature: String, getAction: () -> Action) = register(name) {
        ActionRegistryEntry(HexPattern.fromAngles(signature, startDir), getAction())
    }
}
