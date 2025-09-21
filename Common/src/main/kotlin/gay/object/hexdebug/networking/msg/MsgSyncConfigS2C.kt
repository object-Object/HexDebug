package gay.`object`.hexdebug.networking.msg

import gay.`object`.hexdebug.config.HexDebugServerConfig
import net.minecraft.network.FriendlyByteBuf

data class MsgSyncConfigS2C(val serverConfig: HexDebugServerConfig.ServerConfig) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanion<MsgSyncConfigS2C> {
        override val type = MsgSyncConfigS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSyncConfigS2C(
            serverConfig = HexDebugServerConfig.ServerConfig().also { it.decode(buf) },
        )

        override fun MsgSyncConfigS2C.encode(buf: FriendlyByteBuf) {
            serverConfig.encode(buf)
        }
    }
}
