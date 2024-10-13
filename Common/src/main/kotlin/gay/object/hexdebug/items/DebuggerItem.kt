package gay.`object`.hexdebug.items

import at.petrak.hexcasting.api.spell.iota.ListIota
import at.petrak.hexcasting.api.utils.getBoolean
import at.petrak.hexcasting.api.utils.getInt
import at.petrak.hexcasting.api.utils.putBoolean
import at.petrak.hexcasting.api.utils.putInt
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.casting.eval.newDebuggerCastEnv
import gay.`object`.hexdebug.debugger.CastArgs
import gay.`object`.hexdebug.items.base.ItemPredicateProvider
import gay.`object`.hexdebug.items.base.ModelPredicateEntry
import gay.`object`.hexdebug.utils.asItemPredicate
import gay.`object`.hexdebug.utils.getWrapping
import gay.`object`.hexdebug.utils.otherHand
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.NonNullList
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.Level

class DebuggerItem(properties: Properties) : ItemPackagedHex(properties), ItemPredicateProvider {
    override fun canDrawMediaFromInventory(stack: ItemStack?) = true

    override fun breakAfterDepletion() = false

    override fun isFoil(stack: ItemStack) = false

    override fun getRarity(stack: ItemStack) = Rarity.RARE

    override fun getDefaultInstance() = applyDefaults(ItemStack(this))

    override fun fillItemCategory(category: CreativeModeTab, items: NonNullList<ItemStack>) {
        if (this.allowedIn(category)) {
            items.add(this.defaultInstance)
        }
    }

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

        val debugAdapter = DebugAdapterManager[player] ?: return InteractionResultHolder.fail(stack)
        if (debugAdapter.isDebugging) {
            // step the ongoing debug session
            debugAdapter.apply {
                when (getStepMode(stack)) {
                    StepMode.CONTINUE -> continue_(null)
                    StepMode.OVER -> next(null)
                    StepMode.IN -> stepIn(null)
                    StepMode.OUT -> stepOut(null)
                    StepMode.RESTART -> restart(null)
                    StepMode.STOP -> terminate(null)
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

            val ctx = newDebuggerCastEnv(serverPlayer, usedHand)
            val args = CastArgs(instrs, ctx, serverLevel)

            if (!debugAdapter.startDebugging(args)) {
                // already debugging (how??)
                return InteractionResultHolder.fail(stack)
            }
        }

        val stat = Stats.ITEM_USED[this]
        player.awardStat(stat)

        serverPlayer.cooldowns.addCooldown(this, 5)
        return InteractionResultHolder.consume(stack)
    }

    override fun hurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        if (stack.hoverName.string == "Thwacker" && attacker is ServerPlayer) {
            attacker.displayClientMessage(Component.translatable("text.hexdebug.thwack"), true)
        }
        return super.hurtEnemy(stack, target, attacker)
    }

    fun handleShiftScroll(sender: ServerPlayer, stack: ItemStack, delta: Double) {
        val newMode = rotateStepMode(stack, delta < 0)
        val component = Component.translatable(
            "hexdebug.tooltip.debugger.step_mode",
            Component.translatable("hexdebug.tooltip.debugger.step_mode.${newMode.name.lowercase()}"),
        )
        sender.displayClientMessage(component, true)
    }

    private fun rotateStepMode(stack: ItemStack, increase: Boolean): StepMode {
        val idx = getStepModeIdx(stack) + (if (increase) 1 else -1)
        return StepMode.values().getWrapping(idx).also {
            stack.putInt(STEP_MODE_TAG, it.ordinal)
        }
    }

    private fun getStepMode(stack: ItemStack) = StepMode.values().getWrapping(getStepModeIdx(stack))

    private fun getStepModeIdx(stack: ItemStack) = stack.getInt(STEP_MODE_TAG)

    val noIconsInstance get() = ItemStack(this).also { setHideIcons(it, true) }

    @Suppress("SameParameterValue")
    private fun setHideIcons(stack: ItemStack, value: Boolean) = stack.putBoolean(HIDE_ICONS_TAG, value)

    private fun getHideIcons(stack: ItemStack) = stack.getBoolean(HIDE_ICONS_TAG)

    override fun getModelPredicates() = listOf(
        ModelPredicateEntry(DEBUG_STATE_PREDICATE) { _, _, entity, _ ->
            // don't show the active icon for debuggers held by other players, on the ground, etc
            val state = if (entity is LocalPlayer) debugState else DebugState.NOT_DEBUGGING
            state.asItemPredicate(DebugState.values())
        },

        ModelPredicateEntry(STEP_MODE_PREDICATE) { stack, _, _, _ ->
            getStepMode(stack).asItemPredicate(StepMode.values())
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

        var debugState: DebugState = DebugState.NOT_DEBUGGING

        @JvmStatic
        fun isDebugging(): Boolean {
            return debugState == DebugState.DEBUGGING
        }
    }

    enum class DebugState {
        NOT_DEBUGGING,
        DEBUGGING,
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
