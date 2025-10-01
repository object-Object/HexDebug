package gay.`object`.hexdebug.fabric.datagen

import gay.`object`.hexdebug.datagen.tags.HexDebugActionTags
import gay.`object`.hexdebug.datagen.tags.HexDebugBlockTags
import gay.`object`.hexdebug.datagen.tags.HexDebugItemTags
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

object FabricHexDebugDatagen : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(gen: FabricDataGenerator) {
        val pack = gen.createPack()

        pack.addProvider(::HexDebugActionTags)
        pack.addProvider(::HexDebugBlockTags)
        pack.addProvider(::HexDebugItemTags)
    }
}
