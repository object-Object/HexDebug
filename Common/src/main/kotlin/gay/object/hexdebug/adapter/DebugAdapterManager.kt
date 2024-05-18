package gay.`object`.hexdebug.adapter

import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.PlayerEvent
import gay.`object`.hexdebug.HexDebug
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import java.util.*

object DebugAdapterManager {
    private val debugAdapters = mutableMapOf<UUID, DebugAdapter>()

    operator fun get(player: Player) = debugAdapters[player.uuid]

    fun init() {
        PlayerEvent.PLAYER_JOIN.register { player ->
            add(player)
        }
        PlayerEvent.PLAYER_QUIT.register { player ->
            remove(player)
        }
        LifecycleEvent.SERVER_STOPPING.register {
            removeAll()
        }
    }

    private fun add(player: ServerPlayer) {
        HexDebug.LOGGER.debug("Adding debug adapter for {}", player.uuid)
        debugAdapters[player.uuid] = DebugAdapter(player)
    }

    private fun remove(player: ServerPlayer) {
        HexDebug.LOGGER.debug("Removing debug adapter for {}", player.uuid)
        get(player)?.disconnectClient()
        debugAdapters.remove(player.uuid)
    }

    private fun removeAll() {
        HexDebug.LOGGER.debug("Removing {} debug adapters", debugAdapters.size)
        for (debugAdapter in debugAdapters.values) {
            debugAdapter.disconnectClient()
        }
        debugAdapters.clear()
    }
}
