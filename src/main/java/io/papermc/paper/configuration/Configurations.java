package io.papermc.paper.configuration;

import com.mojang.logging.LogUtils;
import io.leangen.geantyref.TypeToken;
import io.papermc.paper.configuration.constraint.Constraint;
import io.papermc.paper.configuration.constraint.Constraints;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.util.CheckedFunction;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.UnaryOperator;

public abstract class Configurations<G, W> {

    private static final Logger LOGGER = LogUtils.getClassLogger();
    public static final String WORLD_DEFAULTS = "__world_defaults__";
    public static final ResourceLocation WORLD_DEFAULTS_KEY = new ResourceLocation("configurations", WORLD_DEFAULTS);
    protected final Path globalFolder;
    protected final Class<G> globalConfigClass;
    protected final Class<W> worldConfigClass;
    protected final String globalConfigFileName;
    protected final String defaultWorldConfigFileName;
    protected final String worldConfigFileName;

    public Configurations(
        final Path globalFolder,
        final Class<G> globalConfigType,
        final Class<W> worldConfigClass,
        final String globalConfigFileName,
        final String defaultWorldConfigFileName,
        final String worldConfigFileName
    ) {
        this.globalFolder = globalFolder;
        this.globalConfigClass = globalConfigType;
        this.worldConfigClass = worldConfigClass;
        this.globalConfigFileName = globalConfigFileName;
        this.defaultWorldConfigFileName = defaultWorldConfigFileName;
        this.worldConfigFileName = worldConfigFileName;
    }

    protected ObjectMapper.Factory.Builder createObjectMapper() {
        return ObjectMapper.factoryBuilder()
            .addConstraint(Constraint.class, new Constraint.Factory())
            .addConstraint(Constraints.Min.class, Number.class, new Constraints.Min.Factory());
    }

    protected YamlConfigurationLoader.Builder createLoaderBuilder() {
        return ConfigurationLoaders.naturallySorted();
    }

    protected abstract boolean isConfigType(final Type type);

    protected ObjectMapper.Factory.Builder createGlobalObjectMapperFactoryBuilder() {
        return this.createObjectMapper();
    }

    @MustBeInvokedByOverriders
    protected YamlConfigurationLoader.Builder createGlobalLoaderBuilder() {
        return this.createLoaderBuilder();
    }

    static <T> CheckedFunction<ConfigurationNode, T, SerializationException> creator(Class<T> type, boolean refreshNode) {
        return node -> {
            T instance = node.require(type);
            if (refreshNode) {
                node.set(type, instance);
            }
            return instance;
        };
    }

    public static <T> CheckedFunction<ConfigurationNode, T, SerializationException> reloader(Class<T> type, T instance) { // Plazma - package -> public
        return node -> {
            ObjectMapper.Factory factory = (ObjectMapper.Factory) Objects.requireNonNull(node.options().serializers().get(type));
            ObjectMapper.Mutable<T> mutable = (ObjectMapper.Mutable<T>) factory.get(type);
            mutable.load(instance, node);
            return instance;
        };
    }

    public G initializeGlobalConfiguration() throws ConfigurateException {
        return this.initializeGlobalConfiguration(creator(this.globalConfigClass, true));
    }

    private void trySaveFileNode(YamlConfigurationLoader loader, ConfigurationNode node, String filename) throws ConfigurateException {
        try {
            loader.save(node);
        } catch (ConfigurateException ex) {
            if (ex.getCause() instanceof AccessDeniedException) {
                LOGGER.warn("Could not save {}: Paper could not persist the full set of configuration settings in the configuration file. Any setting missing from the configuration file will be set with its default value in memory. Admins should make sure to review the configuration documentation at https://docs.papermc.io/paper/configuration for more details.", filename, ex);
            } else throw ex;
        }
    }

    protected G initializeGlobalConfiguration(final CheckedFunction<ConfigurationNode, G, SerializationException> creator) throws ConfigurateException {
        final Path configFile = this.globalFolder.resolve(this.globalConfigFileName);
        final YamlConfigurationLoader loader = this.createGlobalLoaderBuilder()
            .defaultOptions(this.applyObjectMapperFactory(this.createGlobalObjectMapperFactoryBuilder().build()))
            .path(configFile)
            .build();
        final ConfigurationNode node;
        if (Files.exists(configFile)) {
            node = loader.load();
        } else {
            node = CommentedConfigurationNode.root(loader.defaultOptions());
        }
        this.applyGlobalConfigTransformations(node);
        final G instance = creator.apply(node);
        trySaveFileNode(loader, node, configFile.toString());
        return instance;
    }

