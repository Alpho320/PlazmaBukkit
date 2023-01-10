package org.plazmamc.plazma.configurations;

import com.google.common.collect.Table;
import com.mojang.logging.LogUtils;
import io.leangen.geantyref.TypeToken;
import io.papermc.paper.configuration.*;
import io.papermc.paper.configuration.legacy.RequiresSpigotInitialization;
import io.papermc.paper.configuration.serializer.ComponentSerializer;
import io.papermc.paper.configuration.serializer.EnumValueSerializer;
import io.papermc.paper.configuration.serializer.PacketClassSerializer;
import io.papermc.paper.configuration.serializer.StringRepresentableSerializer;
import io.papermc.paper.configuration.serializer.collections.FastutilMapSerializer;
import io.papermc.paper.configuration.serializer.collections.MapSerializer;
import io.papermc.paper.configuration.serializer.collections.TableSerializer;
import io.papermc.paper.configuration.serializer.registry.RegistryHolderSerializer;
import io.papermc.paper.configuration.serializer.registry.RegistryValueSerializer;
import io.papermc.paper.configuration.transformation.Transformations;
import io.papermc.paper.configuration.type.*;
import io.papermc.paper.configuration.type.fallback.FallbackValueSerializer;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.spongepowered.configurate.*;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.transformation.TransformAction;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static io.leangen.geantyref.GenericTypeReflector.erase;

@SuppressWarnings("Convert2Diamond")
public class PlazmaConfigurations extends Configurations<GlobalConfiguration, LevelConfigurations> {

    private static final Logger LOGGER = LogUtils.getLogger();
    static final String GLOBAL_CONFIGURATION_FILE_NAME= "plazma-global.yml";
    static final String LEVEL_DEFAULT_CONFIGURATION_FILE_NAME = "plazma-level-defaults.yml";
    static final String LEVEL_CONFIGURATION_FILE_NAME = "plazma-configuration.yml";

    private static final String HEADER_START = """
            # English
            This is the %s configuration file for Plazma.
            As you can see, there's a lot to configure. Some options may impact gameplay,
            so use with caution, and make sure you know what each option does before configuring.
            
            If you need help with the configuration or have any questions related to Plazma,
            join us in our Discord for Plazma, or check the GitHub Wiki pages.
            
            %s
            
            # 한국어
            본 파일은 Plazma의 %s 구성 파일입니다.
            보시다시피, 굉장히 많은 설정이 있습니다. 몇몇 설정은 게임플레이에 영향을 줄 수 있으므로,
            주의해서 사용하시기 바라며, 각 설정이 어떠한 역할을 하는지 알고 사용하시기 바랍니다.
            
            만약 구성에 관한 도움이 필요하거나 Plazma에 관련한 질문이 있으시다면,
            Discord 또는 네이버 카페에 가입하거나, GitHub 위키 페이지를 참고하시기 바랍니다.
            
            %s
            
            Wiki: https://github.com/PlazmaMC/Plazma/wiki
            Discord: https://discord.gg/MmfC52K8A8
            Twitter: *COMMING SOON*
            Naver Cafe: *COMMING SOON*
            """;

    private static final Function<Configurations.ContextMap, String> LEVEL_SPECIFIC_HEADER = map -> String.format("""
            # English
            This is a level specific configuration file for Plazma.
            This file may start empty, but can be filled with settings to override ones in the %s/%s
            
            If you need help with the configuration or have any questions related to Plazma,
            join us in our Discord for Plazma, or check the GitHub Wiki pages.
            
            
            # 한국어
            본 파일은 Plazma의 레벨별 구성 파일입니다.
            이 파일은 비어있을 수 있지만, %s/%s 파일의 설정을 덮어쓰기 위해 설정을 채울 수 있습니다.
            
            만약 구성에 관한 도움이 필요하거나 Plazma에 관련한 질문이 있으시다면,
            Discord 또는 네이버 카페에 가입하거나, GitHub 위키 페이지를 참고하시기 바랍니다.
            
            
            Level: %s (%s)
            
            Wiki: https://github.com/PlazmaMC/Plazma/wiki
            Discord: https://discord.gg/MmfC52K8A8
            Twitter: *COMMING SOON*
            Naver Cafe: *COMMING SOON*
            """, PaperConfigurations.CONFIG_DIR, LEVEL_DEFAULT_CONFIGURATION_FILE_NAME, PaperConfigurations.CONFIG_DIR, LEVEL_DEFAULT_CONFIGURATION_FILE_NAME,
            map.require(WORLD_NAME), map.require(WORLD_KEY));

    private static final String GLOBAL_HEADER = String.format(HEADER_START,
            "global", String.format("""
                    The level configuration options are inside their respective level folder.
                    The files are named %s
                    """, LEVEL_CONFIGURATION_FILE_NAME),
            "전역", String.format("""
                    레벨 구성 옵션은 각각의 레벨 폴더 안에 있으며, 파일 이름은 %s 입니다.
                    """, LEVEL_CONFIGURATION_FILE_NAME)
    );

