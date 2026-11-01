package com.kitsune.upgradablespawners.listeners;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import com.kitsune.upgradablespawners.data.SpawnerData;
import com.kitsune.upgradablespawners.economy.EconomyProvider;
import com.kitsune.upgradablespawners.gui.SpawnerGUI;
import com.kitsune.upgradablespawners.gui.SpawnerMenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

/**
 * Handles clicks in the spawner upgrade GUI
 */
public class GUIListener implements Listener {

    private final UpgradableSpawners plugin;

    public GUIListener(UpgradableSpawners plugin) {
        this.plugin = plugin;
    }

    /**
     * Describes one upgrade track (radius/speed/quantity) so the three near-identical
     * upgrade paths can share a single handler instead of three copies of the same logic.
     */
    private enum UpgradeTrack {
        RADIUS("radius", "Activation Range", Material.IRON_BLOCK, "Iron Blocks", 4, 100.0,
                SpawnerData::getRadiusLevel, SpawnerData::setRadiusLevel, true),
        SPEED("speed", "Speed", Material.EMERALD_BLOCK, "Emerald Blocks", 2, 150.0,
                SpawnerData::getSpeedLevel, SpawnerData::setSpeedLevel, false),
        QUANTITY("quantity", "Quantity", Material.DIAMOND, "Diamonds", 8, 200.0,
                SpawnerData::getQuantityLevel, SpawnerData::setQuantityLevel, false);

        private final String configKey;
        private final String displayName;
        private final Material costMaterial;
        private final String itemLabel;
        private final int defaultCost;
        private final double defaultMoneyCost;
        private final ToIntFunction<SpawnerData> levelGetter;
        private final ObjIntConsumer<SpawnerData> levelSetter;
        private final boolean refreshSpawnerImmediately;

        UpgradeTrack(String configKey, String displayName, Material costMaterial, String itemLabel, int defaultCost,
                double defaultMoneyCost, ToIntFunction<SpawnerData> levelGetter, ObjIntConsumer<SpawnerData> levelSetter,
                boolean refreshSpawnerImmediately) {
            this.configKey = configKey;
            this.displayName = displayName;
            this.costMaterial = costMaterial;
            this.itemLabel = itemLabel;
            this.defaultCost = defaultCost;
            this.defaultMoneyCost = defaultMoneyCost;
            this.levelGetter = levelGetter;
            this.levelSetter = levelSetter;
            this.refreshSpawnerImmediately = refreshSpawnerImmediately;
        }
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Identify our menu by its holder, not the (cosmetic, spoofable) title. The holder
        // also carries the spawner location, so no separate uuid-to-location map is needed.
        if (!(event.getInventory().getHolder() instanceof SpawnerMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!player.hasPermission("upgradablespawners.upgrade")) {
            player.closeInventory();
            sendMessage(player, plugin.getConfig().getString("messages.no-permission", "&cNo permission!"));
            return;
        }

        Location spawnerLocation = holder.getSpawnerLocation();
        if (spawnerLocation.getWorld() == null || spawnerLocation.getBlock().getType() != Material.SPAWNER) {
            player.closeInventory();
            return;
        }

        if (!SpawnerClickListener.canBuild(plugin, player, spawnerLocation.getBlock())) {
            player.closeInventory();
            sendMessage(player, plugin.getConfig().getString("messages.no-build-access",
                    "&cYou can't upgrade a spawner you don't have build access to!"));
            return;
        }

        SpawnerData data = plugin.getDataManager().getSpawnerData(spawnerLocation);
        int slot = event.getRawSlot();

        switch (slot) {
            case SpawnerGUI.RADIUS_SLOT -> handleUpgrade(player, data, spawnerLocation, UpgradeTrack.RADIUS);
            case SpawnerGUI.SPEED_SLOT -> handleUpgrade(player, data, spawnerLocation, UpgradeTrack.SPEED);
            case SpawnerGUI.QUANTITY_SLOT -> handleUpgrade(player, data, spawnerLocation, UpgradeTrack.QUANTITY);
        }
    }

    private void handleUpgrade(Player player, SpawnerData data, Location spawnerLocation, UpgradeTrack track) {
        int currentLevel = track.levelGetter.applyAsInt(data);
        int maxLevel = Math.max(0, plugin.getConfig().getInt("max-levels." + track.configKey, 5));
        currentLevel = Math.max(0, currentLevel);

        if (currentLevel >= maxLevel) {
            sendMessage(player, plugin.getConfig().getString("messages.upgrade-max", "&eAlready max level!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int nextLevel = currentLevel + 1;

        // Decide how this upgrade is paid for based on the configured economy mode.
        // items = items only, vault = money only, both = money AND items. If money is
        // requested but no economy is hooked, fall back to charging items so upgrades
        // still work.
        String mode = plugin.getEconomyMode();
        EconomyProvider economy = plugin.getEconomy();
        boolean economyReady = economy != null && (mode.equals("vault") || mode.equals("both"));
        boolean chargeMoney = economyReady;
        boolean chargeItems = mode.equals("items") || mode.equals("both") || !economyReady;

        int itemCost = plugin.getConfig().getInt(track.configKey + "-costs." + nextLevel, track.defaultCost);
        if (itemCost <= 0) {
            itemCost = track.defaultCost;
        }
        double moneyCost = plugin.getConfig().getDouble(
                "economy.money-costs." + track.configKey + "." + nextLevel, track.defaultMoneyCost);
        if (!Double.isFinite(moneyCost) || moneyCost <= 0.0) {
            moneyCost = track.defaultMoneyCost;
        }

        // Verify affordability of everything BEFORE deducting anything, so "both" mode
        // never half-charges a player who cannot afford one side of the cost.
        if (chargeMoney && economy.getBalance(player) < moneyCost) {
            String message = plugin.getConfig()
                    .getString("messages.upgrade-fail-money", "&cYou need {amount} to upgrade!")
                    .replace("{amount}", economy.format(moneyCost));
            sendMessage(player, message);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (chargeItems && !hasItems(player, track.costMaterial, itemCost)) {
            String message = plugin.getConfig().getString("messages.upgrade-fail-cost", "&cYou need {amount}x {item}!")
                    .replace("{amount}", String.valueOf(itemCost))
                    .replace("{item}", track.itemLabel);
            sendMessage(player, message);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (chargeMoney) {
            if (!economy.withdraw(player, moneyCost).success()) {
                String message = plugin.getConfig()
                        .getString("messages.upgrade-fail-money", "&cYou need {amount} to upgrade!")
                        .replace("{amount}", economy.format(moneyCost));
                sendMessage(player, message);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }
        if (chargeItems) {
            removeItems(player, track.costMaterial, itemCost);
        }

        track.levelSetter.accept(data, nextLevel);
        plugin.getDataManager().saveSpawnerData(spawnerLocation);

        if (track.refreshSpawnerImmediately) {
            plugin.getDataManager().updateSpawnerSettings(spawnerLocation.getBlock(), data);
        }

        String message = plugin.getConfig().getString("messages.upgrade-success", "&aUpgraded!")
                .replace("{type}", track.displayName)
                .replace("{level}", String.valueOf(nextLevel));
        sendMessage(player, message);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        // Refresh the GUI on next tick to avoid close event race condition
        final Location loc = spawnerLocation.clone();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getSpawnerGUI().openGUI(player, loc);
        }, 1L);
    }

    private boolean hasItems(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    player.getInventory().setItem(i, null);
                    remaining -= itemAmount;
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }

        player.updateInventory();
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
