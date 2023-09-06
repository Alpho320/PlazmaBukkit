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

    public ChunkSending chunkSending;
    public class ChunkSending extends ConfigurationPart {

        public boolean enabled = DO_OPTIMIZE;
        public int maxChunksPerTick = 5;

    }

    public Structure structure;
    public class Structure extends ConfigurationPart {

        public Portal portal;
        public class Portal extends ConfigurationPart {

            public NetherPortal netherPortal;
            public class NetherPortal extends ConfigurationPart {

                public Size size;
                public class Size extends ConfigurationPart {

                    public Width width;
                    public Height height;

                    public class Width extends ConfigurationPart {

                        int min = 2;
                        int max = 21;

                        public int min() {
                            return Math.max(this.min, 1);
                        }

                        public int max() {
                            return Math.max(this.min, this.max);
                        }

                    }

                    public class Height extends ConfigurationPart {

                        int min = 3;
                        int max = 21;

                        public int min() {
                            return Math.max(this.min, 2);
                        }

                        public int max() {
                            return Math.max(this.min, this.max);
                        }

                    }

                }

            }

        }

    }

    public Misc misc;
    public class Misc extends ConfigurationPart {

        public boolean reduceCreateRandomInstance = DO_OPTIMIZE;
        public boolean checkSpectatorMovedToQuickly = DO_OPTIMIZE;
        public boolean suppressErrorFromDirtyAttributes = false;

    }

    public Entity entity;
    public class Entity extends ConfigurationPart {

        public Monster monster;
        public class Monster extends ConfigurationPart {

            public Phantom phantom;
            public class Phantom extends ConfigurationPart {

                public boolean doNotLoadChunksToSpawn = DO_OPTIMIZE;

            }

        }

        public Sensor sensor;
        public class Sensor extends ConfigurationPart {

            public int tick = DO_OPTIMIZE ? 10 : 1;

        }

        public WakeUp wakeUp;
        public class WakeUp extends ConfigurationPart {

            public DurationVariance durationVariance;
            public class DurationVariance extends ConfigurationPart {

                public double animal = DO_OPTIMIZE ? 0.2 : 0.0;
                public double monster = DO_OPTIMIZE ? 0.2 : 0.0;
                public double flyingMonster = DO_OPTIMIZE ? 0.2 : 0.0;
                public double villager = DO_OPTIMIZE ? 0.2 : 0.0;

            }

        }

    }

    public CarpetFixes carpetFixes;

    public class CarpetFixes extends ConfigurationPart {

        public boolean enabled = DO_OPTIMIZE;
        boolean optimizedSheepChildColor = false;

        public boolean optimizedSheepChildColor() {
            return enabled && optimizedSheepChildColor;
        }

    }
}
