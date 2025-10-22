package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.block.HexBlockEntity;
import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus;
import at.petrak.hexcasting.api.casting.circles.CircleExecutionState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gay.object.hexdebug.core.api.HexDebugCoreAPI;
import gay.object.hexdebug.core.api.debugging.OutputCategory;
import gay.object.hexdebug.core.api.exceptions.DebugException;
import gay.object.hexdebug.debugger.circles.CircleDebugEnv;
import gay.object.hexdebug.debugger.circles.IMixinBlockEntityAbstractImpetus;
import gay.object.hexdebug.debugger.circles.IMixinCircleExecutionState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityAbstractImpetus.class)
public abstract class MixinBlockEntityAbstractImpetus
    extends HexBlockEntity implements IMixinBlockEntityAbstractImpetus
{
    @Shadow(remap = false)
    @Nullable
    protected CircleExecutionState executionState;

    public MixinBlockEntityAbstractImpetus(BlockEntityType<?> pType, BlockPos pWorldPosition, BlockState pBlockState) {
        super(pType, pWorldPosition, pBlockState);
    }

    @Shadow
    public abstract void startExecution(@Nullable ServerPlayer player);

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    @NotNull
    public InteractionResult startDebugging(@NotNull ServerPlayer caster, int threadId) {
        if (executionState != null) return InteractionResult.PASS;

        var debugEnv = new CircleDebugEnv(caster, getBlockPos());
        try {
            HexDebugCoreAPI.INSTANCE.createDebugThread(debugEnv, threadId);
        } catch (DebugException ignored) {
            return InteractionResult.PASS;
        }

        startExecution(caster);
        ((IMixinCircleExecutionState) executionState).setDebugEnv$hexdebug(debugEnv);

        return InteractionResult.CONSUME;
    }

    @Override
    public void hexdebug$clearExecutionState() {
        executionState = null;
    }

    @Inject(method = "postPrint", at = @At("HEAD"))
    private void hexdebug$printDebugMessage(Component printDisplay, CallbackInfo ci) {
        if (executionState != null) {
            var debugEnv = ((IMixinCircleExecutionState) executionState).getDebugEnv$hexdebug();
            if (debugEnv != null) {
                debugEnv.printDebugMessage(printDisplay);
            }
        }
    }

    @Inject(method = "postMishap", at = @At("HEAD"))
    private void hexdebug$printDebugMishap(Component mishapDisplay, CallbackInfo ci) {
        if (executionState != null) {
            var debugEnv = ((IMixinCircleExecutionState) executionState).getDebugEnv$hexdebug();
            if (debugEnv != null) {
                debugEnv.printDebugMessage(mishapDisplay, OutputCategory.STDERR);
            }
        }
    }

    @WrapOperation(
        method = "postNoExits",
        at = @At(
            value = "INVOKE",
            target = "Lat/petrak/hexcasting/api/casting/circles/BlockEntityAbstractImpetus;postDisplay(Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void hexdebug$printDebugNoExits(
        BlockEntityAbstractImpetus instance,
        Component error,
        ItemStack display,
        Operation<Void> original
    ) {
        if (executionState != null) {
            var debugEnv = ((IMixinCircleExecutionState) executionState).getDebugEnv$hexdebug();
            if (debugEnv != null) {
                debugEnv.printDebugMessage(error, OutputCategory.STDERR, false);
            }
        }
        original.call(instance, error, display);
    }
}
