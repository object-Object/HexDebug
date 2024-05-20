package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.casting.eval.ResolvedPattern;
import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv;
import at.petrak.hexcasting.api.casting.math.HexCoord;
import at.petrak.hexcasting.api.mod.HexStatistics;
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternC2S;
import gay.object.hexdebug.items.ItemEvaluator;
import gay.object.hexdebug.registry.HexDebugItems;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;

@Mixin(StaffCastEnv.class)
public abstract class MixinStaffCastEnv {
    @Inject(method = "handleNewPatternOnServer", at = @At("HEAD"), cancellable = true)
    private static void handleNewEvaluatorPatternOnServer$hexdebug(
        ServerPlayer sender,
        MsgNewSpellPatternC2S msg,
        CallbackInfo ci
    ) {
        var item = sender.getItemInHand(msg.handUsed()).getItem();
        if (item != HexDebugItems.EVALUATOR.getValue()) return;

        // FIXME: copied from handleNewPatternOnServer because I can't figure out how to inject before getStaffcastVM

        boolean cheatedPatternOverlap = false;

        List<ResolvedPattern> resolvedPatterns = msg.resolvedPatterns();
        if (!resolvedPatterns.isEmpty()) {
            var allPoints = new HashSet<HexCoord>();
            for (int i = 0; i < resolvedPatterns.size() - 1; i++) {
                ResolvedPattern pat = resolvedPatterns.get(i);
                allPoints.addAll(pat.getPattern().positions(pat.getOrigin()));
            }
            var currentResolvedPattern = resolvedPatterns.get(resolvedPatterns.size() - 1);
            var currentSpellPoints = currentResolvedPattern.getPattern()
                .positions(currentResolvedPattern.getOrigin());
            if (currentSpellPoints.stream().anyMatch(allPoints::contains)) {
                cheatedPatternOverlap = true;
            }
        }

        if (cheatedPatternOverlap) {
            ci.cancel();
            return;
        }

        sender.awardStat(HexStatistics.PATTERNS_DRAWN);

        // actual new logic

        ItemEvaluator.handleNewPatternOnServer(sender, msg);
        ci.cancel();
    }
}
