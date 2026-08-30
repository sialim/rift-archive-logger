package org.bermei.riftArchiveLogger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Nameable;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.Smoker;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ArchiveService {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RiftArchiveLogger plugin;
    private final Object fileLock = new Object();

    public ArchiveService(RiftArchiveLogger plugin) {
        this.plugin = plugin;
    }

    public void archiveBook(Player player, ItemStack book) {
        if (!(book.getItemMeta() instanceof BookMeta meta)) {
            return;
        }

        String title = meta.hasTitle() ? meta.getTitle() : "Untitled Book";
        String author = meta.hasAuthor() ? meta.getAuthor() : "Unknown Author";
        String type = book.getType() == Material.WRITTEN_BOOK ? "Published Book" : "Writable Book";
        List<String> pages = meta.hasPages() ? meta.getPages() : List.of();
        String fileTitle = bookFileTitle(book, meta);

        StringBuilder output = new StringBuilder();
        output.append(title).append(" | ").append(author).append(System.lineSeparator());
        output.append("Type: ").append(type).append(System.lineSeparator()).append(System.lineSeparator());
        for (int index = 0; index < pages.size(); index++) {
            output.append("Page ").append(index + 1).append(System.lineSeparator());
            output.append(pages.get(index)).append(System.lineSeparator()).append(System.lineSeparator());
        }

        Path directory = plugin.getDataFolder().toPath().resolve(plugin.getConfig().getString("books-directory", "books"));
        writeBookAsync(directory, safeFileName(fileTitle) + ".txt", output.toString(), player);
    }

    private String bookFileTitle(ItemStack book, BookMeta meta) {
        String originalTitle = meta.hasTitle() ? meta.getTitle() : originalBookName(book.getType());
        ItemMeta itemMeta = book.getItemMeta();
        String renamedTitle = null;
        if (itemMeta != null) {
            if (itemMeta.hasDisplayName() && itemMeta.getDisplayName() != null && !itemMeta.getDisplayName().isBlank()) {
                renamedTitle = itemMeta.getDisplayName();
            } else if (itemMeta.hasItemName() && itemMeta.getItemName() != null && !itemMeta.getItemName().isBlank()) {
                renamedTitle = itemMeta.getItemName();
            }
        }

        if (renamedTitle == null || renamedTitle.equalsIgnoreCase(originalTitle)) {
            return originalTitle;
        }
        return originalTitle + " (" + renamedTitle + ")";
    }

    private String originalBookName(Material material) {
        return material == Material.WRITABLE_BOOK ? "Book & Quill" : "Written Book";
    }

    public void archiveContainer(Player player, Inventory inventory, String viewTitle) {
        ContainerInfo info = ContainerInfo.from(inventory.getHolder(), inventory, viewTitle);
        String name = resolveContainerName(info);
        LocalDateTime now = LocalDateTime.now();

        StringBuilder output = new StringBuilder();
        output.append("# Container : ").append(name).append(System.lineSeparator());
        output.append("# Type      : ").append(info.type()).append(System.lineSeparator());
        output.append("# Logged by : ").append(player.getName()).append(System.lineSeparator());
        output.append("# Time      : ").append(DISPLAY_TIME.format(now)).append(System.lineSeparator());
        output.append("# Items     : ").append(countItems(inventory)).append(System.lineSeparator()).append(System.lineSeparator());

        ItemStack[] contents = inventory.getContents();
        int itemNumber = 0;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) {
                continue;
            }
            itemNumber++;
            output.append("=== Item ").append(itemNumber).append("  (slot ").append(slot).append(") ===").append(System.lineSeparator());
            output.append(formatItem(item)).append(System.lineSeparator());
        }

        String timestampedName = name + " - " + FILE_TIME.format(now) + ".txt";
        Path directory = plugin.getDataFolder().toPath().resolve(plugin.getConfig().getString("containers-directory", "containers"));
        writeAsync(directory, timestampedName, output.toString(), true, player, "container");
    }

    private String resolveContainerName(ContainerInfo info) {
        if (info.customName() != null && !info.customName().isBlank()) {
            return info.customName();
        }

        if (info.location() == null) {
            return info.baseName() + "1";
        }

        String key = "container-names." + locationKey(info.location());
        String existing = plugin.getConfig().getString(key);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }

        int next = plugin.getConfig().getInt("next-container-number." + info.baseName(), 1);
        String generated = info.baseName() + next;
        plugin.getConfig().set(key, generated);
        plugin.getConfig().set("next-container-number." + info.baseName(), next + 1);
        plugin.saveConfig();
        return generated;
    }

    private String locationKey(Location location) {
        return location.getWorld().getUID() + "-" + location.getBlockX() + "-" + location.getBlockY() + "-" + location.getBlockZ();
    }

    private int countItems(Inventory inventory) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir()) {
                count++;
            }
        }
        return count;
    }

    private String formatItem(ItemStack item) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("item", item);
        return yaml.saveToString();
    }

    private void writeAsync(Path directory, String fileName, String contents, boolean avoidOverwrite,
                            Player player, String archiveType) {
        String safeName = safeFileName(fileName);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (fileLock) {
                try {
                    Files.createDirectories(directory);
                    Path target = directory.resolve(safeName).normalize();
                    if (!target.startsWith(directory.normalize())) {
                        throw new IOException("Archive filename escaped the archive directory");
                    }
                    if (avoidOverwrite) {
                        target = nextAvailable(target);
                    }
                    Files.writeString(target, contents, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                    reportSuccess(player, archiveType, safeName, target.getFileName().toString());
                } catch (IOException exception) {
                    reportFailure(player, archiveType, exception);
                }
            }
        });
    }

    private void writeBookAsync(Path directory, String fileName, String contents, Player player) {
        String safeName = safeFileName(fileName);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (fileLock) {
                try {
                    Files.createDirectories(directory);
                    Path target = findMatchingBook(directory, safeName, contents);
                    if (!target.startsWith(directory.normalize())) {
                        throw new IOException("Archive filename escaped the archive directory");
                    }
                    Files.writeString(target, contents, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                    reportSuccess(player, "book", safeName, target.getFileName().toString());
                } catch (IOException exception) {
                    reportFailure(player, "book", exception);
                }
            }
        });
    }

    private void reportSuccess(Player player, String archiveType, String requestedName, String savedName) {
        boolean renamed = !requestedName.equals(savedName);
        String notice = renamed
                ? ChatColor.GREEN + "Archived " + archiveType + ": " + ChatColor.WHITE + savedName
                + ChatColor.YELLOW + " (duplicate filename " + ChatColor.WHITE + requestedName
                + ChatColor.YELLOW + "; saved as " + ChatColor.WHITE + savedName + ChatColor.YELLOW + ")."
                : ChatColor.GREEN + "Archived " + archiveType + ": " + ChatColor.WHITE + savedName;
        plugin.getServer().getScheduler().runTask(plugin, () -> player.sendMessage(notice));
    }

    private void reportFailure(Player player, String archiveType, IOException exception) {
        String detail = exception.getMessage() == null ? "unknown file error" : exception.getMessage();
        String notice = ChatColor.RED + "Could not archive " + archiveType + ": " + detail;
        plugin.getServer().getScheduler().runTask(plugin, () -> player.sendMessage(notice));
    }

    private Path findMatchingBook(Path directory, String fileName, String contents) throws IOException {
        int extension = fileName.lastIndexOf('.');
        String base = extension >= 0 ? fileName.substring(0, extension) : fileName;
        String suffix = extension >= 0 ? fileName.substring(extension) : "";
        int number = 1;

        while (true) {
            String candidateName = number == 1 ? base + suffix : base + " - " + number + suffix;
            Path candidate = directory.resolve(candidateName).normalize();
            if (!Files.exists(candidate)) {
                return candidate;
            }
            if (Files.readString(candidate, StandardCharsets.UTF_8).equals(contents)) {
                return candidate;
            }
            number++;
        }
    }

    private Path nextAvailable(Path original) {
        if (!Files.exists(original)) {
            return original;
        }

        String fileName = original.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        String base = extension >= 0 ? fileName.substring(0, extension) : fileName;
        String suffix = extension >= 0 ? fileName.substring(extension) : "";
        int number = 2;
        Path candidate;
        do {
            candidate = original.resolveSibling(base + "-" + number + suffix);
            number++;
        } while (Files.exists(candidate));
        return candidate;
    }

    private String safeFileName(String value) {
        String stripped = ChatColor.stripColor(value == null ? "" : value).trim();
        String safe = stripped.replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "_");
        safe = safe.replaceAll("\\s+", " ").trim();
        if (safe.isBlank() || safe.equals(".") || safe.equals("..")) {
            return "untitled";
        }
        return safe.length() > 120 ? safe.substring(0, 120).trim() : safe;
    }

    public void shutdown() {
    }

    private record ContainerInfo(String type, String baseName, String customName, Location location) {
        private static ContainerInfo from(InventoryHolder holder, Inventory inventory, String viewTitle) {
            String type = inventory.getType().name();
            String baseName = titleCase(type);
            String customName = null;
            Location location = null;

            if (holder instanceof DoubleChest doubleChest) {
                type = "Double Chest";
                baseName = "Chest";
                location = locationOf(doubleChest.getLeftSide());
                customName = customNameOf(doubleChest.getLeftSide());
            } else if (holder instanceof ShulkerBox shulkerBox) {
                type = shulkerBox.getColor() == null ? "Shulker Box" : shulkerBox.getColor().name() + " Shulker Box";
                baseName = "Shulker";
                location = shulkerBox.getLocation();
                customName = customNameOf(shulkerBox);
            } else if (holder instanceof BlockState state) {
                type = readableType(state, type);
                baseName = baseName(type);
                location = state.getLocation();
                customName = customNameOf(state);
            }

            return new ContainerInfo(type, baseName, customName, location);
        }

        private static String readableType(BlockState state, String fallback) {
            if (state instanceof Chest) return "Chest";
            if (state instanceof Barrel) return "Barrel";
            if (state instanceof Hopper) return "Hopper";
            if (state instanceof Dispenser) return "Dispenser";
            if (state instanceof Dropper) return "Dropper";
            if (state instanceof Furnace) return "Furnace";
            if (state instanceof BlastFurnace) return "Blast Furnace";
            if (state instanceof Smoker) return "Smoker";
            if (state instanceof BrewingStand) return "Brewing Stand";
            return titleCase(fallback);
        }

        private static String baseName(String type) {
            if (type.equals("Double Chest")) return "Chest";
            if (type.endsWith(" Box")) return type.substring(0, type.length() - 4);
            return type;
        }

        private static String customNameOf(Object value) {
            if (value instanceof Nameable nameable && nameable.getCustomName() != null) {
                return ChatColor.stripColor(nameable.getCustomName());
            }
            return null;
        }

        private static Location locationOf(InventoryHolder holder) {
            return holder instanceof BlockState state ? state.getLocation() : null;
        }

        private static String titleCase(String value) {
            String[] words = value.toLowerCase().split("_");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (word.isBlank()) continue;
                if (!result.isEmpty()) result.append(' ');
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
            return result.toString();
        }
    }
}
