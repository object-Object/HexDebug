package gay.object.hexdebug.mixin;

import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gay.object.hexdebug.blocks.splicing.SplicingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "at.petrak.hexcasting.common.casting.actions.spells.great.OpBrainsweep$Spell")
public abstract class MixinOpBrainsweepSpell implements RenderedSpell {
    @Invoker
    public abstract BlockPos callGetPos();

    @Invoker
    public abstract Mob callGetSacrifice();

    @WrapMethod(method = "cast(Lat/petrak/hexcasting/api/casting/eval/CastingEnvironment;)V", remap = false)
    private void hexdebug$copyVillagerNameToSplicingTable(CastingEnvironment env, Operation<Void> original) {
        Component customName = null;
        if (callGetSacrifice().hasCustomName()) {
            customName = callGetSacrifice().getCustomName();
        } else if (env.getWorld().getBlockEntity(callGetPos()) instanceof SplicingTableBlockEntity table) {
            customName = table.getCustomName();
        }

        original.call(env);

        if (
            env.getWorld().getBlockEntity(callGetPos()) instanceof SplicingTableBlockEntity table
            && customName != null
        ) {
            table.setCustomName(customName);
            table.sync();
        }
    }
}
