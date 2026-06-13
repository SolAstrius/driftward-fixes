package ink.astrius.driftwardfixes.mixin;

import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// `level` is a protected final field on the Balloon base class with no getter. @Shadow from the
// ServerBalloon subclass can't resolve it through inheritance, so expose it with an accessor on
// the class that actually declares it. (controllerPos already has a public getControllerPos().)
@Mixin(Balloon.class)
public interface BalloonAccessor {
    @Accessor("level")
    Level driftward$level();
}
