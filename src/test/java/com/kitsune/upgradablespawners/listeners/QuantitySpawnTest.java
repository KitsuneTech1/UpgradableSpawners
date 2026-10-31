package com.kitsune.upgradablespawners.listeners;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import com.kitsune.upgradablespawners.data.SpawnerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FIX 4: quantity-upgrade mobs must never be forced into a wall/ground. In a sealed area
 * with no valid air spot, no extra mobs spawn at all; with open air over a solid floor,
 * the extra mobs do spawn and land on valid air locations within the spawn radius.
 */
class QuantitySpawnTest {

    private static final int CX = 0;
    private static final int CY = 64;
    private static final int CZ = 0;
    // findSpawnLocation searches a hardcoded radius of 4 around the spawner.
    private static final int SPAWN_RADIUS = 4;

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

    private void fill(int y0, int y1, Material material) {
        for (int x = CX - 6; x <= CX + 6; x++) {
            for (int z = CZ - 6; z <= CZ + 6; z++) {
                for (int y = y0; y <= y1; y++) {
                    world.getBlockAt(x, y, z).setType(material);
                }
            }
        }
    }

    /**
     * Places an upgraded spawner (quantity level 3) and fires a SpawnerSpawnEvent for one
     * naturally spawned zombie. Returns every EXTRA zombie the listener added (the trigger
     * zombie is excluded by identity, since it sits on the spawner block itself).
     */
    private List<Entity> runSpawnAndCollectExtras() {
        Block block = world.getBlockAt(CX, CY, CZ);
        block.setType(Material.SPAWNER);
        CreatureSpawner state = (CreatureSpawner) block.getState();
        state.setSpawnedType(EntityType.ZOMBIE);
        state.update(true, true);

        Location loc = block.getLocation();
        SpawnerData data = plugin.getDataManager().getSpawnerData(loc);
        data.setQuantityLevel(3); // quantity-effects.3 = 3 extra mobs

        assertTrue(plugin.getDataManager().hasUpgrades(loc), "spawner should be flagged as upgraded");

        // The one "natural" spawn that triggers the event.
        Entity trigger = world.spawnEntity(new Location(world, CX + 0.5, CY, CZ + 0.5), EntityType.ZOMBIE);

        SpawnerSpawnEvent event = new SpawnerSpawnEvent(trigger, state);
        server.getPluginManager().callEvent(event);

        // Extra mobs are scheduled one tick later.
        server.getScheduler().performTicks(3L);

        List<Entity> extras = new ArrayList<>();
        for (Entity e : zombies()) {
            if (e != trigger) {
                extras.add(e);
            }
        }
        return extras;
    }

    private List<Entity> zombies() {
        List<Entity> out = new ArrayList<>();
        for (Entity e : world.getEntities()) {
            if (e.getType() == EntityType.ZOMBIE && e instanceof LivingEntity) {
                out.add(e);
            }
        }
        return out;
    }

    @Test
    void enclosedArea_spawnsNoExtraMobs() {
        // Solid everywhere in the search volume: no valid air spot exists.
        fill(CY - 6, CY + 6, Material.STONE);
        // Restore the spawner block itself (fill overwrote it).
        world.getBlockAt(CX, CY, CZ).setType(Material.SPAWNER);
        CreatureSpawner state = (CreatureSpawner) world.getBlockAt(CX, CY, CZ).getState();
        state.setSpawnedType(EntityType.ZOMBIE);
        state.update(true, true);

        int extra = runSpawnAndCollectExtras().size();
        assertEquals(0, extra, "no extra mobs may spawn when there is no valid air location (no wall-spawning)");
    }

    @Test
    void openArea_spawnsExtraMobsAtValidAirLocations() {
        // Solid floor one below the spawner, open air above it.
        fill(CY - 1, CY - 1, Material.STONE);
        fill(CY, CY + 5, Material.AIR);

        List<Entity> extras = runSpawnAndCollectExtras();

        int maxMobs = plugin.getConfig().getInt("max-mobs-per-spawner", 30);
        assertTrue(extras.size() > 0, "extra mobs should spawn when open air over a solid floor is available");
        assertTrue(extras.size() <= maxMobs, "extra mobs must not exceed the per-spawner cap");

        // Each extra mob must sit on a valid air block within the spawn radius.
        for (Entity zombie : extras) {
            Location zl = zombie.getLocation();
            Block at = zl.getBlock();
            assertTrue(at.getType().isAir(), "each extra mob should be placed at an air block");
            assertTrue(Math.abs(zl.getBlockX() - CX) <= SPAWN_RADIUS
                    && Math.abs(zl.getBlockZ() - CZ) <= SPAWN_RADIUS,
                    "each extra mob should be within the spawn radius");
        }
    }
}
