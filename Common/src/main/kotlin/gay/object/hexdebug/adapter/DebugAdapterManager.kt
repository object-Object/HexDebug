package gay.`object`.hexdebug.adapter

import gay.`object`.hexdebug.proxy.DebugAdapterProxyServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import java.util.*

object DebugAdapterManager {
    private val proxyServers = mutableMapOf<UUID, DebugAdapterProxyServer>()

    operator fun get(player: Player) = get(player.uuid)

    operator fun get(playerUUID: UUID) = proxyServers[playerUUID]

    fun register(player: ServerPlayer, debugAdapter: DebugAdapter) {
        proxyServers[player.uuid] = DebugAdapterProxyServer(player, debugAdapter)
    }

    fun stop(playerUUID: UUID) {
        get(playerUUID)?.debugAdapter?.stop()
        proxyServers.remove(playerUUID)
    }

    fun stopAll() {
        for ((_, proxyServer) in proxyServers) {
            proxyServer.debugAdapter.stop()
        }
        proxyServers.clear()
    }
}
