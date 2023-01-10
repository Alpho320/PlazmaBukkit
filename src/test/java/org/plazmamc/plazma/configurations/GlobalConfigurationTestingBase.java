package org.plazmamc.plazma.configurations;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class GlobalConfigurationTestingBase {
    public static void setupGlobalConfigurationForTest() {
        if (GlobalConfiguration.get() == null) {
            ConfigurationNode node = PlazmaConfigurations.createForTesting();
            try {
                GlobalConfiguration globalConfiguration = node.require(GlobalConfiguration.class);
                GlobalConfiguration.set(globalConfiguration);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
