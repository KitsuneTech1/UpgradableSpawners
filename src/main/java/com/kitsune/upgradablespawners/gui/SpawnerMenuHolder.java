package com.kitsune.upgradablespawners.gui;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Custom InventoryHolder that ties an upgrade menu to the spawner it was opened for.
 *
 * Identifying the menu by its holder instead of a title string is both reliable (titles
 * are cosmetic and spoofable) and lets the click handler read the spawner location
 * straight off the inventory, so no separate uuid-to-location map or close-event cleanup
 * is needed.
 */
public class SpawnerMenuHolder implements InventoryHolder {

    private final Location spawnerLocation;
    private Inventory inventory;

    public SpawnerMenuHolder(Location spawnerLocation) {
        this.spawnerLocation = spawnerLocation;
    }

    /**
     * The spawner this menu upgrades.
     */
    public Location getSpawnerLocation() {
        return spawnerLocation;
    }

    /**
     * Records the inventory created for this holder. Set once, right after
     * Bukkit.createInventory, so getInventory() honors the InventoryHolder contract.
     */
    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
