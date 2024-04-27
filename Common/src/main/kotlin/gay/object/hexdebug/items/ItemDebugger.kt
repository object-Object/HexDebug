package gay.`object`.hexdebug.items

import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.mod.HexConfig
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex
import at.petrak.hexcasting.common.msgs.MsgNewSpiralPatternsS2C
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.CastArgs
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.casting.eval.DebugItemCastEnv
import gay.`object`.hexdebug.utils.itemPredicate
import gay.`object`.hexdebug.utils.otherHand
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class ItemDebugger(properties: Properties) : ItemPackagedHex(properties) {
    override fun canDrawMediaFromInventory(stack: ItemStack?) = true

    override fun breakAfterDepletion() = false

    override fun cooldown() = HexConfig.common().artifactCooldown()

    override fun use(world: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)

        if (world.isClientSide) {
            return InteractionResultHolder.success(stack)
        }

        val serverPlayer = player as ServerPlayer
        val serverLevel = world as ServerLevel

        val debugAdapter = DebugAdapterManager[player] ?: return InteractionResultHolder.fail(stack)
        if (debugAdapter.isDebugging) {
            // step the ongoing debug session (TODO: step types)
            debugAdapter.next(null)
        } else {
            // start a new debug session
            val instrs = if (hasHex(stack)) {
                getHex(stack, serverLevel) ?: return InteractionResultHolder.fail(stack)
            } else {
                val datumHolder = IXplatAbstractions.INSTANCE.findDataHolder(player.getItemInHand(usedHand.otherHand))
                when (val iota = datumHolder?.readIota(serverLevel)) {
                    is ListIota -> iota.list.toList()
                    else -> null
                }
            } ?: return InteractionResultHolder.fail(stack)

            val ctx = DebugItemCastEnv(serverPlayer, usedHand)
            val args = CastArgs(instrs, ctx, serverLevel) {
                if (it is PatternIota) {
                    val packet = MsgNewSpiralPatternsS2C(serverPlayer.uuid, listOf(it.pattern), 140)
                    IXplatAbstractions.INSTANCE.sendPacketToPlayer(serverPlayer, packet)
                    IXplatAbstractions.INSTANCE.sendPacketTracking(serverPlayer, packet)
                }
            }
            debugAdapter.cast(args)
        }

        val stat = Stats.ITEM_USED[this]
        player.awardStat(stat)

        serverPlayer.cooldowns.addCooldown(this, this.cooldown())
        return InteractionResultHolder.consume(stack)
    }

    companion object {
        val STATE_PREDICATE = HexDebug.id("state")
        val STEP_MODE_PREDICATE = HexDebug.id("step_mode")

        var debuggerState: State = State.INACTIVE

        // TODO: use CC or something probably
        val debuggerStepMode
            get(): StepMode = StepMode.entries[(System.currentTimeMillis() / 4000).toInt() % 4]

        fun getProperties() = mapOf(
            STATE_PREDICATE to ClampedItemPropertyFunction { _, _, _, _ -> debuggerState.itemPredicate },
            STEP_MODE_PREDICATE to ClampedItemPropertyFunction { _, _, _, _ -> debuggerStepMode.itemPredicate },
        )
    }

    enum class State {
        INACTIVE,
        WAITING_FOR_CLIENT,
        ACTIVE,
    }

    enum class StepMode {
        IN,
        OUT,
        OVER,
        CONTINUE,
    }
}
