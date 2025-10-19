package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv;
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect;
import gay.object.hexdebug.core.api.HexDebugCoreAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerBasedCastEnv.class)
public abstract class MixinPlayerBasedCastEnv extends CastingEnvironment {
    protected MixinPlayerBasedCastEnv(ServerLevel world) {
        super(world);
    }

    @Inject(method = "printMessage", at = @At("HEAD"))
    private void hexdebug$printDebugMessage(Component message, CallbackInfo ci) {
        var debugEnv = HexDebugCoreAPI.INSTANCE.getDebugEnv(this);
        if (debugEnv != null) {
            debugEnv.printDebugMessage(message);
        }
    }

    @Inject(method = "sendMishapMsgToPlayer", at = @At("HEAD"), remap = false)
    private void hexdebug$printDebugMishap(OperatorSideEffect.DoMishap mishap, CallbackInfo ci) {
        var debugEnv = HexDebugCoreAPI.INSTANCE.getDebugEnv(this);
        if (debugEnv != null) {
            debugEnv.printDebugMishap(this, mishap);
        }
    }
}
