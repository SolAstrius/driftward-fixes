package ink.astrius.driftwardfixes;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Driftward Fixes configuration. Currently only the balloon temperature integration
 * (Create Aeronautics hot-air/steam lift reacting to the Thermoo/Scorchful/Frostiful
 * ambient temperature). Server-side, since balloon physics is server-authoritative.
 */
public final class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue BALLOON_TEMPERATURE_ENABLED;
    public static final ModConfigSpec.DoubleValue BALLOON_REFERENCE_TEMP_C;
    public static final ModConfigSpec.DoubleValue BALLOON_SENSITIVITY_PER_C;
    public static final ModConfigSpec.DoubleValue BALLOON_MIN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue BALLOON_MAX_MULTIPLIER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Hot-air and steam balloon lift scales with ambient temperature (delta-T buoyancy):",
                "colder outside air = more lift, hotter air = less. Levitite is a separate system and is unaffected.")
            .push("balloon_temperature");

        BALLOON_TEMPERATURE_ENABLED = b
            .comment("Master toggle for temperature-dependent balloon lift.")
            .define("enabled", true);

        BALLOON_REFERENCE_TEMP_C = b
            .comment("Ambient temperature (Celsius) at which lift is unchanged (multiplier = 1.0).")
            .defineInRange("referenceTemperatureCelsius", 20.0, -273.0, 1000.0);

        BALLOON_SENSITIVITY_PER_C = b
            .comment("Lift multiplier change per Celsius degree below the reference.",
                "0.0125 => +1.25% lift per degree colder; with the default clamp this reaches 1.5x near -20C and 0.7x near +44C.")
            .defineInRange("sensitivityPerCelsius", 0.0125, 0.0, 1.0);

        BALLOON_MIN_MULTIPLIER = b
            .comment("Lowest lift multiplier (very hot environments).")
            .defineInRange("minMultiplier", 0.7, 0.0, 1.0);

        BALLOON_MAX_MULTIPLIER = b
            .comment("Highest lift multiplier (very cold environments / high altitude).")
            .defineInRange("maxMultiplier", 1.5, 1.0, 10.0);

        b.pop();
        SPEC = b.build();
    }

    private Config() {}
}
