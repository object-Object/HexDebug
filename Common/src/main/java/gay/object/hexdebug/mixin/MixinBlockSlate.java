package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.block.circle.BlockCircleComponent;
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.common.blocks.circles.BlockEntitySlate;
import at.petrak.hexcasting.common.blocks.circles.BlockSlate;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import gay.object.hexdebug.core.api.HexDebugCoreAPI;
import gay.object.hexdebug.core.api.debugging.DebuggableCircleComponent;
import gay.object.hexdebug.core.api.debugging.env.BaseCircleDebugEnv;
import gay.object.hexdebug.core.api.exceptions.IllegalDebugSessionException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Mixin(BlockSlate.class)
public abstract class MixinBlockSlate
    extends BlockCircleComponent
    implements EntityBlock, SimpleWaterloggedBlock, DebuggableCircleComponent
{
    public MixinBlockSlate(Properties p_49795_) {
        super(p_49795_);
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public void acceptDebugControlFlow(
        @NotNull ServerPlayer caster,
        @NotNull BaseCircleDebugEnv debugEnv,
        @NotNull CastingImage imageIn,
        @NotNull CircleCastEnv env,
        @NotNull Direction enterDir,
        @NotNull BlockPos pos,
        @NotNull BlockState bs
    ) {
        if (!(caster.serverLevel().getBlockEntity(pos) instanceof BlockEntitySlate tile)) return;

        List<Iota> iotas = null;
        for (var resolver : HexDebugCoreAPI.INSTANCE.getSlateDebugIotasResolvers()) {
            iotas = resolver.getIotas(caster, debugEnv, imageIn, env, enterDir, pos, bs, tile);
            if (iotas != null) break;
        }

        if (iotas == null) {
            if (tile.pattern == null) {
                iotas = Collections.emptyList();
            } else {
                iotas = Collections.singletonList(new PatternIota(tile.pattern));
            }
        }

        // TODO: maybe startExecuting should return something to tell us if we can continue
        try {
            HexDebugCoreAPI.INSTANCE.startDebuggingIotas(debugEnv, env, iotas, imageIn);
        } catch (IllegalDebugSessionException ignored) {}
    }

    @Inject(
        method = "acceptControlFlow",
        at = @At(
            value = "INVOKE",
            target = "Lat/petrak/hexcasting/api/casting/eval/vm/CastingVM;<init>(Lat/petrak/hexcasting/api/casting/eval/vm/CastingImage;Lat/petrak/hexcasting/api/casting/eval/CastingEnvironment;)V"
        ),
        cancellable = true
    )
    private void hexdebug$skipExecuteIfDebugging(
        CastingImage imageIn,
        CircleCastEnv env,
        Direction enterDir,
        BlockPos pos,
        BlockState bs,
        ServerLevel world,
        CallbackInfoReturnable<ControlFlow> cir,
        @Local(name = "exitDirs") Stream<Pair<BlockPos, Direction>> exitDirs
        ) {
        var debugEnv = HexDebugCoreAPI.INSTANCE.getDebugEnv(env);
        if (debugEnv instanceof BaseCircleDebugEnv circleDebugEnv) {
            var maybeNewImage = circleDebugEnv.getNewImage();
            circleDebugEnv.setNewImage(null);

            var newImage = maybeNewImage != null ? maybeNewImage : imageIn;
            cir.setReturnValue(new ControlFlow.Continue(newImage, exitDirs.toList()));
        }
    }
}
