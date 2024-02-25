package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ServerFunctionLibrary implements PreparableReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter LISTER = new FileToIdConverter("functions", ".mcfunction");
    private volatile Map<ResourceLocation, CommandFunction> functions = ImmutableMap.of();
    private final TagLoader<CommandFunction> tagsLoader = new TagLoader<>(this::getFunction, "tags/functions");
    private volatile Map<ResourceLocation, Collection<CommandFunction>> tags = Map.of();
    private final int functionCompilationLevel;
    private final CommandDispatcher<CommandSourceStack> dispatcher;

    public Optional<CommandFunction> getFunction(ResourceLocation id) {
        return Optional.ofNullable(this.functions.get(id));
    }

    public Map<ResourceLocation, CommandFunction> getFunctions() {
        return this.functions;
    }

    public Collection<CommandFunction> getTag(ResourceLocation id) {
        return this.tags.getOrDefault(id, List.of());
    }

    public Iterable<ResourceLocation> getAvailableTags() {
        return this.tags.keySet();
    }

    public ServerFunctionLibrary(int level, CommandDispatcher<CommandSourceStack> commandDispatcher) {
        this.functionCompilationLevel = level;
        this.dispatcher = commandDispatcher;
    }

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier synchronizer, ResourceManager manager, ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        CompletableFuture<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> completableFuture = CompletableFuture.supplyAsync(() -> {
            return this.tagsLoader.load(manager);
        }, prepareExecutor);
        CompletableFuture<Map<ResourceLocation, CompletableFuture<CommandFunction>>> completableFuture2 = CompletableFuture.supplyAsync(() -> {
            return LISTER.listMatchingResources(manager);
        }, prepareExecutor).thenCompose((functions) -> {
            Map<ResourceLocation, CompletableFuture<CommandFunction>> map = Maps.newHashMap();
            CommandSourceStack commandSourceStack = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, (ServerLevel)null, this.functionCompilationLevel, "", CommonComponents.EMPTY, (MinecraftServer)null, (Entity)null);

            for(Map.Entry<ResourceLocation, Resource> entry : functions.entrySet()) {
                ResourceLocation resourceLocation = entry.getKey();
                ResourceLocation resourceLocation2 = LISTER.fileToId(resourceLocation);
                map.put(resourceLocation2, CompletableFuture.supplyAsync(() -> {
                    List<String> list = readLines(entry.getValue());
                    return CommandFunction.fromLines(resourceLocation2, this.dispatcher, commandSourceStack, list);
                }, prepareExecutor));
            }

            CompletableFuture<?>[] completableFutures = map.values().toArray(new CompletableFuture[0]);
            return CompletableFuture.allOf(completableFutures).handle((unused, ex) -> {
                return map;
            });
        });
        return completableFuture.thenCombine(completableFuture2, Pair::of).thenCompose(synchronizer::wait).thenAcceptAsync((intermediate) -> {
            Map<ResourceLocation, CompletableFuture<CommandFunction>> map = (Map)intermediate.getSecond();
            ImmutableMap.Builder<ResourceLocation, CommandFunction> builder = ImmutableMap.builder();
            map.forEach((id, functionFuture) -> {
                functionFuture.handle((function, ex) -> {
                    if (ex != null) {
                        LOGGER.error("Failed to load function {}", id, ex);
                    } else {
                        builder.put(id, function);
                    }

                    return null;
                }).join();
            });
            this.functions = builder.build();
            this.tags = this.tagsLoader.build((Map)intermediate.getFirst());
        }, applyExecutor);
    }

    private static List<String> readLines(Resource resource) {
        try (BufferedReader bufferedReader = resource.openAsReader()) {
            return bufferedReader.lines().toList();
        } catch (IOException var6) {
            throw new CompletionException(var6);
        }
    }
}
