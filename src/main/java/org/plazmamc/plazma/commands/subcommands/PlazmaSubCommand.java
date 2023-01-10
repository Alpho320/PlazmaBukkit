package org.plazmamc.plazma.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.Collections;
import java.util.List;

@DefaultQualifier(NonNull.class)
public interface PlazmaSubCommand {
    boolean execute(CommandSender sender, String subCommand, String[] args);

    default List<String> tabComplete(final CommandSender sender, final String subCommand, final String[] args) {
        return Collections.emptyList();
    }
}
