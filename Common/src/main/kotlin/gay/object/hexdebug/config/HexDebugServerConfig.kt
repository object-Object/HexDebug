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
import me.shedaniel.autoconfig.annotation.ConfigEntry.BoundedDiscrete
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject
import me.shedaniel.autoconfig.serializer.PartitioningSerializer
import me.shedaniel.autoconfig.serializer.PartitioningSerializer.GlobalData
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.InteractionResult

// we can't use a companion object because GlobalData sees the field and throws an error :/

object HexDebugServerConfig {
    const val THREAD_BITS = 4
    const val MAX_DEBUG_THREADS_LIMIT = 1 shl THREAD_BITS

    @JvmStatic
    lateinit var holder: ConfigHolder<GlobalConfig>

    @JvmStatic
    val config get() = syncedServerConfig ?: holder.config.server

    // only used on the client, probably
    private var syncedServerConfig: ServerConfig? = null

    fun init() {
        holder = AutoConfig.register(
            GlobalConfig::class.java,
            PartitioningSerializer.wrap(::Toml4jConfigSerializer),
        )

        // never save the server config here; that happens in the client config gui
        holder.registerSaveListener { _, _ -> InteractionResult.FAIL }
    }

    fun initClient() {
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register { _ ->
            syncedServerConfig = null
        }
    }

    fun initServer() {
        // don't sync the config in singleplayer, to allow changing server configs without reloading the world
        PlayerEvent.PLAYER_JOIN.register { player ->
            MsgSyncConfigS2C(holder.config.server).sendToPlayer(player)
        }
    }

    fun onSyncConfig(serverConfig: ServerConfig) {
        syncedServerConfig = serverConfig
    }

    @Config(name = HexDebug.MODID)
    class GlobalConfig(
        @Category("server")
        @TransitiveObject
        val server: ServerConfig = ServerConfig(),
    ) : GlobalData()

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

        @Tooltip
        @BoundedDiscrete(min = 1, max = MAX_DEBUG_THREADS_LIMIT.toLong())
        var maxDebugThreads: Int = 4
            private set

        fun encode(buf: FriendlyByteBuf) {
            buf.writeInt(maxUndoStackSize)
            buf.writeLong(splicingTableMediaCost)
            buf.writeLong(splicingTableMaxMedia)
            buf.writeInt(splicingTableCastingCooldown)
            buf.writeDouble(splicingTableAmbit)
            buf.writeInt(maxDebugThreads)
        }

        fun decode(buf: FriendlyByteBuf) {
            maxUndoStackSize = buf.readInt()
            splicingTableMediaCost = buf.readLong()
            splicingTableMaxMedia = buf.readLong()
            splicingTableCastingCooldown = buf.readInt()
            splicingTableAmbit = buf.readDouble()
            maxDebugThreads = buf.readInt()
        }
    }
}
