package gay.`object`.hexdebug.gui.splicing.renderers
import at.petrak.hexcasting.api.casting.iota.IotaType
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRenderer
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererParser
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererProvider
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRenderers
import gay.`object`.hexdebug.api.splicing.SplicingTableIotaClientView
import gay.`object`.hexdebug.utils.getAsNbtPath
import gay.`object`.hexdebug.utils.getOrNull
import net.minecraft.commands.arguments.NbtPathArgument.NbtPath
import net.minecraft.nbt.CompoundTag

class SubIotaRendererProvider(private val path: NbtPath) : SplicingTableIotaRendererProvider {
    override fun createRenderer(
        type: IotaType<*>,
        iota: SplicingTableIotaClientView,
        x: Int,
        y: Int
    ): SplicingTableIotaRenderer? {
        val data = iota.data ?: return null
        val subIotaTag = path.getOrNull(data)?.first() as? CompoundTag ?: return null
        val subIotaType = IotaType.getTypeFromTag(subIotaTag) ?: return null
        val provider = SplicingTableIotaRenderers.getProvider(subIotaType) ?: return null
        val subIota = SplicingTableIotaClientView.subIota(subIotaTag)
        return provider.createRenderer(subIotaType, subIota, x, y)
    }

    companion object {
        val PARSER = SplicingTableIotaRendererParser<SubIotaRendererProvider> { _, jsonObject, _ ->
            SubIotaRendererProvider(path = jsonObject.getAsNbtPath("path"))
        }
    }
}
