package gay.`object`.hexdebug.common.items

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.mod.HexConfig
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex
import at.petrak.hexcasting.common.msgs.MsgNewSpiralPatternsS2C
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.debugger.DebugCastArgs
import gay.`object`.hexdebug.debugger.DebugItemCastEnv
import gay.`object`.hexdebug.server.HexDebugServerManager
import gay.`object`.hexdebug.server.HexDebugServerState
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stat
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

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
        val vm = CastingVM.empty(ctx)
        val castArgs = DebugCastArgs(vm, instrs, serverLevel) {
            if (it is PatternIota) {
                val packet = MsgNewSpiralPatternsS2C(serverPlayer.uuid, listOf(it.pattern), 140)
                IXplatAbstractions.INSTANCE.sendPacketToPlayer(serverPlayer, packet)
                IXplatAbstractions.INSTANCE.sendPacketTracking(serverPlayer, packet)
            }
        }

        val debugServer = HexDebugServerManager.server
        if (debugServer == null) {
            HexDebugServerManager.queuedCast = castArgs
            player.displayClientMessage(Component.translatable("text.hexdebug.no_client"), true)
        } else when (debugServer.state) {
            HexDebugServerState.NOT_READY, HexDebugServerState.READY -> if (!debugServer.startDebugging(castArgs)) {
                return InteractionResultHolder.fail(stack)
            }
            HexDebugServerState.DEBUGGING -> debugServer.next(null)
            else -> return InteractionResultHolder.fail(stack)
        }

        ParticleSpray(player.position(), Vec3(0.0, 1.5, 0.0), 0.4, Math.PI / 3, 30)
            .sprayParticles(serverPlayer.serverLevel(), ctx.pigment)

        val broken = breakAfterDepletion() && getMedia(stack) == 0L
        val stat: Stat<*> = if (broken) {
            Stats.ITEM_BROKEN[this]
        } else {
            Stats.ITEM_USED[this]
        }
        player.awardStat(stat)

        serverPlayer.cooldowns.addCooldown(this, this.cooldown())

        if (broken) {
            stack.shrink(1)
            player.broadcastBreakEvent(usedHand)
            return InteractionResultHolder.consume(stack)
        } else {
            return InteractionResultHolder.success(stack)
        }
    }
}

val InteractionHand.otherHand get() = when (this) {
    InteractionHand.MAIN_HAND -> InteractionHand.OFF_HAND
    InteractionHand.OFF_HAND -> InteractionHand.MAIN_HAND
}
