package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import gay.object.hexdebug.api.IMixinCastingImage;
import gay.object.hexdebug.debugger.DebugStepType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


// this value will not be serialized, but that's fine
// we just need to smuggle it from Action#operate to HexDebugger, which shouldn't go through serde
@Mixin(CastingImage.class)
public abstract class MixinCastingImage implements IMixinCastingImage {
    @Unique
    @Nullable
    private DebugStepType hexDebug$debugStepType;

    @Override
    public @Nullable DebugStepType hexDebug$getDebugStepType() {
        return hexDebug$debugStepType;
    }

    @Override
    public void hexDebug$setDebugStepType(@Nullable DebugStepType debugStepType) {
        this.hexDebug$debugStepType = debugStepType;
    }
}
