package gay.`object`.hexdebug.recipes

import com.google.gson.JsonObject
import gay.`object`.hexdebug.registry.HexDebugItems
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

class FlyswatterQuenchingShapedRecipe(
    id: ResourceLocation,
    group: String,
    category: CraftingBookCategory,
    width: Int,
    height: Int,
    recipeItems: NonNullList<Ingredient>,
    result: ItemStack,
    showNotification: Boolean,
) : ShapedRecipe(id, group, category, width, height, recipeItems, result, showNotification) {
    override fun assemble(container: CraftingContainer, registryAccess: RegistryAccess): ItemStack {
        var original: ItemStack? = null
        for (stack in container.items) {
            if (stack.`is`(HexDebugItems.DEBUGGER.value) || stack.`is`(HexDebugItems.EVALUATOR.value)) {
                original = stack
                break
            }
        }

        return super.assemble(container, registryAccess).also {
            it.tag = original?.tag?.copy()
        }
    }

    override fun getSerializer() = HexDebugRecipeSerializers.FLYSWATTER_QUENCHING.value

    companion object {
        private fun fromShapedRecipe(recipe: ShapedRecipe): FlyswatterQuenchingShapedRecipe {
            return recipe.run {
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // lie
                FlyswatterQuenchingShapedRecipe(
                    id = id,
                    group = group,
                    category = category(),
                    width = width,
                    height = height,
                    recipeItems = ingredients,
                    result = getResultItem(null),
                    showNotification = showNotification(),
                )
            }
        }
    }

    class Serializer : ShapedRecipe.Serializer() {
        override fun fromJson(recipeId: ResourceLocation, json: JsonObject): ShapedRecipe {
            return fromShapedRecipe(super.fromJson(recipeId, json))
        }

        override fun fromNetwork(recipeId: ResourceLocation, buf: FriendlyByteBuf): ShapedRecipe {
            return fromShapedRecipe(super.fromNetwork(recipeId, buf))
        }
    }
}
