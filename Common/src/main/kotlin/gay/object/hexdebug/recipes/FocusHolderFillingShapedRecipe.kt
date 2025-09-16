package gay.`object`.hexdebug.recipes

import at.petrak.hexcasting.common.lib.HexItems
import com.google.gson.JsonObject
import gay.`object`.hexdebug.items.FocusHolderBlockItem.Companion.hasIotaStack
import gay.`object`.hexdebug.items.FocusHolderBlockItem.Companion.putIotaStack
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.registry.HexDebugRecipeSerializers
import net.minecraft.core.NonNullList
import net.minecraft.core.RegistryAccess
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.level.Level

class FocusHolderFillingShapedRecipe(
    id: ResourceLocation,
    group: String,
    category: CraftingBookCategory,
    width: Int,
    height: Int,
    recipeItems: NonNullList<Ingredient>,
    result: ItemStack,
    showNotification: Boolean,
) : ShapedRecipe(id, group, category, width, height, recipeItems, result, showNotification) {
    override fun matches(container: CraftingContainer, level: Level): Boolean {
        if (!super.matches(container, level)) return false
        for (ingredient in container.items) {
            // don't allow filling a holder that's already filled
            if (ingredient.`is`(HexDebugBlocks.FOCUS_HOLDER.item) && ingredient.hasIotaStack) {
                return false
            }
        }
        return true
    }

    override fun assemble(container: CraftingContainer, registryAccess: RegistryAccess): ItemStack {
        val result = super.assemble(container, registryAccess)
        for (ingredient in container.items) {
            if (ingredient.`is`(HexItems.FOCUS)) {
                result.putIotaStack(ingredient)
                break
            }
        }
        return result
    }

    override fun getSerializer() = HexDebugRecipeSerializers.FOCUS_HOLDER_FILLING_SHAPED.value

    companion object {
        private fun fromShapedRecipe(recipe: ShapedRecipe): FocusHolderFillingShapedRecipe {
            return recipe.run {
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // it's fiiiiiiiiiiiine
                FocusHolderFillingShapedRecipe(
                    id = id,
                    group = group,
                    category = category(),
                    width = width,
                    height = height,
                    recipeItems = ingredients,
                    result = getResultItem(null).apply {
                        putIotaStack(ItemStack(HexItems.FOCUS))
                    },
                    showNotification = showNotification(),
                )
            }
        }
    }

    class Serializer : ShapedRecipe.Serializer() {
        override fun fromJson(recipeId: ResourceLocation, json: JsonObject): ShapedRecipe {
            return fromShapedRecipe(super.fromJson(recipeId, json))
        }

        override fun fromNetwork(recipeId: ResourceLocation, buffer: FriendlyByteBuf): ShapedRecipe {
            return fromShapedRecipe(super.fromNetwork(recipeId, buffer))
        }
    }
}
