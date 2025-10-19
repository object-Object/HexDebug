package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus;
import at.petrak.hexcasting.api.casting.circles.CircleExecutionState;
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gay.object.hexdebug.core.api.HexDebugCoreAPI;
import gay.object.hexdebug.core.api.debugging.DebuggableCircleComponent;
import gay.object.hexdebug.debugger.circles.CircleDebugEnv;
import gay.object.hexdebug.debugger.circles.IMixinCircleExecutionState;
import gay.object.hexdebug.impl.IDebugEnvAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CircleExecutionState.class)
public abstract class MixinCircleExecutionState implements IMixinCircleExecutionState {
    @Shadow(remap = false)
    @Final
    public List<BlockPos> reachedPositions;
    @Shadow
    public BlockPos currentPos;
    @Shadow
    public Direction enteredFrom;
    @Shadow(remap = false)
    public CastingImage currentImage;

    @Unique
    @Nullable
    private CircleDebugEnv debugEnv$hexdebug;

    @Shadow
    @Nullable
    public abstract ServerPlayer getCaster(ServerLevel world);

    @Nullable
    @Override
    public CircleDebugEnv getDebugEnv$hexdebug() {
        return debugEnv$hexdebug;
    }

    @Override
    public void setDebugEnv$hexdebug(@Nullable CircleDebugEnv debugEnv) {
        debugEnv$hexdebug = debugEnv;
    }

    @SuppressWarnings("UnreachableCode")
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void hexdebug$debugTick(BlockEntityAbstractImpetus impetus, CallbackInfoReturnable<Boolean> cir) {
        if (debugEnv$hexdebug == null) return;

        var world = (ServerLevel) impetus.getLevel();
        if (world == null) return;

        var bs = world.getBlockState(currentPos);
        if (!(bs.getBlock() instanceof DebuggableCircleComponent debuggable)) return;

        var caster = getCaster(world);
        if (caster == null) return;

        // we'll be executing this many times, so only energize etc the first time
        if (!debuggable.isEnergized(currentPos, bs, world)) {
            bs = debuggable.startEnergized(currentPos, bs, world);
            reachedPositions.add(currentPos);

            debugEnv$hexdebug.setPaused(true);

            var env = new CircleCastEnv(world, (CircleExecutionState) (Object) this);
            debuggable.acceptDebugControlFlow(caster, debugEnv$hexdebug, currentImage, env, enteredFrom, currentPos, bs);
        }

        // if we stopped on entry or a breakpoint, continue ticking but skip the normal logic
        // if we got terminated, stop now
        // otherwise, do the regular tick logic
        if (debugEnv$hexdebug.isPaused()) {
            cir.setReturnValue(true);
        } else if (HexDebugCoreAPI.INSTANCE.getDebugEnv(caster, debugEnv$hexdebug.getSessionId()) == null) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
            ordinal = 0
        ),
        require = 0,
        remap = false
    )
    private boolean hexdebug$maybeSkipAddingToReachedPositions(
        List<Object> instance,
        Object pos,
        Operation<Boolean> original
    ) {
        if (
            pos instanceof BlockPos
            && !reachedPositions.isEmpty()
            && reachedPositions.get(reachedPositions.size() - 1) == pos
        ) {
            return true;
        }
        return original.call(instance, pos);
    }

    @ModifyExpressionValue(
        method = "tick",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/server/level/ServerLevel;Lat/petrak/hexcasting/api/casting/circles/CircleExecutionState;)Lat/petrak/hexcasting/api/casting/eval/env/CircleCastEnv;"
        )
    )
    private CircleCastEnv hexdebug$setDebugEnv(CircleCastEnv env) {
        ((IDebugEnvAccessor) env).setDebugEnv$hexdebug(debugEnv$hexdebug);
        return env;
    }

    @Inject(method = "endExecution", at = @At("HEAD"), remap = false)
    private void hexdebug$stopDebugging(BlockEntityAbstractImpetus impetus, CallbackInfo ci) {
        if (debugEnv$hexdebug != null) {
            HexDebugCoreAPI.INSTANCE.removeDebugThread(debugEnv$hexdebug);
        }
    }

    @ModifyReturnValue(method = "save", at = @At("RETURN"))
    private CompoundTag hexdebug$saveDebugEnvSessionId(CompoundTag out) {
        if (debugEnv$hexdebug != null && debugEnv$hexdebug.isDebugging()) {
            out.putUUID(TAG_HEXDEBUG_SESSION_ID, debugEnv$hexdebug.getSessionId());
        }
        return out;
    }

    @ModifyReturnValue(method = "load", at = @At("RETURN"))
    private static CircleExecutionState hexdebug$loadDebugEnv(CircleExecutionState state, CompoundTag nbt, ServerLevel level) {
        var caster = state.getCaster(level);
        if (caster != null && nbt.contains(TAG_HEXDEBUG_SESSION_ID)) {
            var sessionId = nbt.getUUID(TAG_HEXDEBUG_SESSION_ID);
            var debugEnv = HexDebugCoreAPI.INSTANCE.getDebugEnv(caster, sessionId);
            if (debugEnv instanceof CircleDebugEnv circleEnv && circleEnv.getPos() == state.impetusPos) {
                ((MixinCircleExecutionState) (Object) state).debugEnv$hexdebug = circleEnv;
            }
        }
        return state;
    }

    @Unique
    private static final String TAG_HEXDEBUG_SESSION_ID = "hexdebug:session_id";
}
