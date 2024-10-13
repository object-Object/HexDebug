package gay.`object`.hexdebug.items

import at.petrak.hexcasting.common.items.ItemStaff
import at.petrak.hexcasting.common.lib.HexSounds
import at.petrak.hexcasting.common.network.MsgNewSpellPatternAck
import at.petrak.hexcasting.common.network.MsgNewSpellPatternSyn
import at.petrak.hexcasting.common.network.MsgOpenSpellGuiAck
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.casting.eval.newEvaluatorCastEnv
import gay.`object`.hexdebug.utils.itemPredicate
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class EvaluatorItem(properties: Properties) : ItemStaff(properties), ItemPredicateProvider {
    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemStack = player.getItemInHand(hand)

        if (world.isClientSide) {
            if (player.isShiftKeyDown && evalState == EvalState.MODIFIED) {
                player.playSound(HexSounds.FAIL_PATTERN, 1f, 1f)
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
        }

        val patterns = debugger.evaluatorUIPatterns
        val view = debugger.getClientView()
        IXplatAbstractions.INSTANCE.sendPacketToPlayer(
            player, MsgOpenSpellGuiAck(hand, patterns, view.stack, view.parenthesized, view.ravenmind, view.parenCount)
        )

        player.awardStat(Stats.ITEM_USED[this])

        return InteractionResultHolder.consume(itemStack)
    }

    override fun getModelPredicates() = listOf(
        ModelPredicateEntry(EVAL_STATE_PREDICATE) { _, _, entity, _ ->
            // don't show the active icon for items held by other players, on the ground, etc
            val state = if (entity is LocalPlayer) evalState else EvalState.DEFAULT
            state.itemPredicate(EvalState.values())
        }
    )

    companion object {
        val EVAL_STATE_PREDICATE = HexDebug.id("eval_state")

        var evalState: EvalState = EvalState.DEFAULT

        /**
         * Copy of [MsgNewSpellPatternSyn.handle][at.petrak.hexcasting.common.network.MsgNewSpellPatternSyn.handle]
         * for evaluating patterns in the active debugger, if any.
         */
        @JvmStatic
        fun handleNewPatternOnServer(sender: ServerPlayer, msg: MsgNewSpellPatternSyn) {
            val debugAdapter = DebugAdapterManager[sender]
            val debugger = debugAdapter?.debugger
            if (debugAdapter == null || debugger == null) {
                return
            }

            val env = newEvaluatorCastEnv(sender, msg.handUsed)
            val clientInfo = debugAdapter.evaluate(env, msg.pattern) ?: return

            debugger.evaluatorUIPatterns.clear()
            if (!clientInfo.isStackClear) {
                msg.resolvedPatterns.lastOrNull()?.type = clientInfo.resolutionType
                debugger.evaluatorUIPatterns.addAll(msg.resolvedPatterns)
            }

            IXplatAbstractions.INSTANCE.sendPacketToPlayer(
                sender, MsgNewSpellPatternAck(clientInfo, msg.resolvedPatterns.size - 1)
            )
        }
    }

    enum class EvalState {
        DEFAULT,
        MODIFIED,
    }
}