    private static final String LEVEL_DEFAULTS_HEADER = String.format(HEADER_START,
            "level defaults", """
                    Configuration options here apply to all levels, unless you specify overrides inside
                    the level-specific config file inside each level folder.
                    """,
            "기본 레벨", """
                    이 구성 파일의 설정은 각 레벨 폴더 안 구성 파일에서 덮어쓰기 되지 않는 한 모든 레벨에 적용됩니다.
                    """
    );

    private static final List<Transformations.DefaultsAware> DEFAULTS_AWARE_TRANSFORMATIONS = Collections.emptyList();

    public PlazmaConfigurations(final Path globalFolder) {
        super(globalFolder, GlobalConfiguration.class, LevelConfigurations.class, GLOBAL_CONFIGURATION_FILE_NAME, LEVEL_DEFAULT_CONFIGURATION_FILE_NAME, LEVEL_CONFIGURATION_FILE_NAME);
    }

    // Create Loader Builder
    private static ConfigurationOptions defaultOptions(ConfigurationOptions options) {
        return options.serializers(builder -> builder
                .register(MapSerializer.TYPE, new MapSerializer(false))
                .register(new EnumValueSerializer())
                .register(new ComponentSerializer())
        );
    }

    @Override
    protected YamlConfigurationLoader.Builder createLoaderBuilder() {
        return super.createLoaderBuilder().defaultOptions(PlazmaConfigurations::defaultOptions);
    }

    // Create Global Object Mapper Factory Builder
    private static ObjectMapper.Factory.Builder defaultGlobalFactoryBuilder(ObjectMapper.Factory.Builder builder) {
        return builder.addDiscoverer(InnerClassFieldDiscoverer.globalConfig());
    }

    @Override
    protected ObjectMapper.Factory.Builder createGlobalObjectMapperFactoryBuilder() {
        return defaultGlobalFactoryBuilder(super.createGlobalObjectMapperFactoryBuilder());
    }

    // Create Global Loader Builder
    private static ConfigurationOptions defaultGlobalOptions(ConfigurationOptions options) {
        return options.header(GLOBAL_HEADER).serializers(builder -> builder.register(new PacketClassSerializer()));
    }

    @Override
    protected YamlConfigurationLoader.Builder createGlobalLoaderBuilder() {
        return super.createGlobalLoaderBuilder().defaultOptions(PlazmaConfigurations::defaultGlobalOptions);
    }

    // Initialize
    @Override
    public GlobalConfiguration initializeGlobalConfiguration() throws ConfigurateException {
        GlobalConfiguration configuration = super.initializeGlobalConfiguration();
        GlobalConfiguration.set(configuration);
        return configuration;
    }

    @Override
    protected ContextMap.Builder createDefaultContextMap() {
        return super.createDefaultContextMap().put(PaperConfigurations.SPIGOT_WORLD_CONFIG_CONTEXT_KEY, PaperConfigurations.SPIGOT_WORLD_DEFAULTS);
    }

    @Override
    protected ObjectMapper.Factory.Builder createWorldObjectMapperFactoryBuilder(final ContextMap contextMap) {
        return super.createWorldObjectMapperFactoryBuilder(contextMap)
                .addNodeResolver(new RequiresSpigotInitialization.Factory(contextMap.require(PaperConfigurations.SPIGOT_WORLD_CONFIG_CONTEXT_KEY).get()))
                .addNodeResolver(new NestedSetting.Factory())
                .addDiscoverer(InnerClassFieldDiscoverer.plazmaLevelConfiguration(contextMap));
    }

    @Override
    protected YamlConfigurationLoader.Builder createWorldConfigLoaderBuilder(final ContextMap contextMap) {
        return super.createWorldConfigLoaderBuilder(contextMap).defaultOptions(options -> options
                .header(contextMap.require(WORLD_NAME).equals(WORLD_DEFAULTS) ? LEVEL_DEFAULTS_HEADER : LEVEL_SPECIFIC_HEADER.apply(contextMap))
                .serializers(serializers -> serializers
                        .register(new TypeToken<Reference2IntMap<?>>() {}, new FastutilMapSerializer.SomethingToPrimitive<Reference2IntMap<?>>(Reference2IntOpenHashMap::new, Integer.TYPE))
                        .register(new TypeToken<Reference2LongMap<?>>() {}, new FastutilMapSerializer.SomethingToPrimitive<Reference2LongMap<?>>(Reference2LongOpenHashMap::new, Long.TYPE))
                        .register(new TypeToken<Table<?, ?, ?>>() {}, new TableSerializer())
                        .register(new StringRepresentableSerializer())
                        .register(IntOr.Default.SERIALIZER)
                        .register(DoubleOrDefault.SERIALIZER)
                        .register(BooleanOrDefault.SERIALIZER)
                        .register(Duration.SERIALIZER)
                        .register(EngineMode.SERIALIZER)
                        .register(FallbackValueSerializer.create(contextMap.require(PaperConfigurations.SPIGOT_WORLD_CONFIG_CONTEXT_KEY).get(), MinecraftServer::getServer))
                        .register(new RegistryValueSerializer<>(new TypeToken<EntityType<?>>() {}, Registries.ENTITY_TYPE, true))
                        .register(new RegistryValueSerializer<>(Item.class, Registries.ITEM, true))
                        .register(new RegistryHolderSerializer<>(new TypeToken<ConfiguredFeature<?, ?>>() {}, Registries.CONFIGURED_FEATURE, false))
                        .register(new RegistryHolderSerializer<>(Item.class, Registries.ITEM, true))
                )
        );
    }

