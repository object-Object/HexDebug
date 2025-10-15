package gay.`object`.hexdebug.items

import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.mod.HexConfig
import at.petrak.hexcasting.api.utils.*
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.casting.eval.DebuggerCastEnv
import gay.`object`.hexdebug.debugger.CastArgs
import gay.`object`.hexdebug.items.base.ItemPredicateProvider
import gay.`object`.hexdebug.items.base.ModelPredicateEntry
import gay.`object`.hexdebug.items.base.getThreadId
import gay.`object`.hexdebug.items.base.rotateThreadId
import gay.`object`.hexdebug.utils.asItemPredicate
import gay.`object`.hexdebug.utils.getWrapping
import gay.`object`.hexdebug.utils.otherHand
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.Level
import org.eclipse.lsp4j.debug.*

class DebuggerItem(properties: Properties) : ItemPackagedHex(properties), ItemPredicateProvider {
    override fun canDrawMediaFromInventory(stack: ItemStack?) = true

    override fun breakAfterDepletion() = false

    override fun cooldown() = HexConfig.common().artifactCooldown()

    override fun isFoil(stack: ItemStack) = false

    override fun getRarity(stack: ItemStack) = Rarity.RARE

    override fun getDefaultInstance() = applyDefaults(ItemStack(this))

    // TODO: this doesn't give the advancement until after it's taken out of the table
    override fun onCraftedBy(stack: ItemStack, level: Level, player: Player) {
        applyDefaults(stack)
    }

    private fun applyDefaults(stack: ItemStack) = stack.apply {
        enchant(Enchantments.BANE_OF_ARTHROPODS, 1)
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

        if (debugAdapter.isDebugging(threadId)) {
            // step the ongoing debug session
            debugAdapter.apply {
                when (getStepMode(stack)) {
                    StepMode.CONTINUE -> continue_(ContinueArguments().also {
                        it.threadId = threadId
                        it.singleThread = true
                    })
                    StepMode.OVER -> next(NextArguments().also {
                        it.threadId = threadId
                        it.singleThread = true
                    })
                    StepMode.IN -> stepIn(StepInArguments().also {
                        it.threadId = threadId
                        it.singleThread = true
                    })
                    StepMode.OUT -> stepOut(StepOutArguments().also {
                        it.threadId = threadId
                        it.singleThread = true
                    })
                    StepMode.RESTART -> restartThread(threadId)
                    StepMode.STOP -> terminateThreads(TerminateThreadsArguments().also {
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

            val ctx = DebuggerCastEnv(serverPlayer, usedHand)
            val args = CastArgs(instrs, ctx, serverLevel)

            if (!debugAdapter.startDebugging(threadId, args)) {
                // already debugging (how??)
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

    fun handleShiftScroll(sender: ServerPlayer, stack: ItemStack, delta: Double, isCtrl: Boolean) {
        val increase = delta < 0
        val component = if (isCtrl) {
            rotateThreadId(stack, increase)
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

    enum class StepMode {
        CONTINUE,
        OVER,
        IN,
        OUT,
        RESTART,
        STOP,
    }
}
