package org.plazmamc.plazma.configurations;

import io.papermc.paper.configuration.Configuration;
import io.papermc.paper.configuration.ConfigurationPart;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@SuppressWarnings({"CanBeFinal", "FieldCanBeLocal", "FieldMayBeFinal", "NotNullFieldNotInitialized", "InnerClassMayBeStatic"})
public class GlobalConfiguration extends ConfigurationPart {
    static final int CURRENT_VERSION = 1;
    static final boolean DO_OPTIMIZE = !Boolean.getBoolean("Plazma.disableConfigOptimization");
    private static GlobalConfiguration instance;

    public static GlobalConfiguration get() {
        return instance;
    }

    static void set(GlobalConfiguration instance) {
        GlobalConfiguration.instance = instance;
    }

    @Setting(Configuration.VERSION_FIELD)
    public int version = CURRENT_VERSION;

    public ConsoleLogs consoleLogs;
    public class ConsoleLogs extends ConfigurationPart {

        public boolean enableOfflineWarnings = true;
        public boolean enableRootUserWarnings = true;

    }
}
