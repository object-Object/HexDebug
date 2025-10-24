package gay.`object`.hexdebug.adapter

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.EntityEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.PlayerEvent
import gay.`object`.hexdebug.HexDebug
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import java.util.*

object DebugAdapterManager {
    private val debugAdapters = mutableMapOf<UUID, DebugAdapter>()

    operator fun get(playerUUID: UUID) = debugAdapters[playerUUID]

    operator fun get(player: Player) = debugAdapters[player.uuid]

    operator fun get(env: CastingEnvironment) = env.castingEntity?.let { it as? Player }?.let { get(it) }

    fun init() {
        PlayerEvent.PLAYER_JOIN.register { player ->
            add(player)
        }
        PlayerEvent.PLAYER_QUIT.register { player ->
            remove(player)
        }
        EntityEvent.LIVING_DEATH.register { entity, _ ->
            if (entity is ServerPlayer) {
                get(entity)?.onDeath()
            }
            EventResult.pass()
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
        get(player)?.onRemove()
        debugAdapters.remove(player.uuid)
    }

    private fun removeAll() {
        HexDebug.LOGGER.debug("Removing {} debug adapters", debugAdapters.size)
        for (debugAdapter in debugAdapters.values) {
            debugAdapter.onRemove()
        }
        debugAdapters.clear()
    }
}
