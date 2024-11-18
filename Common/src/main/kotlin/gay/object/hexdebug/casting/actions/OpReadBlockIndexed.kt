package gay.`object`.hexdebug.casting.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.getInt
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import at.petrak.hexcasting.api.utils.downcast
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import gay.`object`.hexdebug.utils.findDataHolder
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag

// temporary implementation of FallingColors/HexMod#412 since there's no 1.20 gloop
object OpReadBlockIndexed : ConstMediaAction {
    override val argc = 2

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment,
    ): List<Iota> {
        val pos = args.getBlockPos(0, argc)
        val index = args.getInt(1, argc)

        env.assertPosInRange(pos)

        val datumHolder = findDataHolder(env.world, pos)
            ?: throw MishapBadBlock.of(pos, "iota.read")

        val tag = datumHolder.readIotaTag()
            ?: throw MishapBadBlock.of(pos, "iota.read")

        if (tag.getString(HexIotaTypes.KEY_TYPE) != "hexcasting:list")
            throw MishapBadBlock.of(pos, "iota.read")

        val item = try {
            tag.get(HexIotaTypes.KEY_DATA)
                ?.downcast(ListTag.TYPE)
                ?.getOrNull(index)
                ?.downcast(CompoundTag.TYPE)
                ?: throw MishapBadBlock.of(pos, "iota.read")
        } catch (e: IllegalArgumentException) {
            throw MishapBadBlock.of(pos, "iota.read")
        }

        val datum = IotaType.deserialize(item, env.world)
        return listOf(datum)
    }
}
