package gay.`object`.hexdebug.forge.datagen

import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.lib.HexItems
import at.petrak.paucal.api.datagen.PaucalRecipeProvider
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.registry.HexDebugItems
import net.minecraft.data.DataGenerator
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import java.util.function.Consumer

class HexDebugRecipes(gen: DataGenerator) : PaucalRecipeProvider(gen, HexDebug.MODID) {
    override fun makeRecipes(writer: Consumer<FinishedRecipe>) {
        ShapedRecipeBuilder.shaped(HexDebugItems.DEBUGGER.value)
            .define('G', Items.GOLD_INGOT)
            .define('A', HexItems.ARTIFACT)
            .define('C', HexItems.CHARGED_AMETHYST)
            .pattern(" CC")
            .pattern(" AC")
            .pattern("G  ")
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES))
            .save(writer)
    }
}
