package gay.`object`.hexdebug.items

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.common.items.ItemStaff
import at.petrak.hexcasting.common.lib.HexSounds
import at.petrak.hexcasting.common.msgs.*
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.casting.eval.EvaluatorCastEnv
import gay.`object`.hexdebug.items.base.ItemPredicateProvider
import gay.`object`.hexdebug.items.base.ModelPredicateEntry
import gay.`object`.hexdebug.utils.asItemPredicate
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class EvaluatorItem(properties: Properties) : ItemStaff(properties), ItemPredicateProvider {
    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemStack = player.getItemInHand(hand)

        if (world.isClientSide) {
            if (player.isShiftKeyDown && evalState == EvalState.MODIFIED) {
                player.playSound(HexSounds.STAFF_RESET, 1f, 1f)
            }
            return InteractionResultHolder.success(itemStack)
        }

        player as ServerPlayer

        val debugAdapter = DebugAdapterManager[player]
        val debugger = debugAdapter?.debugger
        if (debugAdapter == null || debugger == null) {
            player.displayClientMessage(Component.translatable("text.hexdebug.no_session"), true)
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
        val (stack, ravenmind) = debugger.generateDescs()
        IXplatAbstractions.INSTANCE.sendPacketToPlayer(
            player, MsgOpenSpellGuiS2C(hand, patterns, stack, ravenmind, 0)
        )

        player.awardStat(Stats.ITEM_USED[this])

        return InteractionResultHolder.consume(itemStack)
    }

    override fun getModelPredicates() = listOf(
        ModelPredicateEntry(EVAL_STATE_PREDICATE) { _, _, entity, _ ->
            // don't show the active icon for items held by other players, on the ground, etc
            val state = if (entity is LocalPlayer) evalState else EvalState.DEFAULT
            state.asItemPredicate
        }
    )

    companion object {
        val EVAL_STATE_PREDICATE = HexDebug.id("eval_state")

        var evalState: EvalState = EvalState.DEFAULT

        /**
         * Copy of [StaffCastEnv.handleNewPatternOnServer][at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv.handleNewPatternOnServer]
         * for evaluating patterns in the active debugger, if any.
         */
        @JvmStatic
        fun handleNewPatternOnServer(sender: ServerPlayer, msg: MsgNewSpellPatternC2S) {
            val debugAdapter = DebugAdapterManager[sender]
            val debugger = debugAdapter?.debugger
            if (debugAdapter == null || debugger == null) {
                return
            }

            val env = EvaluatorCastEnv(sender, msg.handUsed)
            val clientInfo = debugAdapter.evaluate(env, msg.pattern) ?: return

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
        }
    }

    enum class EvalState {
        DEFAULT,
        MODIFIED,
    }
}
