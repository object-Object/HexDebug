package gay.`object`.hexdebug.items

import at.petrak.hexcasting.common.items.ItemStaff
import at.petrak.hexcasting.common.lib.HexSounds
import at.petrak.hexcasting.common.msgs.MsgClearSpiralPatternsS2C
import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class ItemEvaluator(properties: Properties) : ItemStaff(properties) {
    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)

        if (world.isClientSide) {
            if (player.isShiftKeyDown) {
                player.playSound(HexSounds.STAFF_RESET, 1f, 1f)
            }
            return InteractionResultHolder.success(stack)
        }

        player as ServerPlayer

        if (player.isShiftKeyDown) {
//            IXplatAbstractions.INSTANCE.clearCastingData(player)
            MsgClearSpiralPatternsS2C(player.uuid).also {
                IXplatAbstractions.INSTANCE.sendPacketToPlayer(player, it)
                IXplatAbstractions.INSTANCE.sendPacketTracking(player, it)
            }
        }

//        val harness = IXplatAbstractions.INSTANCE.getStaffcastVM(player, hand)
//        val patterns = IXplatAbstractions.INSTANCE.getPatternsSavedInUi(player)
//        val descs = harness.generateDescs()
//
//        IXplatAbstractions.INSTANCE.sendPacketToPlayer(
//            player,
//            MsgOpenSpellGuiS2C(hand, patterns, descs.first, descs.second, 0)
//        )

        player.awardStat(Stats.ITEM_USED[this])

        return InteractionResultHolder.consume(stack)
    }
}
