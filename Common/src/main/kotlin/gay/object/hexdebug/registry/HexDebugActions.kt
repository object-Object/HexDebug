package gay.`object`.hexdebug.registry

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexActions
import gay.`object`.hexdebug.casting.actions.OpBreakpoint
import gay.`object`.hexdebug.casting.actions.OpIsDebugging

object HexDebugActions : HexDebugRegistrar<ActionRegistryEntry>(HexRegistries.ACTION, { HexActions.REGISTRY }) {
    val IS_DEBUGGING = make("const/debugging", HexDir.EAST, "qqqqqewaa", OpIsDebugging)

    val BREAKPOINT_BEFORE = make("breakpoint/before", HexDir.SOUTH_WEST, "awqdeew", OpBreakpoint(true))
    val BREAKPOINT_AFTER = make("breakpoint/after", HexDir.EAST, "wqqaewd", OpBreakpoint(false))

    private fun make(name: String, startDir: HexDir, signature: String, action: Action) =
        register(name) { ActionRegistryEntry(HexPattern.fromAngles(signature, startDir), action) }
}
