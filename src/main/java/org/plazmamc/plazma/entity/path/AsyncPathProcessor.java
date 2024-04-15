package org.plazmamc.plazma.entity.path;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.plazmamc.plazma.configurations.GlobalConfiguration;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * used to handle the scheduling of async path processing
 */
public class AsyncPathProcessor {

    private static final Executor mainThreadExecutor = MinecraftServer.getServer();
    private static final Executor pathProcessingExecutor = new ThreadPoolExecutor(
            1,
            GlobalConfiguration.get().entity.asyncPathProcessing.maxThreads,
            GlobalConfiguration.get().entity.asyncPathProcessing.keepAlive, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder()
                    .setNameFormat("plazma-path-processor-%d")
                    .setPriority(Thread.NORM_PRIORITY - 2)
                    .build()
    );

    protected static CompletableFuture<Void> queue(@NotNull AsyncPath path) {
        return CompletableFuture.runAsync(path::process, pathProcessingExecutor);
    }

    /**
     * takes a possibly unprocessed path, and waits until it is completed
     * the consumer will be immediately invoked if the path is already processed
     * the consumer will always be called on the main thread
     *
     * @param path            a path to wait on
     * @param afterProcessing a consumer to be called
     */
    public static void awaitProcessing(@Nullable Path path, Consumer<@Nullable Path> afterProcessing) {
        if (path != null && !path.isProcessed() && path instanceof AsyncPath asyncPath) {
            asyncPath.postProcessing(() -> mainThreadExecutor.execute(() -> afterProcessing.accept(path)));
        } else {
            afterProcessing.accept(path);
        }
    }
}