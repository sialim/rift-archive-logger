package org.bermei.riftArchiveLogger;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Lectern;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class ArchiveListener implements Listener {
    private final RiftArchiveLogger plugin;

    public ArchiveListener(RiftArchiveLogger plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!plugin.isLoggingEnabled() || !(event.getPlayer() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof Lectern) {
            ItemStack book = inventory.getItem(0);
            if (isBook(book)) {
                plugin.getArchiveService().archiveBook(player, book);
            }
            return;
        }

        if (isArchiveContainer(inventory.getHolder())) {
            plugin.getArchiveService().archiveContainer(player, inventory, event.getView().getTitle());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBookUse(PlayerInteractEvent event) {
        if (!plugin.isLoggingEnabled() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction().isRightClick() && isBook(event.getItem())) {
            plugin.getArchiveService().archiveBook(event.getPlayer(), event.getItem());
        }
    }

    private boolean isArchiveContainer(InventoryHolder holder) {
        return holder instanceof Container
                || holder instanceof ShulkerBox
                || holder instanceof DoubleChest;
    }

    private boolean isBook(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        return type == Material.WRITTEN_BOOK || type == Material.WRITABLE_BOOK;
    }
}
