package gay.`object`.hexdebug.gui.splicing.renderers

import at.petrak.hexcasting.api.casting.iota.IotaType
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRenderer
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererParser
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererProvider
import gay.`object`.hexdebug.api.splicing.SplicingTableIotaClientView
import gay.`object`.hexdebug.utils.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.commands.arguments.NbtPathArgument.NbtPath
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.GsonHelper
import net.minecraft.world.item.ItemStack

class ItemRendererProvider(
    val itemPath: NbtPath,
    val blockPath: NbtPath?,
    val countPath: NbtPath?,
    val tagPath: NbtPath?,
    val xOffset: Float,
    val yOffset: Float,
    val scale: Float,
) : SplicingTableIotaRendererProvider {
    override fun createRenderer(
        type: IotaType<*>,
        iota: SplicingTableIotaClientView,
        x: Int,
        y: Int
    ): SplicingTableIotaRenderer? {
        val data = iota.data ?: return null

        val item = itemPath.getResourceLocationOrNull(data)?.let(BuiltInRegistries.ITEM::getOrNull)
            ?: blockPath?.getResourceLocationOrNull(data)?.let(BuiltInRegistries.BLOCK::getOrNull)
            ?: return null

        val count = countPath?.getIntOrNull(data) ?: 1

        val tag = tagPath?.getOrNull(data) as? CompoundTag

        val stack = ItemStack(item, count)
        stack.tag = tag

        return ItemRenderer(type, iota, x, y, stack)
    }

    inner class ItemRenderer(
        type: IotaType<*>,
        iota: SplicingTableIotaClientView,
        x: Int,
        y: Int,
        val stack: ItemStack,
    ) : SplicingTableIotaRenderer(type, iota, x, y) {
        override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            guiGraphics.pose().letPushPose { ps ->
                // align to center of iota display
                ps.translate(x + (18f / 2f) + xOffset, y + (21f / 2f) + yOffset, 0f)
                ps.scale(scale)
                // renderItem wants the top left corner, but it'll calculate it after the scale has been applied
                ps.translate(-8f, -8f, 0f)
                guiGraphics.renderItem(stack, 0, 0)
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, 0, 0)
            }
        }
    }

    companion object {
        val PARSER = SplicingTableIotaRendererParser<ItemRendererProvider> { _, json, parent ->
            ItemRendererProvider(
                itemPath = json.getAsNbtPath("item_path", parent?.itemPath),
                blockPath = json.getAsNbtPathOrNull("block_path", parent?.blockPath),
                countPath = json.getAsNbtPathOrNull("count_path", parent?.countPath),
                tagPath = json.getAsNbtPathOrNull("tag_path", parent?.tagPath),
                xOffset = GsonHelper.getAsFloat(json, "x_offset", 0f),
                yOffset = GsonHelper.getAsFloat(json, "y_offset", 0f),
                scale = GsonHelper.getAsFloat(json, "scale", 0.75f),
            )
        }
    }
}
