package org.bermei.riftArchiveLogger;

import org.bukkit.plugin.java.JavaPlugin;

public final class RiftArchiveLogger extends JavaPlugin {
    private ArchiveService archiveService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        archiveService = new ArchiveService(this);

        getServer().getPluginManager().registerEvents(new ArchiveListener(this), this);
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

}
