package gay.object.hexdebug.core.api.debugging;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import gay.object.hexdebug.core.api.HexDebugCoreAPI;
import org.jetbrains.annotations.Nullable;

/**
 * An optional helper interface for implementing casting environments with debug support.
 */
public interface DebuggableCastEnv {
    @Nullable
    default DebugEnvironment getDebugEnv() {
        return HexDebugCoreAPI.INSTANCE.getDebugEnv((CastingEnvironment) this);
    }
}
