package org.bermei.riftArchiveLogger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public final class ArchiveCommand implements CommandExecutor, TabCompleter {
    private final RiftArchiveLogger plugin;

    public ArchiveCommand(RiftArchiveLogger plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("riftarchive.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Archive logging is "
                    + (plugin.isLoggingEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.YELLOW + ".");
            sender.sendMessage(ChatColor.YELLOW + "Debug logging is "
                    + (plugin.isDebugEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.YELLOW + ".");
            sender.sendMessage(ChatColor.GRAY + "Usage: /riftarchive logging <on|off|toggle>, /riftarchive debug <on|off|toggle>, or /riftarchive reload");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "logging" -> handleLogging(sender, args);
            case "debug" -> handleDebug(sender, args);
            case "reload" -> {
                plugin.reloadConfig();
                plugin.setLoggingEnabled(plugin.getConfig().getBoolean("logging-enabled", false));
                plugin.setDebugEnabled(plugin.getConfig().getBoolean("debug-logging", false));
                sender.sendMessage(ChatColor.GREEN + "Rift Archive Logger configuration reloaded.");
            }
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /riftarchive logging <on|off|toggle>, /riftarchive debug <on|off|toggle>, or /riftarchive reload");
        }
        return true;
    }

    private void handleLogging(CommandSender sender, String[] args) {
        String value = args.length > 1 ? args[1].toLowerCase() : "toggle";
        boolean enabled;
        switch (value) {
            case "on", "enable", "enabled" -> enabled = true;
            case "off", "disable", "disabled" -> enabled = false;
            case "toggle" -> enabled = !plugin.isLoggingEnabled();
            default -> {
                sender.sendMessage(ChatColor.YELLOW + "Choose on, off, or toggle.");
                return;
            }
        }

        plugin.setLoggingEnabled(enabled);
        sender.sendMessage(ChatColor.GREEN + "Archive logging " + (enabled ? "enabled" : "disabled") + ".");
    }

    private void handleDebug(CommandSender sender, String[] args) {
        String value = args.length > 1 ? args[1].toLowerCase() : "toggle";
        boolean enabled;
        switch (value) {
            case "on", "enable", "enabled" -> enabled = true;
            case "off", "disable", "disabled" -> enabled = false;
            case "toggle" -> enabled = !plugin.isDebugEnabled();
            default -> {
                sender.sendMessage(ChatColor.YELLOW + "Choose on, off, or toggle.");
                return;
            }
        }

        plugin.setDebugEnabled(enabled);
        sender.sendMessage(ChatColor.GREEN + "Debug logging " + (enabled ? "enabled" : "disabled") + ".");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partial(args[0], List.of("logging", "debug", "reload"));
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("logging") || args[0].equalsIgnoreCase("debug"))) {
            return partial(args[1], List.of("on", "off", "toggle"));
        }
        return List.of();
    }

    private List<String> partial(String input, List<String> values) {
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.startsWith(input.toLowerCase())) {
                matches.add(value);
            }
        }
        return matches;
    }
}
