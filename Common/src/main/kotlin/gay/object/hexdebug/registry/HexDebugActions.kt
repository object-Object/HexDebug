package gay.`object`.hexdebug.registry

import at.petrak.hexcasting.api.PatternRegistry
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.spell.Action
import at.petrak.hexcasting.api.spell.iota.PatternIota
import at.petrak.hexcasting.api.spell.math.HexDir
import at.petrak.hexcasting.api.spell.math.HexPattern
import at.petrak.hexcasting.common.casting.operators.spells.OpMakePackagedSpell
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.casting.actions.OpBreakpoint
import gay.`object`.hexdebug.casting.actions.OpIsDebugging
import net.minecraft.resources.ResourceLocation

object HexDebugActions {
    val IS_DEBUGGING = make("const/debugging", HexDir.EAST, "qqqqqewaa", OpIsDebugging)

    val BREAKPOINT_BEFORE = make("breakpoint/before", HexDir.SOUTH_WEST, "awqdeew", OpBreakpoint(true))
    val BREAKPOINT_AFTER = make("breakpoint/after", HexDir.EAST, "wqqaewd", OpBreakpoint(false))

    // kotlin, why do i have to cast this??
    val CRAFT_DEBUGGER = make("craft/debugger", HexDir.SOUTH_WEST, "aaewwwwwaqwawqwadawqwwwawwwqwwwaw") {
        OpMakePackagedSpell(HexDebugItems.DEBUGGER.value as ItemPackagedHex, 10 * MediaConstants.CRYSTAL_UNIT)
    }

    // registry stuff

    private val patterns = mutableMapOf<ResourceLocation, Pair<HexPattern, () -> Action>>()

    private fun make(name: String, startDir: HexDir, signature: String, action: Action) =
        make(name, startDir, signature) { action }

    private fun make(name: String, startDir: HexDir, signature: String, getAction: () -> Action): PatternIota {
        val pattern = HexPattern.fromAngles(signature, startDir)
        patterns[HexDebug.id(name)] = Pair(pattern, getAction)
        return PatternIota(pattern)
    }

    fun init() {
        for ((location, value) in patterns) {
            val (pattern, getAction) = value
            PatternRegistry.mapPattern(pattern, location, getAction())
        }
    }
}
