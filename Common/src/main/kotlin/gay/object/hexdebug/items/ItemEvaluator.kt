package gay.`object`.hexdebug.items

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.common.items.ItemStaff
import at.petrak.hexcasting.common.lib.HexSounds
import at.petrak.hexcasting.common.msgs.*
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class ItemEvaluator(properties: Properties) : ItemStaff(properties) {
    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemStack = player.getItemInHand(hand)

        if (world.isClientSide) {
            if (player.isShiftKeyDown) {
                player.playSound(HexSounds.STAFF_RESET, 1f, 1f)
            }
            return InteractionResultHolder.success(itemStack)
        }

        player as ServerPlayer

        val debugAdapter = DebugAdapterManager[player]
        val debugger = debugAdapter?.debugger
        if (debugAdapter == null || debugger == null) {
            return InteractionResultHolder.fail(itemStack)
        }

        if (player.isShiftKeyDown) {
            debugAdapter.resetEvaluator()
            MsgClearSpiralPatternsS2C(player.uuid).also {
                IXplatAbstractions.INSTANCE.sendPacketToPlayer(player, it)
                IXplatAbstractions.INSTANCE.sendPacketTracking(player, it)
            }
        }

        val patterns = debugger.evaluatorUIPatterns
        val (stack, ravenmind) = debugger.vm.generateDescs()
        IXplatAbstractions.INSTANCE.sendPacketToPlayer(
            player, MsgOpenSpellGuiS2C(hand, patterns, stack, ravenmind, 0)
        )

        player.awardStat(Stats.ITEM_USED[this])

        return InteractionResultHolder.consume(itemStack)
    }

    companion object {
        /**
         * Copy of [StaffCastEnv.handleNewPatternOnServer][at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv.handleNewPatternOnServer]
         * for evaluating patterns in the active debugger, if any.
         *
         * Returns `true` if the pattern was handled successfully (ie. handleNewPatternOnServer should be cancelled).
         */
        @JvmStatic
        fun handleNewEvaluatorPatternOnServer(sender: ServerPlayer, msg: MsgNewSpellPatternC2S): Boolean {
            val debugAdapter = DebugAdapterManager[sender]
            val debugger = debugAdapter?.debugger
            if (debugAdapter == null || debugger == null) {
                return false
            }

            val clientInfo = debugAdapter.evaluate(msg.pattern) ?: return false

            debugger.evaluatorUIPatterns.clear()
            if (!clientInfo.isStackClear) {
                msg.resolvedPatterns.lastOrNull()?.type = clientInfo.resolutionType
                debugger.evaluatorUIPatterns.addAll(msg.resolvedPatterns)
            }

            IXplatAbstractions.INSTANCE.sendPacketToPlayer(
                sender, MsgNewSpellPatternS2C(clientInfo, msg.resolvedPatterns.size - 1)
            )

            val packet = if (clientInfo.isStackClear) {
                MsgClearSpiralPatternsS2C(sender.uuid)
            } else {
                MsgNewSpiralPatternsS2C(sender.uuid, listOf(msg.pattern()), Integer.MAX_VALUE)
            }
            IXplatAbstractions.INSTANCE.sendPacketToPlayer(sender, packet)
            IXplatAbstractions.INSTANCE.sendPacketTracking(sender, packet)

            if (clientInfo.resolutionType.success) {
                ParticleSpray(sender.position(), Vec3(0.0, 1.5, 0.0), 0.4, Math.PI / 3, 30)
                    .sprayParticles(sender.serverLevel(), IXplatAbstractions.INSTANCE.getPigment(sender))
            }

            return true
        }
    }
}