    private void applyTransformations(final NodePath[] paths, final ConfigurationNode node) throws ConfigurateException {
        if (paths.length > 0) {
            ConfigurationTransformation.Builder builder = ConfigurationTransformation.builder();

            for (NodePath path : paths)
                builder.addAction(path, TransformAction.remove());

            builder.build().apply(node);
        }
    }

    @Override
    protected void applyGlobalConfigTransformations(final ConfigurationNode node) throws ConfigurateException {
        applyTransformations(RemovedConfigurations.REMOVED_GLOBAL_PATHS, node);
    }

    @Override
    protected void applyWorldConfigTransformations(final ContextMap contextMap, final ConfigurationNode node) throws ConfigurateException {
        final ConfigurationNode version = node.node(Configuration.VERSION_FIELD);
        final String world = contextMap.require(WORLD_NAME);

        if (version.virtual()) {
            LOGGER.warn("The Plazma level configuration file for {} didn't have a version field, assuming latest", world);
            version.raw(LevelConfigurations.CURRENT_VERSION);
        }

        applyTransformations(RemovedConfigurations.REMOVED_LEVEL_PATHS, node);
    }

    @Override
    protected void applyDefaultsAwareWorldConfigTransformations(final ContextMap contextMap, final ConfigurationNode levelNode, final ConfigurationNode defaultsNode) throws ConfigurateException {
        final ConfigurationTransformation.Builder builder = ConfigurationTransformation.builder();
        DEFAULTS_AWARE_TRANSFORMATIONS.forEach(transform -> transform.apply(builder, contextMap, defaultsNode));

        ConfigurationTransformation transformation;
        try {
            transformation = builder.build();
        } catch (IllegalArgumentException ignored) {
            return;
        }
        transformation.apply(levelNode);
    }

    @Override
    public LevelConfigurations createWorldConfig(final ContextMap contextMap) {
        final String levelName = contextMap.require(WORLD_NAME);
        try {
            return super.createWorldConfig(contextMap);
        } catch (IOException exception) {
            throw new RuntimeException(String.format("Could not create Plazma level configuration for %s", levelName), exception);
        }
    }

    @Override
    protected boolean isConfigType(Type type) {
        return ConfigurationPart.class.isAssignableFrom(erase(type));
    }

    @Override
    protected int getWorldConfigurationCurrentVersion() {
        return LevelConfigurations.CURRENT_VERSION;
    }

    @VisibleForTesting
    static ConfigurationNode createForTesting() {
        ObjectMapper.Factory factory = defaultGlobalFactoryBuilder(ObjectMapper.factoryBuilder()).build();
        ConfigurationOptions options = defaultGlobalOptions(defaultOptions(ConfigurationOptions.defaults()))
                .serializers(builder -> builder.register(type -> ConfigurationPart.class.isAssignableFrom(erase(type)), factory.asTypeSerializer()));
        return BasicConfigurationNode.root(options);
    }

    public static PlazmaConfigurations setup(final Path configurationDir) throws Exception {
        try {
            PaperConfigurations.createDirectoriesSymlinkAware(configurationDir);
            return new PlazmaConfigurations(configurationDir);
        } catch (final IOException e) {
            throw new RuntimeException("Could not setup Plazma configuration files", e);
        }
    }

    public void reloadConfigurations(MinecraftServer server) {
        try {
            this.initializeGlobalConfiguration(reloader(this.globalConfigClass, GlobalConfiguration.get()));
            this.initializeWorldDefaultsConfiguration();
            for (ServerLevel level : server.getAllLevels())
                this.createWorldConfig(PaperConfigurations.createWorldContextMap(level), reloader(this.worldConfigClass, level.plazmaLevelConfiguration()));
        } catch (Exception e) {
            throw new RuntimeException("Could not reload Plazma configuration files", e);
        }
    }
}
