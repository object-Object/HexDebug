package gay.`object`.hexdebug.registry

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.lib.hex.HexActions
import dev.architectury.registry.registries.RegistrySupplier
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.casting.actions.OpBreakpoint
import net.minecraft.resources.ResourceLocation

object HexDebugActions : HexDebugRegistry<ActionRegistryEntry>(HexActions.REGISTRY) {
    private val ACTIONS = mutableMapOf<ResourceLocation, ActionRegistryEntry>()

    val BREAKPOINT_BEFORE by make("breakpoint/before", HexDir.SOUTH_WEST, "awqdeew", OpBreakpoint(OpBreakpoint.Type.BEFORE))
    val BREAKPOINT_AFTER by make("breakpoint/after", HexDir.EAST, "wqqaewd", OpBreakpoint(OpBreakpoint.Type.AFTER))

    private fun make(
        name: String,
        startDir: HexDir,
        signature: String,
        action: Action,
    ): Lazy<RegistrySupplier<ActionRegistryEntry>> {
        val entry = ActionRegistryEntry(HexPattern.fromAngles(signature, startDir), action)
        if (ACTIONS.put(HexDebug.id(name), entry) != null) throw IllegalArgumentException("Duplicate ID: $name")
        return register(name) { entry }
    }
}