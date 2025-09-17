package gay.`object`.hexdebug.recipes

import gay.`object`.hexdebug.items.FocusHolderBlockItem.Companion.getIotaStack
import gay.`object`.hexdebug.items.FocusHolderBlockItem.Companion.hasIotaStack
import gay.`object`.hexdebug.items.FocusHolderBlockItem.Companion.setIotaStack
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.registry.HexDebugRecipeSerializers
import net.minecraft.core.NonNullList
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.level.Level

class FocusHolderEmptyingRecipe(id: ResourceLocation, category: CraftingBookCategory) : CustomRecipe(id, category) {
    override fun matches(container: CraftingContainer, level: Level): Boolean {
        return findItem(container) != null
    }

    override fun assemble(container: CraftingContainer, registryAccess: RegistryAccess): ItemStack {
        val holder = findItem(container) ?: return ItemStack.EMPTY
        return holder.getIotaStack().first.copy()
    }

    override fun getRemainingItems(container: CraftingContainer): NonNullList<ItemStack> {
        val remaining = NonNullList.withSize(container.containerSize, ItemStack.EMPTY)
        for (i in 0..remaining.size) {
            val itemStack = container.getItem(i)
            if (itemStack.`is`(HexDebugBlocks.FOCUS_HOLDER.item)) {
                remaining[i] = itemStack.copy().apply { setIotaStack(ItemStack.EMPTY) }
            }
        }
        return remaining
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean {
        return width * height >= 1
    }

    override fun getSerializer() = HexDebugRecipeSerializers.FOCUS_HOLDER_EMPTYING.value

    private fun findItem(container: CraftingContainer): ItemStack? {
        var holder: ItemStack? = null
        for (itemStack in container.items) {
            when (itemStack.item) {
                HexDebugBlocks.FOCUS_HOLDER.item -> {
                    if (holder != null || !itemStack.hasIotaStack || itemStack.count > 1) return null
                    holder = itemStack
                }
                Items.AIR -> {}
                else -> return null
            }
        }
        return holder
    }
}
