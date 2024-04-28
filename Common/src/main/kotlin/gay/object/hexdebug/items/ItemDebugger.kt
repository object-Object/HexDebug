package gay.`object`.hexdebug.items

import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.mod.HexConfig
import at.petrak.hexcasting.api.utils.getInt
import at.petrak.hexcasting.api.utils.putInt
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex
import at.petrak.hexcasting.common.msgs.MsgNewSpiralPatternsS2C
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.CastArgs
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.casting.eval.DebugItemCastEnv
import gay.`object`.hexdebug.utils.getWrapping
import gay.`object`.hexdebug.utils.itemPredicate
import gay.`object`.hexdebug.utils.otherHand
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction
import net.minecraft.network.chat.Component
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
            // step the ongoing debug session
            debugAdapter.apply {
                when (getStepMode(stack)) {
                    StepMode.IN -> stepIn(null)
                    StepMode.OUT -> stepOut(null)
                    StepMode.OVER -> next(null)
                    StepMode.CONTINUE -> continue_(null)
                }
            }
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

        val STEP_MODE_TAG = "step_mode"

        var currentState: State = State.INACTIVE

        fun getProperties() = mapOf(
            STATE_PREDICATE to ClampedItemPropertyFunction { _, _, _, _ -> currentState.itemPredicate },
            STEP_MODE_PREDICATE to ClampedItemPropertyFunction { stack, _, _, _ ->
                getStepMode(stack).itemPredicate
            },
        )

        @JvmStatic
        fun handleShiftScroll(sender: ServerPlayer, hand: InteractionHand, stack: ItemStack, delta: Double) {
            val newMode = rotateStepMode(stack, delta > 0)
            val component = Component.translatable("hexdebug.tooltip.debugger.step_mode.${newMode.name.lowercase()}")
            sender.displayClientMessage(component, true)
        }

        private fun rotateStepMode(stack: ItemStack, increase: Boolean): StepMode {
            val idx = getStepModeIdx(stack) + (if (increase) 1 else -1)
            return StepMode.entries.getWrapping(idx).also {
                stack.putInt(STEP_MODE_TAG, it.ordinal)
            }
        }

        private fun getStepMode(stack: ItemStack) = StepMode.entries.getWrapping(getStepModeIdx(stack))

        private fun getStepModeIdx(stack: ItemStack) = stack.getInt(STEP_MODE_TAG)
    }

    enum class State {
        INACTIVE,
        WAITING_FOR_CLIENT,
        ACTIVE,
    }

    enum class StepMode {
        CONTINUE,
        OVER,
        IN,
        OUT,
    }
}
