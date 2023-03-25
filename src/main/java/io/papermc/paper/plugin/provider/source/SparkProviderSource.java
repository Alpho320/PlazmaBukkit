package io.papermc.paper.plugin.provider.source;

import com.mojang.logging.LogUtils;
import io.papermc.paper.plugin.entrypoint.Entrypoint;
import io.papermc.paper.plugin.entrypoint.EntrypointHandler;
import io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler;
import io.papermc.paper.plugin.provider.PluginProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.stream.Collectors;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class SparkProviderSource extends FileProviderSource {
    public static final SparkProviderSource INSTANCE = new SparkProviderSource();

    private static final Logger LOGGER = LogUtils.getClassLogger();

    public SparkProviderSource() {
        super("File '%s' specified by Purpur"::formatted);
    }

    @Override
    public void registerProviders(EntrypointHandler entrypointHandler, Path context) throws Exception {
        // first, check if user doesn't want spark at all
        if (Boolean.getBoolean("Purpur.IReallyDontWantSpark")) {
            return; // boo!
        }

        // second, check if user has their own spark
        if (hasSpark()) {
            LOGGER.info("Purpur: Using user-provided spark plugin instead of our own.");
            return; // let's hope it's at least the modern version :3
        }

        // you can't have errors in your code if you wrap the entire codebase in a try/catch block
        try {

            // make sure the directory exists where we want to keep spark
            File file = context.toFile();
            file.getParentFile().mkdirs();

            boolean shouldDownload;

            // check if our spark exists
            if (!file.exists()) {
                // it does not, so let's download it
                shouldDownload = true;
            } else {
                // we have a spark file, let's see if it's up-to-date by comparing shas
                String fileSha1 = String.format("%040x", new BigInteger(1, MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(file.toPath()))));
                String sparkSha1;

                // luck has a nifty endpoint containing the sha of the newest version
                URLConnection urlConnection = new URL("https://sparkapi.lucko.me/download/bukkit/sha1").openConnection();

                // set a reasonable timeout to prevent servers without internet from hanging for 60+ seconds on startup
                urlConnection.setReadTimeout(5000);
                urlConnection.setConnectTimeout(5000);

                // read it
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                    sparkSha1 = reader.lines().collect(Collectors.joining(""));
                }

                // compare; we only download a new spark if the shas don't match
                shouldDownload = !fileSha1.equals(sparkSha1);
            }

            // ok, finally we can download spark if we need it
            if (shouldDownload) {
                URLConnection urlConnection = new URL("https://sparkapi.lucko.me/download/bukkit").openConnection();
                urlConnection.setReadTimeout(5000);
                urlConnection.setConnectTimeout(5000);
                Files.copy(urlConnection.getInputStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // register the spark, newly downloaded or existing
            super.registerProviders(entrypointHandler, context);

        } catch (Throwable e) {
            LOGGER.error("Purpur: Failed to download and install spark plugin", e);
        }
    }

    private static boolean hasSpark() {
        for (PluginProvider<JavaPlugin> provider : LaunchEntryPointHandler.INSTANCE.get(Entrypoint.PLUGIN).getRegisteredProviders()) {
            if (provider.getMeta().getName().equalsIgnoreCase("spark")) {
                return true;
            }
        }
        return false;
    }
}