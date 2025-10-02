package gay.`object`.hexdebug.gui.splicing.renderers.conditional

import at.petrak.hexcasting.api.casting.iota.IotaType
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRenderer
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererParser
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererProvider
import gay.`object`.hexdebug.api.splicing.SplicingTableIotaClientView
import gay.`object`.hexdebug.utils.getAsIotaRendererProvider
import gay.`object`.hexdebug.utils.getAsNbtPath
import net.minecraft.commands.arguments.NbtPathArgument.NbtPath
import net.minecraft.util.GsonHelper

class IfPathExistsRendererProvider(
    private val path: NbtPath,
    private val providerIf: SplicingTableIotaRendererProvider,
    private val providerElse: SplicingTableIotaRendererProvider,
    private val allowInSubIota: Boolean,
) : SplicingTableIotaRendererProvider {
    override fun createRenderer(
        type: IotaType<*>,
        iota: SplicingTableIotaClientView,
        x: Int,
        y: Int
    ): SplicingTableIotaRenderer? {
        val condition = (allowInSubIota || !iota.isSubIota)
            && iota.data?.let { path.countMatching(it) > 0 } == true
        val provider = if (condition) providerIf else providerElse
        return provider.createRenderer(type, iota, x, y)
    }

    companion object {
        val PARSER = SplicingTableIotaRendererParser<IfPathExistsRendererProvider> { _, jsonObject, parent ->
            IfPathExistsRendererProvider(
                path = jsonObject.getAsNbtPath("path", parent?.path),
                providerIf = jsonObject.getAsIotaRendererProvider("if", parent?.providerIf),
                providerElse = jsonObject.getAsIotaRendererProvider("else", parent?.providerElse),
                allowInSubIota = GsonHelper.getAsBoolean(jsonObject, "allow_in_sub_iota", parent?.allowInSubIota ?: false)
            )
        }
    }
}
