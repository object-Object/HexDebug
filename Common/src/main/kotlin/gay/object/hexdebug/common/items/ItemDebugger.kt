package gay.`object`.hexdebug.common.items

import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.mod.HexConfig
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex
import at.petrak.hexcasting.common.msgs.MsgNewSpiralPatternsS2C
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.adapter.DebugAdapter
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.debugger.DebugItemCastEnv
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

        val debugAdapter = DebugAdapterManager[player]?.debugAdapter
        if (debugAdapter != null && !debugAdapter.isTerminated) {
            // step ongoing debug session
            debugAdapter.next(null)
        } else {
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
            val newDebugAdapter = DebugAdapter(instrs, ctx, serverLevel) {
                if (it is PatternIota) {
                    val packet = MsgNewSpiralPatternsS2C(serverPlayer.uuid, listOf(it.pattern), 140)
                    IXplatAbstractions.INSTANCE.sendPacketToPlayer(serverPlayer, packet)
                    IXplatAbstractions.INSTANCE.sendPacketTracking(serverPlayer, packet)
                }
            }
            DebugAdapterManager.register(serverPlayer, newDebugAdapter)
            player.displayClientMessage(Component.translatable("text.hexdebug.no_client"), true)
        }

        val stat = Stats.ITEM_USED[this]
        player.awardStat(stat)

        serverPlayer.cooldowns.addCooldown(this, this.cooldown())
        return InteractionResultHolder.consume(stack)
    }
}

val InteractionHand.otherHand get() = when (this) {
    InteractionHand.MAIN_HAND -> InteractionHand.OFF_HAND
    InteractionHand.OFF_HAND -> InteractionHand.MAIN_HAND
}
