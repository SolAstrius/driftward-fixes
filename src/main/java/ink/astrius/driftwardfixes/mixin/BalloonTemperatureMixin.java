package ink.astrius.driftwardfixes.mixin;

import com.github.thedeathlycow.thermoo.api.environment.EnvironmentLookup;
import com.github.thedeathlycow.thermoo.api.environment.component.EnvironmentComponentTypes;
import com.github.thedeathlycow.thermoo.api.environment.component.TemperatureRecordComponent;
import com.github.thedeathlycow.thermoo.api.util.TemperatureRecord;
import com.github.thedeathlycow.thermoo.api.util.TemperatureUnit;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import ink.astrius.driftwardfixes.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes hot-air and steam balloon lift react to the ambient Thermoo temperature where the
 * airship actually is in the world (resolved through Sable's sub-level pose, since the balloon's
 * stored position is in plot-local coordinates). Colder air = more lift, hotter = less, clamped
 * and configurable. Levitite uses a separate Sable floating-material path and is not touched here,
 * because this scales {@code totalLift}, which only aggregates {@code LiftingGasType} gases.
 */
@Mixin(ServerBalloon.class)
public abstract class BalloonTemperatureMixin {

    @Shadow private double totalLift;

    // Inherited from the Balloon base class.
    @Shadow @Final protected Level level;
    @Shadow protected BlockPos controllerPos;

    @Inject(method = "updateGasAmounts", at = @At("TAIL"))
    private void driftward$scaleLiftByTemperature(CallbackInfo ci) {
        if (!Config.BALLOON_TEMPERATURE_ENABLED.get() || this.totalLift == 0.0) {
            return;
        }
        // The balloon's controllerPos is plot-local; resolve the contraption's real world pose.
        if (!(Sable.HELPER.getContaining(this.level, (Vec3i) this.controllerPos) instanceof ServerSubLevel subLevel)) {
            return;
        }
        ServerLevel serverLevel = subLevel.getLevel();
        Pose3dc pose = subLevel.logicalPose();
        Vector3dc worldPos = pose.position();
        BlockPos samplePos = BlockPos.containing(worldPos.x(), worldPos.y(), worldPos.z());

        TemperatureRecord record = EnvironmentLookup.getInstance()
            .findEnvironmentComponents(serverLevel, samplePos)
            .getOrDefault(EnvironmentComponentTypes.TEMPERATURE, TemperatureRecordComponent.DEFAULT);
        double ambientC = record.valueInUnit(TemperatureUnit.CELSIUS);

        double reference = Config.BALLOON_REFERENCE_TEMP_C.get();
        double sensitivity = Config.BALLOON_SENSITIVITY_PER_C.get();
        double min = Config.BALLOON_MIN_MULTIPLIER.get();
        double max = Config.BALLOON_MAX_MULTIPLIER.get();

        double multiplier = Mth.clamp(1.0 + sensitivity * (reference - ambientC), min, max);
        this.totalLift *= multiplier;
    }
}
