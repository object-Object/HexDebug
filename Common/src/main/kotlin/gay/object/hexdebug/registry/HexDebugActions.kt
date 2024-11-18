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
import gay.`object`.hexdebug.casting.actions.*

object HexDebugActions : HexDebugRegistrar<ActionRegistryEntry>(HexRegistries.ACTION, { HexActions.REGISTRY }) {
    val IS_DEBUGGING = make("const/debugging", HexDir.EAST, "qqqqqewaa", OpIsDebugging)
    val NEXT_EVAL_INDEX = make("next_eval_index", HexDir.SOUTH_WEST, "dedqdeqwaa", OpNextEvalIndex)

    val BREAKPOINT_BEFORE = make("breakpoint/before", HexDir.SOUTH_WEST, "awqdeew", OpBreakpoint(true))
    val BREAKPOINT_AFTER = make("breakpoint/after", HexDir.EAST, "wqqaewd", OpBreakpoint(false))

    // kotlin, why do i have to cast this??
    val CRAFT_DEBUGGER = make("craft/debugger", HexDir.SOUTH_WEST, "aaewwwwwaqwawqwadawqwwwawwwqwwwaw") {
        OpMakePackagedSpell(HexDebugItems.DEBUGGER.value as ItemPackagedHex, 10 * MediaConstants.CRYSTAL_UNIT)
    }

    // temporary implementation of FallingColors/HexMod#412 since there's no 1.20 gloop
    val BLOCK_READ = make("block_read", HexDir.EAST, "aqqqqqeawqwaw", OpReadBlock)
    val BLOCK_WRITE = make("block_write", HexDir.EAST, "deeeeeqdwewewewdw", OpWriteBlock)
    val BLOCK_READ_INDEXED = make("block_read_indexed", HexDir.EAST, "aqqqqqedwewewewdw", OpReadBlockIndexed)
    val BLOCK_WRITE_INDEXED = make("block_write_indexed", HexDir.EAST, "deeeeeqawqwaw", OpWriteBlockIndexed)

    private fun make(name: String, startDir: HexDir, signature: String, action: Action) =
        make(name, startDir, signature) { action }

    private fun make(name: String, startDir: HexDir, signature: String, getAction: () -> Action) = register(name) {
        ActionRegistryEntry(HexPattern.fromAngles(signature, startDir), getAction())
    }
}