    protected void applyGlobalConfigTransformations(final ConfigurationNode node) throws ConfigurateException {
    }

    @MustBeInvokedByOverriders
    protected ContextMap.Builder createDefaultContextMap() {
        return ContextMap.builder()
            .put(WORLD_NAME, WORLD_DEFAULTS)
            .put(WORLD_KEY, WORLD_DEFAULTS_KEY);
    }

    public void initializeWorldDefaultsConfiguration() throws ConfigurateException {
        final ContextMap contextMap = this.createDefaultContextMap()
            .put(FIRST_DEFAULT)
            .build();
        final Path configFile = this.globalFolder.resolve(this.defaultWorldConfigFileName);
        final DefaultWorldLoader result = this.createDefaultWorldLoader(false, contextMap, configFile);
        final YamlConfigurationLoader loader = result.loader();
        final ConfigurationNode node = loader.load();
        if (result.isNewFile()) { // add version to new files
            node.node(Configuration.VERSION_FIELD).raw(getWorldConfigurationCurrentVersion()); // Plazma
        }
        this.applyWorldConfigTransformations(contextMap, node);
        final W instance = node.require(this.worldConfigClass);
        node.set(this.worldConfigClass, instance);
        trySaveFileNode(loader, node, configFile.toString());
    }

    private DefaultWorldLoader createDefaultWorldLoader(final boolean requireFile, final ContextMap contextMap, final Path configFile) {
        boolean willCreate = Files.notExists(configFile);
        if (requireFile && willCreate) {
            throw new IllegalStateException("World defaults configuration file '" + configFile + "' doesn't exist");
        }
        return new DefaultWorldLoader(
            this.createWorldConfigLoaderBuilder(contextMap)
                .defaultOptions(this.applyObjectMapperFactory(this.createWorldObjectMapperFactoryBuilder(contextMap).build()))
                .path(configFile)
                .build(),
            willCreate
        );
    }

    private record DefaultWorldLoader(YamlConfigurationLoader loader, boolean isNewFile) {
    }

    protected ObjectMapper.Factory.Builder createWorldObjectMapperFactoryBuilder(final ContextMap contextMap) {
        return this.createObjectMapper();
    }

    @MustBeInvokedByOverriders
    protected YamlConfigurationLoader.Builder createWorldConfigLoaderBuilder(final ContextMap contextMap) {
        return this.createLoaderBuilder();
    }

    // Make sure to run version transforms on the default world config first via #setupWorldDefaultsConfig
    public W createWorldConfig(final ContextMap contextMap) throws IOException {
        return this.createWorldConfig(contextMap, creator(this.worldConfigClass, false));
    }

    protected W createWorldConfig(final ContextMap contextMap, final CheckedFunction<ConfigurationNode, W, SerializationException> creator) throws IOException {
        final Path defaultsConfigFile = this.globalFolder.resolve(this.defaultWorldConfigFileName);
        final YamlConfigurationLoader defaultsLoader = this.createDefaultWorldLoader(true, this.createDefaultContextMap().build(), defaultsConfigFile).loader();
        final ConfigurationNode defaultsNode = defaultsLoader.load();

        boolean newFile = false;
        final Path dir = contextMap.require(WORLD_DIRECTORY);
        final Path worldConfigFile = dir.resolve(this.worldConfigFileName);
        if (Files.notExists(worldConfigFile)) {
            PaperConfigurations.createDirectoriesSymlinkAware(dir);
            Files.createFile(worldConfigFile); // create empty file as template
            newFile = true;
        }

        final YamlConfigurationLoader worldLoader = this.createWorldConfigLoaderBuilder(contextMap)
            .defaultOptions(this.applyObjectMapperFactory(this.createWorldObjectMapperFactoryBuilder(contextMap).build()))
            .path(worldConfigFile)
            .build();
        final ConfigurationNode worldNode = worldLoader.load();
        if (newFile) { // set the version field if new file
            worldNode.node(Configuration.VERSION_FIELD).set(getWorldConfigurationCurrentVersion()); // Plazma
        }
        this.applyWorldConfigTransformations(contextMap, worldNode);
        this.applyDefaultsAwareWorldConfigTransformations(contextMap, worldNode, defaultsNode);
        trySaveFileNode(worldLoader, worldNode, worldConfigFile.toString()); // save before loading node NOTE: don't save the backing node after loading it, or you'll fill up the world-specific config
        worldNode.mergeFrom(defaultsNode);
        return creator.apply(worldNode);
    }

