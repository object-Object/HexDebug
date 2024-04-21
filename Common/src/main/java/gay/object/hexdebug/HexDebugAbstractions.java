package gay.object.hexdebug;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;

public class HexDebugAbstractions {
    /**
     * This explanation is mostly from Architectury's template project.
     * One thing I would like to add for those familiar with Hex Casting's layout:
     * this is a somewhat simpler and more intuitive (imo) way to accomplish what they accomplish with Xplat abstractions.
     * <p>
     * We can use {@link Platform#getConfigFolder()} but this is just an example of {@link ExpectPlatform}.
     * <p>
     * This must be a <b>public static</b> method. The platform-implemented solution must be placed under a
     * platform sub-package, with its class suffixed with {@code Impl}.
     * <p>
     * Example:
     * <p>
     * Expect: gay.object.hexdebug.HexDebugAbstractions#get()
     * <p>
     * Actual Fabric: gay.object.hexdebug.fabric.HexDebugAbstractionsImpl#get()
     * <p>
     * Actual Forge: gay.object.hexdebug.forge.HexDebugAbstractionsImpl#get()
     * <p>
     * <a href="https://plugins.jetbrains.com/plugin/16210-architectury">You should also get the IntelliJ plugin to help with @ExpectPlatform.</a>
     */
    @ExpectPlatform
    public static IHexDebugAbstractions get() {
        // Just throw an error, the content should get replaced at runtime.
        throw new AssertionError();
    }
}
