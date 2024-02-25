package net.minecraft.world.flag;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class FeatureFlagRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final FeatureFlagUniverse universe;
    public final Map<ResourceLocation, FeatureFlag> names; // PAIL public
    private final FeatureFlagSet allFlags;

    FeatureFlagRegistry(FeatureFlagUniverse universe, FeatureFlagSet featureSet, Map<ResourceLocation, FeatureFlag> featureFlags) {
        this.universe = universe;
        this.names = featureFlags;
        this.allFlags = featureSet;
    }

    public boolean isSubset(FeatureFlagSet features) {
        return features.isSubsetOf(this.allFlags);
    }

    public FeatureFlagSet allFlags() {
        return this.allFlags;
    }

    public FeatureFlagSet fromNames(Iterable<ResourceLocation> features) {
        return this.fromNames(features, (minecraftkey) -> {
            FeatureFlagRegistry.LOGGER.warn("Unknown feature flag: {}", minecraftkey);
        });
    }

    public FeatureFlagSet subset(FeatureFlag... features) {
        return FeatureFlagSet.create(this.universe, Arrays.asList(features));
    }

    public FeatureFlagSet fromNames(Iterable<ResourceLocation> features, Consumer<ResourceLocation> unknownFlagConsumer) {
        Set<FeatureFlag> set = Sets.newIdentityHashSet();
        Iterator iterator = features.iterator();

        while (iterator.hasNext()) {
            ResourceLocation minecraftkey = (ResourceLocation) iterator.next();
            FeatureFlag featureflag = (FeatureFlag) this.names.get(minecraftkey);

            if (featureflag == null) {
                unknownFlagConsumer.accept(minecraftkey);
            } else {
                set.add(featureflag);
            }
        }

        return FeatureFlagSet.create(this.universe, set);
    }

    public Set<ResourceLocation> toNames(FeatureFlagSet features) {
        Set<ResourceLocation> set = new HashSet();

        this.names.forEach((minecraftkey, featureflag) -> {
            if (features.contains(featureflag)) {
                set.add(minecraftkey);
            }

        });
        return set;
    }

    public Codec<FeatureFlagSet> codec() {
        return ResourceLocation.CODEC.listOf().comapFlatMap((list) -> {
            Set<ResourceLocation> set = new HashSet();

            Objects.requireNonNull(set);
            FeatureFlagSet featureflagset = this.fromNames(list, set::add);

            return !set.isEmpty() ? DataResult.error(() -> {
                return "Unknown feature ids: " + set;
            }, featureflagset) : DataResult.success(featureflagset);
        }, (featureflagset) -> {
            return List.copyOf(this.toNames(featureflagset));
        });
    }

    public static class Builder {

        private final FeatureFlagUniverse universe;
        private int id;
        private final Map<ResourceLocation, FeatureFlag> flags = new LinkedHashMap();

        public Builder(String universe) {
            this.universe = new FeatureFlagUniverse(universe);
        }

        public FeatureFlag createVanilla(String feature) {
            return this.create(new ResourceLocation("minecraft", feature));
        }

        public FeatureFlag create(ResourceLocation feature) {
            if (this.id >= 64) {
                throw new IllegalStateException("Too many feature flags");
            } else {
                FeatureFlag featureflag = new FeatureFlag(this.universe, this.id++);
                FeatureFlag featureflag1 = (FeatureFlag) this.flags.put(feature, featureflag);

                if (featureflag1 != null) {
                    throw new IllegalStateException("Duplicate feature flag " + feature);
                } else {
                    return featureflag;
                }
            }
        }

        public FeatureFlagRegistry build() {
            FeatureFlagSet featureflagset = FeatureFlagSet.create(this.universe, this.flags.values());

            return new FeatureFlagRegistry(this.universe, featureflagset, Map.copyOf(this.flags));
        }
    }
}
