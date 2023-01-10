package org.plazmamc.plazma.configurations;

import io.papermc.paper.configuration.Configuration;
import io.papermc.paper.configuration.ConfigurationPart;
import io.papermc.paper.configuration.PaperConfigurations;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal", "NotNullFieldNotInitialized", "InnerClassMayBeStatic"})
public class LevelConfigurations extends ConfigurationPart {
    public static final int CURRENT_VERSION = 1;
    private static final boolean DO_OPTIMIZE = !Boolean.getBoolean("Plazma.disableConfigOptimization");

    private transient final ResourceLocation worldKey;
    public LevelConfigurations(ResourceLocation worldKey) {
        this.worldKey = worldKey;
    }

    public boolean isDefault() {
        return this.worldKey.equals(PaperConfigurations.WORLD_DEFAULTS_KEY);
    }

    @Setting(Configuration.VERSION_FIELD)
    public int version = CURRENT_VERSION;
}
