package org.plazmamc.plazma.commands;

import net.minecraft.server.MinecraftServer;
import org.bukkit.command.Command;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.HashMap;
import java.util.Map;

@DefaultQualifier(NonNull.class)
public final class PlazmaCommands {

    private PlazmaCommands() {}

    private static final Map<String, Command> COMMANDS = new HashMap<>();
    static {
        COMMANDS.put("plazma", new PlazmaCommand("plazma"));
    }

    public static void registerCommands(final MinecraftServer server) {
        COMMANDS.forEach((s, command) -> server.server.getCommandMap().register(s, "Plazma", command));
    }

}
