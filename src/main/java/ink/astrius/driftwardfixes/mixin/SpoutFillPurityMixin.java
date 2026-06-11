package ink.astrius.driftwardfixes.mixin;

import com.simibubi.create.content.fluids.spout.FillingBySpout;
import dev.ghen.thirst.content.purity.WaterPurity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Thirst Was Taken tracks water "purity" (dirty -> purified) as both a fluid component and an
// item component. TWT propagates purity onto Create-basin-brewed *tea fluids* (MixinBasinRecipe),
// but NOT onto items filled by a Create Spout — so automated bottling/bowling silently resets to
// default purity. The Spout fills every container through FillingBySpout.fillItem(level, amount,
// stack, fluid); here we stamp the source fluid's purity onto the resulting water-container item,
// so dirty water yields dirty drinks through Create automation. remap=false: Create's names are
// stable at runtime (NeoForge is Mojang-mapped).
@Mixin(value = FillingBySpout.class, remap = false)
public class SpoutFillPurityMixin {
    @Inject(method = "fillItem", at = @At("RETURN"))
    private static void driftward$propagatePurity(Level level, int amount, ItemStack stack, FluidStack fluid, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result == null || result.isEmpty()) return;
        if (!WaterPurity.hasPurity(fluid)) return;          // source fluid carries no purity -> nothing to do
        if (!WaterPurity.isWaterFilledContainer(result)) return; // only stamp TWT water containers
        WaterPurity.addPurity(result, WaterPurity.getPurity(fluid));
    }
}
