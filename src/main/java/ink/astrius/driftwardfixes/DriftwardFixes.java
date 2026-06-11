package ink.astrius.driftwardfixes;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod("driftwardfixes")
public class DriftwardFixes {
    public DriftwardFixes(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }
}
