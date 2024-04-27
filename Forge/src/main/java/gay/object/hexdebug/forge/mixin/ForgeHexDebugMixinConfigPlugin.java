package gay.object.hexdebug.forge.mixin;

import gay.object.hexdebug.HexDebug;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

// disables MixinDatagenMain if we're not running the datagen task, since it's not necessary at any other time
public class ForgeHexDebugMixinConfigPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals("gay.object.hexdebug.forge.mixin.MixinDatagenMain")) {
            var shouldApply = System.getProperty("hexdebug.apply-datagen-mixin", "false").equals("true");
            if (shouldApply) {
                HexDebug.LOGGER.error("Applying scuffed datagen mixin. This should not happen if not running datagen!");
            }
            return shouldApply;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
