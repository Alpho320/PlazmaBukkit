package org.bukkit.craftbukkit.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;

public final class Versioning {
    // Plazma start
    public static String version = "Unknown-Version";
    static {
        InputStream stream = Bukkit.class.getClassLoader().getResourceAsStream("META-INF/maven/org.plazmamc.plazma/plazma-api/pom.properties");
        Properties properties = new Properties();

        if (stream != null) {
            try {
                properties.load(stream);

                version = properties.getProperty("version");
            } catch (IOException ex) {
                Logger.getLogger(Versioning.class.getName()).log(Level.SEVERE, "Could not get Plazma version!", ex);
            }
        }
    }

    public static String getBukkitVersion() {
        return version;
    }
    // Plazma end
}
