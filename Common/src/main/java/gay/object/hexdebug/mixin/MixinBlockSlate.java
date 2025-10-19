package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.block.circle.BlockCircleComponent;
import at.petrak.hexcasting.api.casting.circles.ICircleComponent;
import at.petrak.hexcasting.api.casting.eval.ExecutionClientView;
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.common.blocks.circles.BlockEntitySlate;
import at.petrak.hexcasting.common.blocks.circles.BlockSlate;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gay.object.hexdebug.core.api.HexDebugCoreAPI;
import gay.object.hexdebug.core.api.debugging.BaseCircleDebugEnv;
import gay.object.hexdebug.core.api.debugging.DebuggableCircleComponent;
import gay.object.hexdebug.core.api.exceptions.IllegalDebugSessionException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.List;

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
        ServerPlayer caster,
        BaseCircleDebugEnv debugEnv,
        CastingImage imageIn,
        CircleCastEnv env,
        Direction enterDir,
        BlockPos pos,
        BlockState bs
    ) {
        if (!(caster.serverLevel().getBlockEntity(pos) instanceof BlockEntitySlate tile)) return;

        List<Iota> iotas;
        if (tile.pattern == null) {
            iotas = Collections.emptyList();
        } else {
            iotas = Collections.singletonList(new PatternIota(tile.pattern));
        }

        // TODO: maybe startExecuting should return something to tell us if we can continue
        try {
            HexDebugCoreAPI.INSTANCE.startDebuggingIotas(debugEnv, env, iotas, imageIn);
        } catch (IllegalDebugSessionException ignored) {}
    }

    @WrapOperation(
        method = "acceptControlFlow",
        at = @At(
            value = "INVOKE",
            target = "Lat/petrak/hexcasting/api/casting/eval/vm/CastingVM;queueExecuteAndWrapIota(Lat/petrak/hexcasting/api/casting/iota/Iota;Lnet/minecraft/server/level/ServerLevel;)Lat/petrak/hexcasting/api/casting/eval/ExecutionClientView;"
        )
    )
    private ExecutionClientView hexdebug$skipExecuteIfDebugging(
        CastingVM vm,
        Iota iota,
        ServerLevel world,
        Operation<ExecutionClientView> original
    ) {
        var debugEnv = HexDebugCoreAPI.INSTANCE.getDebugEnv(vm.getEnv());
        if (debugEnv instanceof BaseCircleDebugEnv circleDebugEnv) {
            if (circleDebugEnv.getNewImage() != null) {
                vm.setImage(circleDebugEnv.getNewImage());
                circleDebugEnv.setNewImage(null);
            }
            // FIXME: hack
            return vm.queueExecuteAndWrapIotas(Collections.emptyList(), world);
        }
        return original.call(vm, iota, world);
    }
}