    protected void applyWorldConfigTransformations(final ContextMap contextMap, final ConfigurationNode node) throws ConfigurateException {
    }

    protected void applyDefaultsAwareWorldConfigTransformations(final ContextMap contextMap, final ConfigurationNode worldNode, final ConfigurationNode defaultsNode) throws ConfigurateException {
    }

    private UnaryOperator<ConfigurationOptions> applyObjectMapperFactory(final ObjectMapper.Factory factory) {
        return options -> options.serializers(builder -> builder
            .register(this::isConfigType, factory.asTypeSerializer())
            .registerAnnotatedObjects(factory));
    }

    public Path getWorldConfigFile(ServerLevel level) {
        return level.convertable.levelDirectory.path().resolve(this.worldConfigFileName);
    }

    // Plazma start
    @Deprecated
    public org.bukkit.configuration.file.YamlConfiguration createLegacyObject(final net.minecraft.server.MinecraftServer server) {
        org.bukkit.configuration.file.YamlConfiguration global = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(this.globalFolder.resolve(this.globalConfigFileName).toFile());
        org.bukkit.configuration.ConfigurationSection worlds = global.createSection("__________WORLDS__________");
        worlds.set("__defaults__", org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(this.globalFolder.resolve(this.defaultWorldConfigFileName).toFile()));
        for (ServerLevel level : server.getAllLevels()) {
            worlds.set(level.getWorld().getName(), org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(getWorldConfigFile(level).toFile()));
        }
        return global;
    }

    protected abstract int getWorldConfigurationCurrentVersion();
    // Plazma end

    public static class ContextMap {
        private static final Object VOID = new Object();

        public static Builder builder() {
            return new Builder();
        }

        private final Map<ContextKey<?>, Object> backingMap;

        private ContextMap(Map<ContextKey<?>, Object> map) {
            this.backingMap = Map.copyOf(map);
        }

        @SuppressWarnings("unchecked")
        public <T> T require(ContextKey<T> key) {
            final @Nullable Object value = this.backingMap.get(key);
            if (value == null) {
                throw new NoSuchElementException("No element found for " + key + " with type " + key.type());
            } else if (value == VOID) {
                throw new IllegalArgumentException("Cannot get the value of a Void key");
            }
            return (T) value;
        }

        @SuppressWarnings("unchecked")
        public <T> @Nullable T get(ContextKey<T> key) {
            return (T) this.backingMap.get(key);
        }

        public boolean has(ContextKey<?> key) {
            return this.backingMap.containsKey(key);
        }

        public boolean isDefaultWorldContext() {
            return this.require(WORLD_KEY).equals(WORLD_DEFAULTS_KEY);
        }

        public static class Builder {

            private Builder() {
            }

            private final Map<ContextKey<?>, Object> buildingMap = new HashMap<>();

            public <T> Builder put(ContextKey<T> key, T value) {
                this.buildingMap.put(key, value);
                return this;
            }

            public Builder put(ContextKey<Void> key) {
                this.buildingMap.put(key, VOID);
                return this;
            }

            public ContextMap build() {
                return new ContextMap(this.buildingMap);
            }
        }
    }

    public static final ContextKey<Path> WORLD_DIRECTORY = new ContextKey<>(Path.class, "world directory");
    public static final ContextKey<String> WORLD_NAME = new ContextKey<>(String.class, "world name"); // TODO remove when we deprecate level names
    public static final ContextKey<ResourceLocation> WORLD_KEY = new ContextKey<>(ResourceLocation.class, "world key");
    public static final ContextKey<Void> FIRST_DEFAULT = new ContextKey<>(Void.class, "first default");

    public record ContextKey<T>(TypeToken<T> type, String name) {

        public ContextKey(Class<T> type, String name) {
            this(TypeToken.get(type), name);
        }

        @Override
        public String toString() {
            return "ContextKey{" + this.name + "}";
        }
    }
}
