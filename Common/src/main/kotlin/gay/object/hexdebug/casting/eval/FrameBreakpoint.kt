package gay.`object`.hexdebug.casting.eval

import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel

object FrameBreakpoint : ContinuationFrame {
    override fun breakDownwards(stack: List<Iota>) = false to stack

    override fun evaluate(continuation: SpellContinuation, level: ServerLevel, harness: CastingVM) = CastResult(
        NullIota(),
        continuation,
        null,
        listOf(),
        ResolvedPatternType.EVALUATED,
        HexEvalSounds.NOTHING,
    )

    override fun serializeToNBT() = CompoundTag()

    override fun size() = 0

    @JvmField
    val TYPE: ContinuationFrame.Type<FrameBreakpoint> = object : ContinuationFrame.Type<FrameBreakpoint> {
        override fun deserializeFromNBT(tag: CompoundTag, world: ServerLevel) = FrameBreakpoint
    }

    override val type = TYPE
}
