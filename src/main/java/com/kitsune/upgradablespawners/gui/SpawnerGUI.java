package com.kitsune.upgradablespawners.gui;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import com.kitsune.upgradablespawners.data.SpawnerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates and manages the spawner upgrade GUI
 */
public class SpawnerGUI {

    private final UpgradableSpawners plugin;

    public static final String GUI_TITLE = ChatColor.DARK_PURPLE + "✦ " + ChatColor.LIGHT_PURPLE + "Spawner Upgrades"
            + ChatColor.DARK_PURPLE + " ✦";

    // Slot positions
    public static final int RADIUS_SLOT = 11;
    public static final int SPEED_SLOT = 13;
    public static final int QUANTITY_SLOT = 15;
    public static final int INFO_SLOT = 22;

    public SpawnerGUI(UpgradableSpawners plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the upgrade GUI for a player at a spawner location. The inventory carries a
     * SpawnerMenuHolder so the click handler can identify the menu and read the spawner
     * location straight off the holder, no separate tracking map required.
     */
    public void openGUI(Player player, Location spawnerLocation) {
        SpawnerData data = plugin.getDataManager().getSpawnerData(spawnerLocation);

        SpawnerMenuHolder holder = new SpawnerMenuHolder(spawnerLocation);
        Inventory gui = Bukkit.createInventory(holder, 27, GUI_TITLE);
        holder.setInventory(gui);

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, glass);
        }

        gui.setItem(RADIUS_SLOT, createRadiusItem(data));
        gui.setItem(SPEED_SLOT, createSpeedItem(data));
        gui.setItem(QUANTITY_SLOT, createQuantityItem(data));
        gui.setItem(INFO_SLOT, createInfoItem(data));

        player.openInventory(gui);
    }

    private ItemStack createRadiusItem(SpawnerData data) {
        int level = data.getRadiusLevel();
        int maxLevel = maxLevel("radius");
        boolean isMaxed = level >= maxLevel;

        Material material = isMaxed ? Material.IRON_BLOCK : Material.IRON_INGOT;
        String name = ChatColor.AQUA + "◆ Activation Range Upgrade " + ChatColor.GRAY + "[" + level + "/" + maxLevel
                + "]";

        List<String> lore = new ArrayList<>();
        lore.add("");

        int currentEffect = plugin.getConfig().getInt("radius-effects." + level, 0);
        lore.add(ChatColor.WHITE + "Current: " + ChatColor.YELLOW + "+" + currentEffect + " chunks range");

        if (!isMaxed) {
            int nextLevel = level + 1;
            int cost = positiveCost("radius-costs." + nextLevel, 4);
            int nextEffect = plugin.getConfig().getInt("radius-effects." + nextLevel, 1);

            lore.add(ChatColor.WHITE + "Next: " + ChatColor.GREEN + "+" + nextEffect + " chunks range");
            lore.add("");
            lore.add(ChatColor.GOLD + "Cost: " + ChatColor.WHITE + cost + "x " + ChatColor.GRAY + "Iron Blocks");
            lore.add("");
            lore.add(ChatColor.YELLOW + "▶ Click to upgrade!");
        } else {
            lore.add("");
            lore.add(ChatColor.GREEN + "✓ MAX LEVEL");
        }

        return createItem(material, name, lore);
    }

    private ItemStack createSpeedItem(SpawnerData data) {
        int level = data.getSpeedLevel();
        int maxLevel = maxLevel("speed");
        boolean isMaxed = level >= maxLevel;

        Material material = isMaxed ? Material.EMERALD_BLOCK : Material.EMERALD;
        String name = ChatColor.GREEN + "⚡ Speed Upgrade " + ChatColor.GRAY + "[" + level + "/" + maxLevel + "]";

        List<String> lore = new ArrayList<>();
        lore.add("");

        int currentEffect = plugin.getConfig().getInt("speed-effects." + level, 0);
        lore.add(ChatColor.WHITE + "Current: " + ChatColor.YELLOW + "+" + currentEffect + "% faster");

        if (!isMaxed) {
            int nextLevel = level + 1;
            int cost = positiveCost("speed-costs." + nextLevel, 2);
            int nextEffect = plugin.getConfig().getInt("speed-effects." + nextLevel, 10);

            lore.add(ChatColor.WHITE + "Next: " + ChatColor.GREEN + "+" + nextEffect + "% faster");
            lore.add("");
            lore.add(ChatColor.GOLD + "Cost: " + ChatColor.WHITE + cost + "x " + ChatColor.DARK_GREEN
                    + "Emerald Blocks");
            lore.add("");
            lore.add(ChatColor.YELLOW + "▶ Click to upgrade!");
        } else {
            lore.add("");
            lore.add(ChatColor.GREEN + "✓ MAX LEVEL");
        }

        return createItem(material, name, lore);
    }

    private ItemStack createQuantityItem(SpawnerData data) {
        int level = data.getQuantityLevel();
        int maxLevel = maxLevel("quantity");
        boolean isMaxed = level >= maxLevel;

        Material material = isMaxed ? Material.DIAMOND_BLOCK : Material.DIAMOND;
        String name = ChatColor.LIGHT_PURPLE + "👹 Quantity Upgrade " + ChatColor.GRAY + "[" + level + "/" + maxLevel
                + "]";

        List<String> lore = new ArrayList<>();
        lore.add("");

        int currentEffect = plugin.getConfig().getInt("quantity-effects." + level, 0);
        lore.add(ChatColor.WHITE + "Current: " + ChatColor.YELLOW + "+" + currentEffect + " mobs/spawn");

        if (!isMaxed) {
            int nextLevel = level + 1;
            int cost = positiveCost("quantity-costs." + nextLevel, 8);
            int nextEffect = plugin.getConfig().getInt("quantity-effects." + nextLevel, 1);

            lore.add(ChatColor.WHITE + "Next: " + ChatColor.GREEN + "+" + nextEffect + " mobs/spawn");
            lore.add("");
            lore.add(ChatColor.GOLD + "Cost: " + ChatColor.WHITE + cost + "x " + ChatColor.AQUA + "Diamonds");
            lore.add("");
            lore.add(ChatColor.YELLOW + "▶ Click to upgrade!");
        } else {
            lore.add("");
            lore.add(ChatColor.GREEN + "✓ MAX LEVEL");
        }

        return createItem(material, name, lore);
    }

    private ItemStack createInfoItem(SpawnerData data) {
        String name = ChatColor.GOLD + "ℹ Spawner Info";

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.AQUA + "Activation Range: " + ChatColor.WHITE + "Level " + data.getRadiusLevel());
        lore.add(ChatColor.GREEN + "Speed: " + ChatColor.WHITE + "Level " + data.getSpeedLevel());
        lore.add(ChatColor.LIGHT_PURPLE + "Quantity: " + ChatColor.WHITE + "Level " + data.getQuantityLevel());
        lore.add("");
        lore.add(ChatColor.GRAY + "Right-click upgrades to");
        lore.add(ChatColor.GRAY + "improve this spawner!");

        return createItem(Material.SPAWNER, name, lore);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private int maxLevel(String track) {
        return Math.max(0, plugin.getConfig().getInt("max-levels." + track, 5));
    }

    private int positiveCost(String path, int fallback) {
        int cost = plugin.getConfig().getInt(path, fallback);
        return cost > 0 ? cost : fallback;
    }
}
