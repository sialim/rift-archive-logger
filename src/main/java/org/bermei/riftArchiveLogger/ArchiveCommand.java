package org.bermei.riftArchiveLogger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class ArchiveCommand implements CommandExecutor {
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

        if (args.length < 2 || !args[0].equalsIgnoreCase("logging")) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /riftarchive logging <on|off|toggle>");
            return true;
        }

        String value = args[1].toLowerCase();
        boolean enabled;
        switch (value) {
            case "on", "enable", "enabled" -> enabled = true;
            case "off", "disable", "disabled" -> enabled = false;
            case "toggle" -> enabled = !plugin.isLoggingEnabled();
            default -> {
                sender.sendMessage(ChatColor.YELLOW + "Choose on, off, or toggle.");
                return true;
            }
        }

        plugin.setLoggingEnabled(enabled);
        sender.sendMessage(ChatColor.GREEN + "Archive logging " + (enabled ? "enabled" : "disabled") + ".");
        return true;
    }
}
