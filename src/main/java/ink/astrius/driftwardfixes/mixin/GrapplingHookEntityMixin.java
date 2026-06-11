package ink.astrius.driftwardfixes.mixin;

import com.github.eterdelta.crittersandcompanions.entity.GrapplingHookEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Critters & Companions' GrapplingHookEntity.tick() calls distanceToSqr(getOwner())
// without a null-check; when the owner is gone (disconnect/death) this NPEs. Cancel the
// tick when there is no owner.
@Mixin(GrapplingHookEntity.class)
public class GrapplingHookEntityMixin {
    @Inject(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lcom/github/eterdelta/crittersandcompanions/entity/GrapplingHookEntity;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"),
        cancellable = true
    )
    private void driftward$fixGrapplingHook(CallbackInfo ci) {
        if (((GrapplingHookEntity) (Object) this).getOwner() == null) {
            ci.cancel();
        }
    }
}
