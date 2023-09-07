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

    public Player player;
    public class Player extends ConfigurationPart {

        public boolean allowAnyUsername = false;

    }

    public Misc misc;
    public class Misc extends ConfigurationPart.Post {

        public boolean reduceCreateRandomInstance = DO_OPTIMIZE;
        public boolean doNotTriggerLootTableRefreshForNonPlayerInteraction = DO_OPTIMIZE;
        public boolean doNotSendUselessEntityPackets = DO_OPTIMIZE;
        public boolean optimizeVarInts = DO_OPTIMIZE;

        @Override
        public void postProcess() {
            net.minecraft.network.FriendlyByteBuf.optimizeVarInts = optimizeVarInts;
        }

    }

    public NoChatReports noChatReports;
    public class NoChatReports extends ConfigurationPart {

        public boolean enabled = false;
        boolean addQueryData = true;
        boolean convertToGameMessage = true;

        public boolean addQueryData() {
            return enabled && addQueryData;
        }

        public boolean convertToGameMessage() {
            return enabled && convertToGameMessage;
        }

    }

    public FixMySpawnR fixMySpawnR;
    public class FixMySpawnR extends ConfigurationPart {

        public boolean enabled = DO_OPTIMIZE;
        public int timerTimeOut = 0;

    }

    public CarpetFixes carpetFixes;

    public class CarpetFixes extends ConfigurationPart {

        public boolean enabled = DO_OPTIMIZE;
        boolean optimizedBiomeAccess = true;

        public boolean optimizedBiomeAccess() {
            return enabled && optimizedBiomeAccess;
        }

    }

    public Entity entity;

    public class Entity extends ConfigurationPart {

        public AsyncPathProcessing asyncPathProcessing;

        public class AsyncPathProcessing extends ConfigurationPart.Post {

            public boolean enabled = false;
            public int maxThreads = 0;
            public int keepAlive = 60;

            @Override
            public void postProcess() {
                if (maxThreads < 0) {
                    maxThreads = Math.max(Runtime.getRuntime().availableProcessors() + maxThreads, 1);
                } else if (maxThreads == 0) {
                    maxThreads = Math.max(Runtime.getRuntime().availableProcessors() / 4, 1);
                }
                if (!enabled) {
                    maxThreads = 0;
                } else {
                    org.bukkit.Bukkit.getLogger().log(java.util.logging.Level.INFO, "Using " + maxThreads + " threads for Async Pathfinding");
                }
            }

        }
    }
}
