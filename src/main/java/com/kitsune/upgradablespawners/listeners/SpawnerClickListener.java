package com.kitsune.upgradablespawners.listeners;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

/**
 * Handles right-clicking on spawners to open the upgrade GUI
 */
public class SpawnerClickListener implements Listener {

    private final UpgradableSpawners plugin;

    // True only while we are firing a dummy BlockBreakEvent to probe build access. Guards
    // our own onSpawnerBreak from acting on that probe (which would wipe the spawner data).
    private boolean probingBuildAccess = false;

    public SpawnerClickListener(UpgradableSpawners plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawnerRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) {
            return;
        }

        Player player = event.getPlayer();

        // ONLY open the GUI if the player's main hand is empty
        // This allows spawn eggs and other block interactions to work normally
        if (!player.getInventory().getItemInMainHand().getType().isAir()) {
            return;
        }

        if (!player.hasPermission("upgradablespawners.upgrade")) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cNo permission!");
            player.sendMessage(message.replace("&", "§"));
            return;
        }

        // Cancel the default spawner interaction (since we are opening our GUI)
        event.setCancelled(true);

        // Optional claim/ownership guard: only let a player upgrade a spawner they could
        // actually break. We ask by firing a dummy BlockBreakEvent; if a protection plugin
        // cancels it, the player has no build access here, so deny.
        if (!canBuild(player, block)) {
            String message = plugin.getConfig().getString("messages.no-build-access",
                    "&cYou can't upgrade a spawner you don't have build access to!");
            player.sendMessage(message.replace("&", "§"));
            return;
        }

        plugin.getSpawnerGUI().openGUI(player, block.getLocation());

        // Re-sync spawner settings in case they drifted from stored data
        plugin.getDataManager().updateSpawnerSettings(block,
                plugin.getDataManager().getSpawnerData(block.getLocation()));
    }

    /**
     * True when the player is allowed to build (break) at the spawner block. When the
     * require-build-access config is off this always returns true; otherwise it fires a
     * dummy BlockBreakEvent and treats a cancel from any other plugin as "no access".
     */
    private boolean canBuild(Player player, Block block) {
        if (!plugin.getConfig().getBoolean("require-build-access", true)) {
            return true;
        }

        BlockBreakEvent probe = new BlockBreakEvent(block, player);
        probingBuildAccess = true;
        try {
            Bukkit.getPluginManager().callEvent(probe);
        } finally {
            probingBuildAccess = false;
        }
        return !probe.isCancelled();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerBreak(BlockBreakEvent event) {
        // Ignore the dummy event we fire to probe build access, otherwise the probe would
        // wipe the spawner's upgrade data (and possibly drop its item) without the block
        // ever actually being broken.
        if (probingBuildAccess) {
            return;
        }

        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) {
            return;
        }

        if (plugin.getDataManager().hasUpgrades(block.getLocation())) {
            if (block.getState() instanceof CreatureSpawner spawner) {
                // Vanilla only drops a spawner when the tool has Silk Touch. Match that so
                // players cannot break-and-relocate (or dupe) upgraded spawners for free.
                boolean silkTouchRequired = plugin.getConfig().getBoolean("silk-touch-required", true);
                boolean hasSilkTouch = hasSilkTouch(event.getPlayer().getInventory().getItemInMainHand());

                if (hasSilkTouch || !silkTouchRequired) {
                    ItemStack upgradedItem = plugin.getDataManager().createSpawnerItem(block.getLocation(),
                            spawner.getSpawnedType());
                    block.getWorld().dropItemNaturally(block.getLocation(), upgradedItem);
                }

                // Prevent the default drop AND other plugins from dropping a spawner.
                // We handle the (silk-touch) drop ourselves above, so vanilla must stay silent
                // whether or not we dropped anything.
                event.setDropItems(false);
                event.setExpToDrop(0);
            }
        }

        plugin.getDataManager().removeSpawnerData(block.getLocation());
    }

    /**
     * True when the given tool carries the Silk Touch enchantment.
     */
    private boolean hasSilkTouch(ItemStack tool) {
        return tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) {
            return;
        }

        ItemStack item = event.getItemInHand();
        plugin.getDataManager().applyItemToSpawner(item, block);
    }
}
