package gay.`object`.hexdebug.interop

import at.petrak.hexcasting.common.lib.HexItems
import dev.emi.emi.api.EmiRegistry
import dev.emi.emi.api.stack.Comparison
import dev.emi.emi.api.stack.EmiStack
import dev.emi.emi.api.widget.Bounds
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import gay.`object`.hexdebug.items.FocusHolderBlockItem.Companion.getIotaStack
import gay.`object`.hexdebug.items.FocusHolderBlockItem.Companion.setIotaStack
import gay.`object`.hexdebug.registry.HexDebugBlocks
import net.minecraft.world.item.ItemStack

object HexDebugEmiPlugin {
    fun register(registry: EmiRegistry) {
        // if a recipe calls for an empty focus holder, disallow filled, and vice versa
        registry.setDefaultComparison(
            HexDebugBlocks.FOCUS_HOLDER.item,
            Comparison.compareData { it.itemStack.getIotaStack().first.item },
        )

        // show filled focus holder in sidebar
        registry.addEmiStack(EmiStack.of(
            ItemStack(HexDebugBlocks.FOCUS_HOLDER)
                .setIotaStack(ItemStack(HexItems.FOCUS))
        ))

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
                    castButtonX,
                    castButtonY,
                    castButtonWidth,
                    castButtonHeight,
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
}
