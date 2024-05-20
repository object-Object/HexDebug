package gay.`object`.hexdebug.forge.datagen

import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.lib.HexBlocks
import at.petrak.hexcasting.common.lib.HexItems
import at.petrak.paucal.api.datagen.PaucalRecipeProvider
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.registry.HexDebugItems
import net.minecraft.data.DataGenerator
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.world.level.ItemLike
import java.util.function.Consumer

class HexDebugRecipes(gen: DataGenerator) : PaucalRecipeProvider(gen, HexDebug.MODID) {
    override fun makeRecipes(writer: Consumer<FinishedRecipe>) {
        flyswatter(HexDebugItems.DEBUGGER.value, Items.GOLD_INGOT, HexItems.ARTIFACT)
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES))
            .save(writer)

        flyswatter(HexDebugItems.EVALUATOR.value, HexBlocks.SLATE_BLOCK)
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES))
            .save(writer)
    }

    @Suppress("SameParameterValue")
    private fun flyswatter(result: ItemLike, handle: ItemLike) = flyswatter(result, handle, handle)

    // thwack!
    private fun flyswatter(result: ItemLike, lowerHandle: ItemLike, upperHandle: ItemLike) =
        ShapedRecipeBuilder.shaped(result)
            .define('L', lowerHandle)
            .define('U', upperHandle)
            .define('C', HexItems.CHARGED_AMETHYST)
            .pattern(" CC")
            .pattern(" UC")
            .pattern("L  ")
}
