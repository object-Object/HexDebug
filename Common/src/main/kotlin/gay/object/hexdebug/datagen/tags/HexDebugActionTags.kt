package gay.`object`.hexdebug.datagen.tags

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.lib.HexRegistries
import gay.`object`.hexdebug.registry.HexDebugActions
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.TagsProvider
import java.util.concurrent.CompletableFuture

class HexDebugActionTags(output: PackOutput, provider: CompletableFuture<HolderLookup.Provider>)
    : TagsProvider<ActionRegistryEntry>(output, HexRegistries.ACTION, provider)
{
    override fun addTags(provider: HolderLookup.Provider) {
        // regular pattern, but requires enlightenment
        tag(HexTags.Actions.REQUIRES_ENLIGHTENMENT).add(
            HexDebugActions.READ_ENLIGHTENED_HEX.key,
            HexDebugActions.WRITE_ENLIGHTENED_HEX.key,
        )
    }
}
