package org.plazmamc.plazma.commands.subcommands;

import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;

import static net.kyori.adventure.text.Component.text;

public class ReloadCommand implements PlazmaSubCommand {

    @Override
    public boolean execute(CommandSender sender, String subCommand, String[] args) {
        this.doReload(sender);
        return true;
    }

    private void doReload(final CommandSender sender) {
        Command.broadcastCommandMessage(sender, text("Please note that this command is not supported and may cause issues.", NamedTextColor.RED));
        Command.broadcastCommandMessage(sender, text("If you encounter any issues please use the /stop command to restart your server.", NamedTextColor.RED));

        MinecraftServer server = ((CraftServer) sender.getServer()).getServer();
        server.plazmaConfigurations.reloadConfigurations(server);
        server.server.reloadCount++;

        Command.broadcastCommandMessage(sender, text("Successfully reloaded Plazma configuration files.", NamedTextColor.GREEN));
    }
}
