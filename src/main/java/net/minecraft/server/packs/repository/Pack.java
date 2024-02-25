package net.minecraft.server.packs.repository;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.world.flag.FeatureFlagSet;
import org.slf4j.Logger;

public class Pack {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final String id;
    public final Pack.ResourcesSupplier resources; // PAIL private -> public
    private final Component title;
    private final Component description;
    private final PackCompatibility compatibility;
    private final FeatureFlagSet requestedFeatures;
    private final Pack.Position defaultPosition;
    private final boolean required;
    private final boolean fixedPosition;
    private final PackSource packSource;

    @Nullable
    public static Pack readMetaAndCreate(String name, Component displayName, boolean alwaysEnabled, Pack.ResourcesSupplier packFactory, PackType type, Pack.Position position, PackSource source) {
        Pack.Info resourcepackloader_a = Pack.readPackInfo(name, packFactory);

        return resourcepackloader_a != null ? Pack.create(name, displayName, alwaysEnabled, packFactory, resourcepackloader_a, type, position, false, source) : null;
    }

    public static Pack create(String name, Component displayName, boolean alwaysEnabled, Pack.ResourcesSupplier packFactory, Pack.Info metadata, PackType type, Pack.Position position, boolean pinned, PackSource source) {
        return new Pack(name, alwaysEnabled, packFactory, displayName, metadata, metadata.compatibility(type), position, pinned, source);
    }

    private Pack(String name, boolean alwaysEnabled, Pack.ResourcesSupplier packFactory, Component displayName, Pack.Info metadata, PackCompatibility compatibility, Pack.Position position, boolean pinned, PackSource source) {
        this.id = name;
        this.resources = packFactory;
        this.title = displayName;
        this.description = metadata.description();
        this.compatibility = compatibility;
        this.requestedFeatures = metadata.requestedFeatures();
        this.required = alwaysEnabled;
        this.defaultPosition = position;
        this.fixedPosition = pinned;
        this.packSource = source;
    }

    @Nullable
    public static Pack.Info readPackInfo(String name, Pack.ResourcesSupplier packFactory) {
        try {
            PackResources iresourcepack = packFactory.open(name);

            Pack.Info resourcepackloader_a = null; // CraftBukkit - decompile fix
            label53:
            {
                FeatureFlagsMetadataSection featureflagsmetadatasection;

                try {
                    PackMetadataSection resourcepackinfo = (PackMetadataSection) iresourcepack.getMetadataSection(PackMetadataSection.TYPE);

                    if (resourcepackinfo != null) {
                        featureflagsmetadatasection = (FeatureFlagsMetadataSection) iresourcepack.getMetadataSection(FeatureFlagsMetadataSection.TYPE);
                        FeatureFlagSet featureflagset = featureflagsmetadatasection != null ? featureflagsmetadatasection.flags() : FeatureFlagSet.of();

                        resourcepackloader_a = new Pack.Info(resourcepackinfo.getDescription(), resourcepackinfo.getPackFormat(), featureflagset);
                        break label53;
                    }

                    Pack.LOGGER.warn("Missing metadata in pack {}", name);
                    featureflagsmetadatasection = null;
                } catch (Throwable throwable) {
                    if (iresourcepack != null) {
                        try {
                            iresourcepack.close();
                        } catch (Throwable throwable1) {
                            throwable.addSuppressed(throwable1);
                        }
                    }

                    throw throwable;
                }

                if (iresourcepack != null) {
                    iresourcepack.close();
                }

                return resourcepackloader_a; // CraftBukkit - decompile fix
            }

            if (iresourcepack != null) {
                iresourcepack.close();
            }

            return resourcepackloader_a;
        } catch (Exception exception) {
            Pack.LOGGER.warn("Failed to read pack metadata", exception);
            return null;
        }
    }

    public Component getTitle() {
        return this.title;
    }

    public Component getDescription() {
        return this.description;
    }

    public Component getChatLink(boolean enabled) {
        return ComponentUtils.wrapInSquareBrackets(this.packSource.decorate(Component.literal(this.id))).withStyle((chatmodifier) -> {
            return chatmodifier.withColor(enabled ? ChatFormatting.GREEN : ChatFormatting.RED).withInsertion(StringArgumentType.escapeIfRequired(this.id)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.empty().append(this.title).append("\n").append(this.description)));
        });
    }

    public PackCompatibility getCompatibility() {
        return this.compatibility;
    }

    public FeatureFlagSet getRequestedFeatures() {
        return this.requestedFeatures;
    }

    public PackResources open() {
        return this.resources.open(this.id);
    }

    public String getId() {
        return this.id;
    }

    public boolean isRequired() {
        return this.required;
    }

    public boolean isFixedPosition() {
        return this.fixedPosition;
    }

    public Pack.Position getDefaultPosition() {
        return this.defaultPosition;
    }

    public PackSource getPackSource() {
        return this.packSource;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Pack)) {
            return false;
        } else {
            Pack resourcepackloader = (Pack) object;

            return this.id.equals(resourcepackloader.id);
        }
    }

    public int hashCode() {
        return this.id.hashCode();
    }

    @FunctionalInterface
    public interface ResourcesSupplier {

        PackResources open(String name);
    }

    public static record Info(Component description, int format, FeatureFlagSet requestedFeatures) {

        public PackCompatibility compatibility(PackType type) {
            return PackCompatibility.forFormat(this.format, type);
        }
    }

    public static enum Position {

        TOP, BOTTOM;

        private Position() {}

        public <T> int insert(List<T> items, T item, Function<T, Pack> profileGetter, boolean listInverted) {
            Pack.Position resourcepackloader_position = listInverted ? this.opposite() : this;
            Pack resourcepackloader;
            int i;

            if (resourcepackloader_position == Pack.Position.BOTTOM) {
                for (i = 0; i < items.size(); ++i) {
                    resourcepackloader = (Pack) profileGetter.apply(items.get(i));
                    if (!resourcepackloader.isFixedPosition() || resourcepackloader.getDefaultPosition() != this) {
                        break;
                    }
                }

                items.add(i, item);
                return i;
            } else {
                for (i = items.size() - 1; i >= 0; --i) {
                    resourcepackloader = (Pack) profileGetter.apply(items.get(i));
                    if (!resourcepackloader.isFixedPosition() || resourcepackloader.getDefaultPosition() != this) {
                        break;
                    }
                }

                items.add(i + 1, item);
                return i + 1;
            }
        }

        public Pack.Position opposite() {
            return this == Pack.Position.TOP ? Pack.Position.BOTTOM : Pack.Position.TOP;
        }
    }
}
