package gay.`object`.hexdebug.splicing

import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.math.HexPattern
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

interface ISplicingTable : Container {
    fun getClientView(): SplicingTableClientView?

    fun listStackChanged(stack: ItemStack)

    fun clipboardStackChanged(stack: ItemStack)

    /** Runs the given action, and returns an updated selection (or null if the selection should be removed). */
    fun runAction(action: SplicingTableAction, player: ServerPlayer?, selection: Selection?): Selection?

    fun drawPattern(
        player: ServerPlayer?,
        pattern: HexPattern,
        index: Int,
        selection: Selection?,
    ): Pair<Selection?, ResolvedPatternType>
}
