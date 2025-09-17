package gay.`object`.hexdebug.recipes

import at.petrak.hexcasting.common.lib.HexItems
import gay.`object`.hexdebug.items.FocusHolderBlockItem
import gay.`object`.hexdebug.items.FocusHolderBlockItem.Companion.hasIotaStack
import gay.`object`.hexdebug.items.FocusHolderBlockItem.Companion.setIotaStack
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.registry.HexDebugRecipeSerializers
import net.minecraft.core.NonNullList
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.Level

class FocusHolderFillingShapelessRecipe(id: ResourceLocation, category: CraftingBookCategory) : CustomRecipe(id, category) {
    private val resultItem = FocusHolderBlockItem.withFocus()

    override fun matches(container: CraftingContainer, level: Level): Boolean {
        return findItems(container) != null
    }

    override fun assemble(container: CraftingContainer, registryAccess: RegistryAccess): ItemStack {
        val (holder, focus) = findItems(container) ?: return ItemStack.EMPTY
        return holder.copyWithCount(1).apply {
            setIotaStack(focus)
        }
    }

    override fun getIngredients(): NonNullList<Ingredient> = NonNullList.of(
        Ingredient.of(HexDebugBlocks.FOCUS_HOLDER.item),
        Ingredient.of(HexItems.FOCUS),
    )

    override fun getResultItem(registryAccess: RegistryAccess) = resultItem

    override fun canCraftInDimensions(width: Int, height: Int): Boolean {
        return width * height >= 2
    }

    override fun getSerializer() = HexDebugRecipeSerializers.FOCUS_HOLDER_FILLING_SHAPELESS.value

    private fun findItems(container: CraftingContainer): Pair<ItemStack, ItemStack>? {
        var holder: ItemStack? = null
        var focus: ItemStack? = null

        for (itemStack in container.items) {
            if (itemStack.isEmpty) continue

            when (itemStack.item) {
                HexDebugBlocks.FOCUS_HOLDER.item -> {
                    if (holder != null || itemStack.hasIotaStack) return null
                    holder = itemStack
                }
                HexItems.FOCUS -> {
                    if (focus != null) return null
                    focus = itemStack
                }
                else -> return null
            }
        }

        return Pair(
            holder ?: return null,
            focus ?: return null,
        )
    }
}
