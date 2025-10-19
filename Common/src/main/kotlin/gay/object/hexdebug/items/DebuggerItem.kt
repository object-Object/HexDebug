package gay.`object`.hexdebug.items

import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.mod.HexConfig
import at.petrak.hexcasting.api.utils.*
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.casting.eval.DebuggerCastEnv
import gay.`object`.hexdebug.core.api.debugging.DebuggableBlock
import gay.`object`.hexdebug.core.api.debugging.SimplePlayerBasedDebugEnv
import gay.`object`.hexdebug.core.api.exceptions.DebugException
import gay.`object`.hexdebug.items.base.*
import gay.`object`.hexdebug.utils.asItemPredicate
import gay.`object`.hexdebug.utils.getWrapping
import gay.`object`.hexdebug.utils.otherHand
import gay.`object`.hexdebug.utils.styledHoverName
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.Level
import org.eclipse.lsp4j.debug.*
import kotlin.math.max

class DebuggerItem(
    properties: Properties,
    val isQuenched: Boolean,
) : ItemPackagedHex(properties), ItemPredicateProvider, ShiftScrollable {
    override fun canDrawMediaFromInventory(stack: ItemStack?) = true

    override fun breakAfterDepletion() = false

    override fun cooldown() = HexConfig.common().artifactCooldown()

    override fun isFoil(stack: ItemStack) = false

    override fun getRarity(stack: ItemStack) = if (isQuenched) Rarity.RARE else Rarity.UNCOMMON

    override fun getDefaultInstance() = applyDefaults(ItemStack(this))

    // TODO: this doesn't give the advancement until after it's taken out of the table
    override fun onCraftedBy(stack: ItemStack, level: Level, player: Player) {
        applyDefaults(stack)
    }

    private fun applyDefaults(stack: ItemStack) = stack.also {
        val enchantments = EnchantmentHelper.getEnchantments(stack)
        enchantments.compute(Enchantments.BANE_OF_ARTHROPODS) { _, level ->
            max(level ?: 0, if (isQuenched) 2 else 1)
        }
        EnchantmentHelper.setEnchantments(enchantments, stack)
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val debuggable = context.level.getBlockState(context.clickedPos).block as? DebuggableBlock
            ?: (context.level.getBlockEntity(context.clickedPos) as? DebuggableBlock)
            ?: return InteractionResult.PASS

        val threadId = getThreadId(context.itemInHand)

        if (context.level.isClientSide) {
            val isDebugging = debugStates[threadId] == DebugState.DEBUGGING
            return if (isDebugging) InteractionResult.PASS else InteractionResult.SUCCESS
        }

        val player = context.player as ServerPlayer

        if (DebugAdapterManager[player]?.isDebugging(threadId) != false) {
            return InteractionResult.PASS
        }

        return debuggable.startDebugging(context, threadId).also {
            if (it.shouldAwardStats()) {
                val stat = Stats.ITEM_USED[this]
                player.awardStat(stat)
            }

            if (it.consumesAction()) {
                player.cooldowns.addCooldown(this, this.cooldown())
            }
        }
    }

    override fun use(world: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)

        if (world.isClientSide) {
            return InteractionResultHolder.success(stack)
        }

        val serverPlayer = player as ServerPlayer
        val serverLevel = world as ServerLevel

        val threadId = getThreadId(stack)

        val debugAdapter = DebugAdapterManager[player]
            ?: return InteractionResultHolder.fail(stack)

        val debugger = debugAdapter.debugger(threadId)
        if (debugger != null) {
            if (!debugger.debugEnv.isCasterInRange) {
                player.displayClientMessage("text.hexdebug.debugging.out_of_range".asTranslatedComponent, true)
                return InteractionResultHolder.fail(stack)
            }

            val stepMode = getStepMode(stack)
            if (debugger.state.canPause && stepMode.canPause) {
                debugAdapter.pause(PauseArguments().also {
                    it.threadId = threadId
                })
            } else {
                // step the ongoing debug session
                when (stepMode) {
                    StepMode.CONTINUE -> debugAdapter.continue_(ContinueArguments().also {
                        it.threadId = threadId
                        it.singleThread = true
                    })
                    StepMode.OVER -> debugAdapter.next(NextArguments().also {
                        it.threadId = threadId
                        it.singleThread = true
                    })
                    StepMode.IN -> debugAdapter.stepIn(StepInArguments().also {
                        it.threadId = threadId
                        it.singleThread = true
                    })
                    StepMode.OUT -> debugAdapter.stepOut(StepOutArguments().also {
                        it.threadId = threadId
                        it.singleThread = true
                    })
                    StepMode.RESTART -> debugAdapter.restartThread(threadId)
                    StepMode.STOP -> debugAdapter.terminateThreads(TerminateThreadsArguments().also {
                        it.threadIds = intArrayOf(threadId)
                    })
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

            val env = DebuggerCastEnv(serverPlayer, usedHand)
            val debugEnv = SimplePlayerBasedDebugEnv(serverPlayer, env, instrs, stack.styledHoverName)

            try {
                debugEnv.start(threadId)
            } catch (_: DebugException) {
                player.displayClientMessage("text.hexdebug.debugging.illegal_thread".asTranslatedComponent, true)
                return InteractionResultHolder.fail(stack)
            }
        }

        val stat = Stats.ITEM_USED[this]
        player.awardStat(stat)

        serverPlayer.cooldowns.addCooldown(this, this.cooldown())
        return InteractionResultHolder.consume(stack)
    }

    override fun hurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        if (stack.hoverName.string == "Thwacker" && attacker is ServerPlayer) {
            attacker.displayClientMessage(Component.translatable("text.hexdebug.thwack"), true)
        }
        return super.hurtEnemy(stack, target, attacker)
    }

    // always allow shift, only allow ctrl if quenched
    override fun canShiftScroll(isCtrl: Boolean) = !isCtrl || isQuenched

    override fun handleShiftScroll(sender: ServerPlayer, stack: ItemStack, delta: Double, isCtrl: Boolean) {
        val increase = delta < 0
        val component = if (isCtrl) {
            rotateThreadId(sender, stack, increase)
        } else {
            rotateStepMode(stack, increase)
        }
        sender.displayClientMessage(component, true)
    }

    val noIconsInstance get() = ItemStack(this).also { setHideIcons(it, true) }

    @Suppress("SameParameterValue")
    private fun setHideIcons(stack: ItemStack, value: Boolean) = stack.putBoolean(HIDE_ICONS_TAG, value)

    private fun getHideIcons(stack: ItemStack) = stack.getBoolean(HIDE_ICONS_TAG)

    override fun getModelPredicates() = listOf(
        ModelPredicateEntry(DEBUG_STATE_PREDICATE) { stack, _, entity, _ ->
            // don't show the active icon for debuggers held by other players, on the ground, etc
            val state = if (entity is LocalPlayer) getDebugState(stack) else DebugState.NOT_DEBUGGING
            state.asItemPredicate
        },

        ModelPredicateEntry(STEP_MODE_PREDICATE) { stack, _, _, _ ->
            getStepMode(stack).asItemPredicate
        },

        ModelPredicateEntry(HAS_HEX_PREDICATE) { stack, _, _, _ ->
            hasHex(stack).asItemPredicate
        },

        ModelPredicateEntry(HIDE_ICONS_PREDICATE) { stack, _, _, _ ->
            getHideIcons(stack).asItemPredicate
        },
    )

    companion object {
        private const val STEP_MODE_TAG = "step_mode"
        private const val HIDE_ICONS_TAG = "hide_icons"

        val DEBUG_STATE_PREDICATE = HexDebug.id("debug_state")
        val STEP_MODE_PREDICATE = HexDebug.id("step_mode")
        val HAS_HEX_PREDICATE = HexDebug.id("has_hex")
        val HIDE_ICONS_PREDICATE = HexDebug.id("hide_icons")

        var debugStates = mutableMapOf<Int, DebugState>()

        @JvmStatic
        fun isDebugging(stack: ItemStack): Boolean {
            return getDebugState(stack) == DebugState.DEBUGGING
        }

        private fun getDebugState(stack: ItemStack) = debugStates[getThreadId(stack)] ?: DebugState.NOT_DEBUGGING

        private fun getStepMode(stack: ItemStack) = StepMode.entries.getWrapping(getStepModeIdx(stack))

        private fun getStepModeIdx(stack: ItemStack) = stack.getInt(STEP_MODE_TAG)

        private fun rotateStepMode(stack: ItemStack, increase: Boolean): Component {
            val idx = getStepModeIdx(stack) + (if (increase) 1 else -1)
            val mode = StepMode.entries.getWrapping(idx)
            stack.putInt(STEP_MODE_TAG, mode.ordinal)
            return "hexdebug.tooltip.debugger.step_mode.${mode.name.lowercase()}".asTranslatedComponent
        }
    }

    enum class DebugState {
        NOT_DEBUGGING,
        DEBUGGING;

        companion object {
            fun of(value: Boolean) = if (value) DEBUGGING else NOT_DEBUGGING
        }
    }

    enum class StepMode(val canPause: Boolean) {
        CONTINUE(canPause = true),
        OVER(canPause = true),
        IN(canPause = true),
        OUT(canPause = true),
        RESTART(canPause = false),
        STOP(canPause = false),
    }
}
