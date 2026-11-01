package com.kitsune.upgradablespawners.data;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages persistent storage of spawner upgrade data
 */
public class SpawnerDataManager {

    private static final int BASE_ACTIVATION_RANGE = 16;
    private static final int BLOCKS_PER_RADIUS_CHUNK = 16;

    private final UpgradableSpawners plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<String, SpawnerData> spawnerDataMap;

    // Guards concurrent access to dataConfig between the main-thread mutators and the
    // async autosave flush.
    private final Object saveLock = new Object();
    // True when in-memory data has changed but not yet been written to disk.
    private volatile boolean dirty;

    private final NamespacedKey radiusKey;
    private final NamespacedKey speedKey;
    private final NamespacedKey quantityKey;
    private final NamespacedKey entityTypeKey;

    public SpawnerDataManager(UpgradableSpawners plugin) {
        this.plugin = plugin;
        this.spawnerDataMap = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "spawners.yml");

        this.radiusKey = new NamespacedKey(plugin, "spawner_radius");
        this.speedKey = new NamespacedKey(plugin, "spawner_speed");
        this.quantityKey = new NamespacedKey(plugin, "spawner_quantity");
        this.entityTypeKey = new NamespacedKey(plugin, "spawner_entity_type");

        loadAllData();
    }

    /**
     * Load all spawner data from file
     */
    public void loadAllData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create spawners.yml: " + e.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        spawnerDataMap.clear();

        ConfigurationSection spawners = dataConfig.getConfigurationSection("spawners");
        if (spawners != null) {
            for (String key : spawners.getKeys(false)) {
                int radius = dataConfig.getInt("spawners." + key + ".radius", 0);
                int speed = dataConfig.getInt("spawners." + key + ".speed", 0);
                int quantity = dataConfig.getInt("spawners." + key + ".quantity", 0);
                spawnerDataMap.put(key, new SpawnerData(key, radius, speed, quantity));
            }
        } else if (dataConfig.contains("spawners")) {
            plugin.getLogger().warning("Ignoring malformed spawners data: expected a section.");
        }

        plugin.getLogger().info("Loaded " + spawnerDataMap.size() + " upgraded spawners from data.");
    }

    /**
     * Rebuild the whole config from memory and write it to disk. This is O(n) over all
     * spawners, so it is only used on disable (not on every upgrade click).
     */
    public void saveAllData() {
        synchronized (saveLock) {
            dataConfig = new YamlConfiguration();

            for (Map.Entry<String, SpawnerData> entry : spawnerDataMap.entrySet()) {
                SpawnerData data = entry.getValue();
                if (data.hasAnyUpgrades()) {
                    String path = "spawners." + entry.getKey();
                    dataConfig.set(path + ".radius", data.getRadiusLevel());
                    dataConfig.set(path + ".speed", data.getSpeedLevel());
                    dataConfig.set(path + ".quantity", data.getQuantityLevel());
                }
            }

            writeToDisk();
        }
    }

    /**
     * Flush pending changes to disk only when something has actually changed. Called on
     * the async autosave timer, so the common no-change tick does no disk I/O.
     */
    public void flush() {
        synchronized (saveLock) {
            if (!dirty) {
                return;
            }
            writeToDisk();
        }
    }

    /**
     * True when there are in-memory changes not yet written to disk. Mainly for tests
     * and diagnostics: an upgrade click marks the data dirty rather than writing the
     * whole file synchronously.
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Writes the current dataConfig to disk and clears the dirty flag. Callers must hold
     * saveLock.
     */
    private void writeToDisk() {
        try {
            dataConfig.save(dataFile);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save spawners.yml: " + e.getMessage());
        }
    }

    /**
     * Get spawner data for a location, creating if it doesn't exist
     */
    public SpawnerData getSpawnerData(Location location) {
        String key = SpawnerData.createKey(location);
        return spawnerDataMap.computeIfAbsent(key, SpawnerData::new);
    }

    /**
     * Check if a spawner has any upgrades
     */
    public boolean hasUpgrades(Location location) {
        String key = SpawnerData.createKey(location);
        SpawnerData data = spawnerDataMap.get(key);
        return data != null && data.hasAnyUpgrades();
    }

    /**
     * Save a specific spawner's data. Updates only that spawner's keys in the in-memory
     * config and marks the state dirty. It does NOT touch disk: the async autosave (and
     * onDisable) do the actual write, so a click is O(1) in memory instead of an O(n)
     * whole-file rewrite.
     */
    public void saveSpawnerData(Location location) {
        String key = SpawnerData.createKey(location);
        String path = "spawners." + key;
        synchronized (saveLock) {
            SpawnerData data = spawnerDataMap.get(key);
            if (data == null || !data.hasAnyUpgrades()) {
                dataConfig.set(path, null);
            } else {
                dataConfig.set(path + ".radius", data.getRadiusLevel());
                dataConfig.set(path + ".speed", data.getSpeedLevel());
                dataConfig.set(path + ".quantity", data.getQuantityLevel());
            }
            dirty = true;
        }
    }

    /**
     * Remove spawner data (when spawner is broken). Updates memory + marks dirty; the
     * flush happens on the autosave timer / disable, not synchronously here.
     */
    public void removeSpawnerData(Location location) {
        String key = SpawnerData.createKey(location);
        synchronized (saveLock) {
            spawnerDataMap.remove(key);
            dataConfig.set("spawners." + key, null);
            dirty = true;
        }
    }

    /**
     * Create an ItemStack for a spawner with saved upgrade data
     */
    public ItemStack createSpawnerItem(Location location, EntityType type) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        SpawnerData data = getSpawnerData(location);
        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(radiusKey, PersistentDataType.INTEGER, data.getRadiusLevel());
        container.set(speedKey, PersistentDataType.INTEGER, data.getSpeedLevel());
        container.set(quantityKey, PersistentDataType.INTEGER, data.getQuantityLevel());
        container.set(entityTypeKey, PersistentDataType.STRING, type.name());

        String typeName = type.name().replace("_", " ").toLowerCase();
        typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
        meta.setDisplayName("§6" + typeName + " Spawner");

        List<String> lore = new ArrayList<>();
        lore.add("§7Upgraded Spawner");
        if (data.getRadiusLevel() > 0)
            lore.add("§bRadius Level: " + data.getRadiusLevel());
        if (data.getSpeedLevel() > 0)
            lore.add("§bSpeed Level: " + data.getSpeedLevel());
        if (data.getQuantityLevel() > 0)
            lore.add("§bQuantity Level: " + data.getQuantityLevel());

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Apply data from an ItemStack to a newly placed spawner block
     */
    public void applyItemToSpawner(ItemStack item, Block block) {
        if (item == null || item.getType() != Material.SPAWNER || item.getItemMeta() == null)
            return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (!container.has(radiusKey, PersistentDataType.INTEGER) &&
                !container.has(speedKey, PersistentDataType.INTEGER) &&
                !container.has(quantityKey, PersistentDataType.INTEGER)) {
            return;
        }

        int radius = container.getOrDefault(radiusKey, PersistentDataType.INTEGER, 0);
        int speed = container.getOrDefault(speedKey, PersistentDataType.INTEGER, 0);
        int quantity = container.getOrDefault(quantityKey, PersistentDataType.INTEGER, 0);
        String entityTypeName = container.get(entityTypeKey, PersistentDataType.STRING);

        Location loc = block.getLocation();
        SpawnerData data = getSpawnerData(loc);
        data.setRadiusLevel(radius);
        data.setSpeedLevel(speed);
        data.setQuantityLevel(quantity);

        saveSpawnerData(loc);

        // Update physical spawner
        if (block.getState() instanceof CreatureSpawner spawner) {
            if (entityTypeName != null) {
                try {
                    EntityType type = EntityType.valueOf(entityTypeName);
                    spawner.setSpawnedType(type);
                } catch (IllegalArgumentException e) {
                    // Stored entity type name doesn't match a known type (e.g. from an older
                    // Minecraft version); leave the spawner's current mob type unchanged.
                }
            }
            spawner.update(true, true);
            updateSpawnerSettings(block, data);
        }
    }

    /**
     * Updates the physical spawner's settings based on upgrade data
     */
    public void updateSpawnerSettings(Block block, SpawnerData data) {
        if (block == null || block.getType() != Material.SPAWNER)
            return;

        if (block.getState() instanceof CreatureSpawner spawner) {
            int radiusLevel = data.getRadiusLevel();
            int radiusChunks = plugin.getConfig().getInt("radius-effects." + radiusLevel, radiusLevel);

            int activationRange = BASE_ACTIVATION_RANGE + (radiusChunks * BLOCKS_PER_RADIUS_CHUNK);

            spawner.setRequiredPlayerRange(activationRange);
            spawner.update(true, true);
        }
    }
}
