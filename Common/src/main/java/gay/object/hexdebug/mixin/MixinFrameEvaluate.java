package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.spell.casting.CastingHarness;
import at.petrak.hexcasting.api.spell.casting.ResolvedPatternType;
import at.petrak.hexcasting.api.spell.casting.eval.FrameEvaluate;
import at.petrak.hexcasting.api.spell.casting.eval.SpellContinuation;
import at.petrak.hexcasting.api.spell.iota.Iota;
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds;
import gay.object.hexdebug.casting.eval.IMixinFrameEvaluate;
import kotlin.Pair;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(FrameEvaluate.class)
public class MixinFrameEvaluate implements IMixinFrameEvaluate {
    @Unique
    private boolean isFrameBreakpoint$hexdebug = false;
    @Unique
    private boolean stopBefore$hexdebug = false;
    @Unique
    private boolean isFatal$hexdebug = false;

    @Inject(method = "breakDownwards", at = @At("HEAD"), cancellable = true)
    private void breakDownwardsFrameBreakpoint$hexdebug(
        List<Iota> stack,
        CallbackInfoReturnable<Pair<Boolean, List<Iota>>> cir
    ) {
        if (!isFrameBreakpoint$hexdebug) return;

        cir.setReturnValue(new Pair<>(false, stack));
    }

    @Inject(method = "evaluate", at = @At("HEAD"), cancellable = true)
    private void evaluateFrameBreakpoint$hexdebug(
        SpellContinuation continuation,
        ServerLevel level,
        CastingHarness harness,
        CallbackInfoReturnable<CastingHarness.CastResult> cir
    ) {
        if (!isFrameBreakpoint$hexdebug) return;

        cir.setReturnValue(
            new CastingHarness.CastResult(
                continuation,
                null,
                ResolvedPatternType.EVALUATED,
                new ArrayList<>(),
                HexEvalSounds.NOTHING
            )
        );
    }

    @Override
    public boolean isFrameBreakpoint$hexdebug() {
        return isFrameBreakpoint$hexdebug;
    }

    @Override
    public void setFrameBreakpoint$hexdebug(boolean isFrameBreakpoint) {
        isFrameBreakpoint$hexdebug = isFrameBreakpoint;
    }

    @Override
    public boolean getStopBefore$hexdebug() {
        return stopBefore$hexdebug;
    }

    @Override
    public void setStopBefore$hexdebug(boolean stopBefore) {
        stopBefore$hexdebug = stopBefore;
    }

    @Override
    public boolean isFatal$hexdebug() {
        return isFatal$hexdebug;
    }

    @Override
    public void setFatal$hexdebug(boolean isFatal) {
        isFatal$hexdebug = isFatal;
    }
}
