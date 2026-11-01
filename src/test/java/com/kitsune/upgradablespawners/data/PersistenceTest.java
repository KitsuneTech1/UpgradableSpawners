package com.kitsune.upgradablespawners.data;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the incremental persistence rework: an upgrade only marks state dirty (it does
 * not synchronously rewrite the whole file), and a flush persists the data across a
 * fresh load (i.e. a restart/reload).
 */
class PersistenceTest {

    private ServerMock server;
    private UpgradableSpawners plugin;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(UpgradableSpawners.class);
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private File dataFile() {
        return new File(plugin.getDataFolder(), "spawners.yml");
    }

    @Test
    void upgradeClickMarksDirtyWithoutWritingWholeFile() {
        SpawnerDataManager manager = plugin.getDataManager();
        Location loc = world.getBlockAt(1, 64, 1).getLocation();
        String key = SpawnerData.createKey(loc);

        SpawnerData data = manager.getSpawnerData(loc);
        data.setRadiusLevel(3);
        manager.saveSpawnerData(loc);

        assertTrue(manager.isDirty(), "an upgrade should leave the manager dirty (pending write)");

        // Reading the on-disk file directly proves the click did NOT synchronously write
        // the whole file: the new level is not on disk yet.
        FileConfiguration onDisk = YamlConfiguration.loadConfiguration(dataFile());
        assertEquals(-1, onDisk.getInt("spawners." + key + ".radius", -1),
                "the upgrade must not be written to disk on the click itself");
    }

    @Test
    void flushPersistsUpgradesAcrossReload() {
        SpawnerDataManager manager = plugin.getDataManager();
        Location loc = world.getBlockAt(2, 64, 2).getLocation();
        String key = SpawnerData.createKey(loc);

        SpawnerData data = manager.getSpawnerData(loc);
        data.setRadiusLevel(2);
        data.setSpeedLevel(4);
        data.setQuantityLevel(1);
        manager.saveSpawnerData(loc);

        manager.flush();
        assertFalse(manager.isDirty(), "flush should clear the dirty flag");

        // It is now on disk.
        FileConfiguration onDisk = YamlConfiguration.loadConfiguration(dataFile());
        assertEquals(2, onDisk.getInt("spawners." + key + ".radius", -1), "radius should be flushed to disk");

        // A fresh manager (simulating a restart/reload) reloads the persisted levels.
        SpawnerDataManager reloaded = new SpawnerDataManager(plugin);
        SpawnerData loaded = reloaded.getSpawnerData(loc);
        assertEquals(2, loaded.getRadiusLevel(), "radius level should survive a reload");
        assertEquals(4, loaded.getSpeedLevel(), "speed level should survive a reload");
        assertEquals(1, loaded.getQuantityLevel(), "quantity level should survive a reload");
    }

    @Test
    void malformedSpawnerSection_isIgnoredInsteadOfCrashingLoad() throws Exception {
        Files.writeString(dataFile().toPath(), "spawners: not-a-map\n");

        SpawnerDataManager reloaded = new SpawnerDataManager(plugin);
        SpawnerData loaded = reloaded.getSpawnerData(world.getBlockAt(3, 64, 3).getLocation());

        assertEquals(0, loaded.getRadiusLevel(), "malformed persisted data should fall back to defaults");
        assertEquals(0, loaded.getSpeedLevel(), "malformed persisted data should fall back to defaults");
        assertEquals(0, loaded.getQuantityLevel(), "malformed persisted data should fall back to defaults");
    }
}
