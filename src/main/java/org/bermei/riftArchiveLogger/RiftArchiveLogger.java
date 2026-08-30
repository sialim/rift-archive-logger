package org.bermei.riftArchiveLogger;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class RiftArchiveLogger extends JavaPlugin {
    private ArchiveService archiveService;
    private boolean loggingEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loggingEnabled = getConfig().getBoolean("logging-enabled", false);
        archiveService = new ArchiveService(this);

        getServer().getPluginManager().registerEvents(new ArchiveListener(this), this);

        PluginCommand command = getCommand("riftarchive");
        if (command != null) {
            ArchiveCommand archiveCommand = new ArchiveCommand(this);
            command.setExecutor(archiveCommand);
            command.setTabCompleter(archiveCommand);
        }
    }

    @Override
    public void onDisable() {
        if (archiveService != null) {
            archiveService.shutdown();
        }
    }

    public ArchiveService getArchiveService() {
        return archiveService;
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
        getConfig().set("logging-enabled", enabled);
        saveConfig();
    }
}
