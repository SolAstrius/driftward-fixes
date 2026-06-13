package ink.astrius.driftwardfixes.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// CactusBlock.canSurvive() pops the cactus if any of its four horizontal neighbours is solid.
// Treat blocks tagged #c:glass_panes as non-solid for that check, so a cactus can stand inside
// glass-pane walls (greenhouse builds) without breaking. Every other solid neighbour still
// breaks it, and the sand/lava checks are untouched.
@Mixin(CactusBlock.class)
public class CactusGlassPaneMixin {

    @Unique
    private static final TagKey<Block> DRIFTWARD$GLASS_PANES =
        TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_panes"));

    @Redirect(
        method = "canSurvive",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolid()Z")
    )
    private boolean driftward$glassPanesNotSolid(BlockState neighbor) {
        if (neighbor.is(DRIFTWARD$GLASS_PANES)) {
            return false;
        }
        return neighbor.isSolid();
    }
}
