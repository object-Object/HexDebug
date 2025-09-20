package gay.`object`.hexdebug.config

import at.petrak.hexcasting.api.misc.MediaConstants
import dev.architectury.event.events.client.ClientPlayerEvent
import dev.architectury.event.events.common.PlayerEvent
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.networking.msg.MsgSyncConfigS2C
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.ConfigHolder
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject
import me.shedaniel.autoconfig.serializer.PartitioningSerializer
import me.shedaniel.autoconfig.serializer.PartitioningSerializer.GlobalData
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer
import net.minecraft.network.FriendlyByteBuf

// we can't use a companion object because GlobalData sees the field and throws an error :/

object HexDebugConfig {
    @JvmStatic
    lateinit var holder: ConfigHolder<GlobalConfig>

    @JvmStatic
    val client get() = holder.config.client

    @JvmStatic
    val server get() = syncedServerConfig ?: holder.config.server

    // only used on the client, probably
    private var syncedServerConfig: ServerConfig? = null

    fun init() {
        holder = AutoConfig.register(
            GlobalConfig::class.java,
            PartitioningSerializer.wrap(::Toml4jConfigSerializer),
        )

        PlayerEvent.PLAYER_JOIN.register { player ->
            MsgSyncConfigS2C(holder.config.server).sendToPlayer(player)
        }
    }

    fun initClient() {
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register { _ ->
            syncedServerConfig = null
        }
    }

    fun onSyncConfig(serverConfig: ServerConfig) {
        syncedServerConfig = serverConfig
    }

    @Config(name = HexDebug.MODID)
    class GlobalConfig : GlobalData() {
        @Category("client")
        @TransitiveObject
        val client = ClientConfig()

        @Category("server")
        @TransitiveObject
        val server = ServerConfig()
    }

    @Config(name = "client")
    class ClientConfig : ConfigData {
        @Tooltip
        val openDebugPort: Boolean = true

        @Tooltip
        val debugPort: Int = 4444

        @Tooltip
        val smartDebuggerSneakScroll: Boolean = true

        @Tooltip
        val debuggerDisplayMode: DebuggerDisplayMode = DebuggerDisplayMode.ENABLED

        @Tooltip
        val showDebugClientLineNumber: Boolean = false

        @Tooltip
        val invertSplicingTableScrollDirection: Boolean = false
    }

    @Config(name = "server")
    class ServerConfig : ConfigData {
        @Tooltip
        var maxUndoStackSize: Int = 64
            private set

        @Tooltip
        var splicingTableMediaCost: Long = MediaConstants.DUST_UNIT / 10
            private set

        @Tooltip
        var splicingTableMaxMedia: Long = MediaConstants.CRYSTAL_UNIT
            private set

        @Tooltip
        var splicingTableCastingCooldown: Int = 5
            private set

        @Tooltip
        var splicingTableAmbit: Double = 4.0
            private set

        fun encode(buf: FriendlyByteBuf) {
            buf.writeInt(maxUndoStackSize)
            buf.writeLong(splicingTableMediaCost)
            buf.writeLong(splicingTableMaxMedia)
            buf.writeInt(splicingTableCastingCooldown)
            buf.writeDouble(splicingTableAmbit)
        }

        companion object {
            fun decode(buf: FriendlyByteBuf) = ServerConfig().apply {
                maxUndoStackSize = buf.readInt()
                splicingTableMediaCost = buf.readLong()
                splicingTableMaxMedia = buf.readLong()
                splicingTableCastingCooldown = buf.readInt()
                splicingTableAmbit = buf.readDouble()
            }
        }
    }
}

enum class DebuggerDisplayMode {
    DISABLED,
    NOT_CONNECTED,
    ENABLED,
}
