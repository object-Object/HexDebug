package gay.`object`.hexdebug.registry

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import gay.`object`.hexdebug.casting.iotas.CognitohazardIota

object HexDebugIotaTypes : HexDebugRegistrar<IotaType<*>>(
    HexRegistries.IOTA_TYPE,
    { HexIotaTypes.REGISTRY },
) {
    val COGNITOHAZARD = register("cognitohazard") { CognitohazardIota.TYPE }
}
