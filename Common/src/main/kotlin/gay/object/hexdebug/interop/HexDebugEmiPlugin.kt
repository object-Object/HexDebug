package gay.`object`.hexdebug.interop

import at.petrak.hexcasting.common.lib.HexItems
import dev.emi.emi.api.EmiRegistry
import dev.emi.emi.api.recipe.EmiCraftingRecipe
import dev.emi.emi.api.stack.Comparison
import dev.emi.emi.api.stack.EmiStack
import dev.emi.emi.api.widget.Bounds
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import gay.`object`.hexdebug.items.FocusHolderBlockItem
import gay.`object`.hexdebug.items.FocusHolderBlockItem.Companion.hasIotaStack
import gay.`object`.hexdebug.recipes.FocusHolderEmptyingRecipe
import gay.`object`.hexdebug.recipes.FocusHolderFillingShapelessRecipe
import gay.`object`.hexdebug.registry.HexDebugBlocks
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.RecipeType

object HexDebugEmiPlugin {
    fun register(registry: EmiRegistry) {
        // if a recipe calls for an empty focus holder, disallow filled, and vice versa
        registry.setDefaultComparison(
            HexDebugBlocks.FOCUS_HOLDER.item,
            Comparison.compareData { it.itemStack.hasIotaStack },
        )

        // show filled focus holder in sidebar
        // necessary because otherwise there's no good way to see the recipe for a new holder with existing focus
        registry.addEmiStack(EmiStack.of(FocusHolderBlockItem.withFocus()))

        // better recipes for focus holder filling/emptying
        for (recipe in registry.recipeManager.getAllRecipesFor(RecipeType.CRAFTING)) {
            val emiRecipe = when (recipe) {
                is FocusHolderEmptyingRecipe -> EmiCraftingRecipe(
                    listOf(
                        EmiStack.of(FocusHolderBlockItem.withFocus())
                            .setRemainder(EmiStack.of(HexDebugBlocks.FOCUS_HOLDER.item)),
                    ),
                    EmiStack.of(HexItems.FOCUS),
                    synthetic(recipe.id),
                    true,
                )

                is FocusHolderFillingShapelessRecipe -> EmiCraftingRecipe(
                    listOf(
                        EmiStack.of(HexDebugBlocks.FOCUS_HOLDER.item),
                        EmiStack.of(HexItems.FOCUS),
                    ),
                    EmiStack.of(FocusHolderBlockItem.withFocus()),
                    synthetic(recipe.id),
                    true,
                )

                else -> continue
            }
            registry.removeRecipes(recipe.id)
            registry.addRecipe(emiRecipe)
        }

        // screen exclusions for splicing table
        registry.addExclusionArea(SplicingTableScreen::class.java) { screen, consumer ->
            screen.apply {
                consumer.accept(Bounds(
                    exportButtonX,
                    exportButtonY,
                    exportButtonWidth,
                    exportButtonHeight,
                ))
                consumer.accept(Bounds(
                    storageMinX,
                    storageMinY,
                    storageMaxX - storageMinX,
                    storageMaxY - storageMinY,
                ))
                consumer.accept(Bounds(
                    staffSlotMinX,
                    staffSlotMinY,
                    staffSlotMaxX - staffSlotMinX,
                    staffSlotMaxY - staffSlotMinY,
                ))
                if (hasStaffItem) {
                    consumer.accept(Bounds(
                        staffMinX,
                        staffMinY,
                        staffMaxX - staffMinX,
                        staffMaxY - staffMinY,
                    ))
                }
            }
        }
    }

    private fun synthetic(id: ResourceLocation) = id.withPrefix("/")
}
