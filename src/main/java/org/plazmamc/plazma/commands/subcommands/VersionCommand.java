package org.plazmamc.plazma.commands.subcommands;

import net.minecraft.server.MinecraftServer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VersionCommand implements PlazmaSubCommand {
    @Override
    public boolean execute(CommandSender sender, String subCommand, String[] args) {
        final @Nullable Command ver = MinecraftServer.getServer().server.getCommandMap().getCommand("version");
        if (ver != null) ver.execute(sender, "plazma", new String[0]);
        return true;
    }
}
