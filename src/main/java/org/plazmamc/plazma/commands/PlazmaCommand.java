package org.plazmamc.plazma.commands;

import io.papermc.paper.command.CommandUtil;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.plazmamc.plazma.commands.subcommands.PlazmaSubCommand;
import org.plazmamc.plazma.commands.subcommands.ReloadCommand;
import org.plazmamc.plazma.commands.subcommands.VersionCommand;

import java.util.*;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.text;

@DefaultQualifier(NonNull.class)
public class PlazmaCommand extends Command {

    private static final Map<String, PlazmaSubCommand> SUB_COMMANDS = Util.make(() -> {
        final Map<Set<String>, PlazmaSubCommand> commands = new HashMap<>();

        commands.put(Set.of("reload"), new ReloadCommand());
        commands.put(Set.of("version"), new VersionCommand());

        return commands.entrySet().stream()
                .flatMap(entry -> entry.getKey().stream().map(key -> Map.entry(key, entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    });

    private static final Map<String, String> ALIASES = Util.make(() -> {
        final Map<String, Set<String>> aliases = new HashMap<>();

        aliases.put("reload", Set.of("rl"));
        aliases.put("version", Set.of("ver"));

        return aliases.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(s -> Map.entry(s, entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    });

    public PlazmaCommand(final String name) {
        super(name);

        final PluginManager pluginManager = Bukkit.getServer().getPluginManager();

        final List<String> permissions = new ArrayList<>();
        permissions.add("bukkit.command.plazma");
        permissions.addAll(SUB_COMMANDS.keySet().stream().map(s -> "bukkit.command.plazma." + s).toList());

        this.description = "Plazma related commands";
        this.usageMessage = String.format("/plazma [%s]", String.join("|", SUB_COMMANDS.keySet()));
        this.setPermission(String.join(";", permissions));

        for (final String perm : permissions)
            pluginManager.addPermission(new Permission(perm, PermissionDefault.OP));
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String aliases, final String[] args) throws IllegalArgumentException {
        if (args.length <= 1)
            return CommandUtil.getListMatchingLast(sender, args, SUB_COMMANDS.keySet());

        final @Nullable Pair<String, PlazmaSubCommand> subCommand = resolveSubCommand(args[0]);
        if (subCommand != null)
            return subCommand.second().tabComplete(sender, subCommand.first(), Arrays.copyOfRange(args, 1, args.length));

        return Collections.emptyList();
    }

    @Override
    public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
        if (!testPermission(sender)) return true;

        if (args.length == 0) {
            sender.sendMessage(text("Usage: " + this.usageMessage, NamedTextColor.RED));
            return false;
        }

        final @Nullable Pair<String, PlazmaSubCommand> subCommand = resolveSubCommand(args[0]);

        if (subCommand == null) {
            sender.sendMessage(text("Usage: " + this.usageMessage, NamedTextColor.RED));
            return false;
        }

        if (!testPermission(sender, subCommand.first())) return true;

        final String[] choppedArgs = Arrays.copyOfRange(args, 1, args.length);
        return subCommand.second().execute(sender, subCommand.first(), choppedArgs);
    }

    private static @Nullable Pair<String, PlazmaSubCommand> resolveSubCommand(String label) {
        label = label.toLowerCase(Locale.ENGLISH);
        @Nullable PlazmaSubCommand subCommand = SUB_COMMANDS.get(label);

        if (subCommand == null) {
            final @Nullable String command = ALIASES.get(label);
            if (command != null) {
                label = command;
                subCommand = SUB_COMMANDS.get(command);
            }
        }

        if (subCommand != null)
            return Pair.of(label, subCommand);

        return null;
    }

    private static boolean testPermission(final CommandSender sender, final String permission) {
        if (sender.hasPermission("bukkit.command.plazma." + permission) || sender.hasPermission("bukkit.command.plazma"))
            return true;

        sender.sendMessage(Bukkit.permissionMessage());
        return false;
    }
}
