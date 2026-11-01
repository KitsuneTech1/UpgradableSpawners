package com.kitsune.upgradablespawners.listeners;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import com.kitsune.upgradablespawners.data.SpawnerData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Applies the spawner upgrade effects during spawning
 */
public class SpawnerMechanicsListener implements Listener {

    private final UpgradableSpawners plugin;
    private final Random random;

    public SpawnerMechanicsListener(UpgradableSpawners plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        Block spawnerBlock = event.getSpawner().getBlock();
        Location spawnerLocation = spawnerBlock.getLocation();

        // Check if this spawner has upgrades
        if (!plugin.getDataManager().hasUpgrades(spawnerLocation)) {
            return;
        }

        SpawnerData data = plugin.getDataManager().getSpawnerData(spawnerLocation);
        Entity spawnedEntity = event.getEntity();
        EntityType entityType = spawnedEntity.getType();

        // Proactively update spawner settings to ensure activation range is correct
        plugin.getDataManager().updateSpawnerSettings(spawnerBlock, data);

        // Radius upgrade is now handled by setting RequiredPlayerRange in
        // SpawnerDataManager
        int radiusBlocks = 4; // Default spawn radius for vanilla spawners

        // Apply speed upgrade - modify spawner min/max delay
        int speedLevel = data.getSpeedLevel();
        if (speedLevel > 0) {
            CreatureSpawner spawner = (CreatureSpawner) spawnerBlock.getState();
            long defaultSpeedPercent = Math.min(100L, Math.max(0L, (long) speedLevel * 10L));
            int speedPercent = plugin.getConfig().getInt("speed-effects." + speedLevel,
                    (int) defaultSpeedPercent);
            speedPercent = Math.max(0, Math.min(100, speedPercent));

            // Default spawner delays are 200-800 ticks (10-40 seconds)
            // Reduce both min and max delay by the speed percentage
            int baseMinDelay = 200;
            int baseMaxDelay = 800;

            int newMinDelay = Math.max(10, baseMinDelay - (baseMinDelay * speedPercent / 100));
            int newMaxDelay = Math.max(20, baseMaxDelay - (baseMaxDelay * speedPercent / 100));

            spawner.setMinSpawnDelay(newMinDelay);
            spawner.setMaxSpawnDelay(newMaxDelay);
            spawner.update();
        }

        // Apply quantity upgrade - spawn additional mobs
        int quantityLevel = data.getQuantityLevel();
        if (quantityLevel > 0) {
            int extraMobs = Math.max(0, plugin.getConfig().getInt("quantity-effects." + quantityLevel,
                    quantityLevel));
            int maxMobs = Math.max(0, plugin.getConfig().getInt("max-mobs-per-spawner", 30));

            // Count existing mobs from this spawner
            int nearbyMobs = countNearbyMobs(spawnerLocation, entityType);
            int mobsToSpawn = Math.min(extraMobs, maxMobs - nearbyMobs - 1);

            // Capture values for lambda
            final int finalRadiusBlocks = radiusBlocks;
            final Location finalSpawnerLocation = spawnerLocation.clone();

            if (mobsToSpawn > 0) {
                // Spawn additional mobs on next tick to avoid issues
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (finalSpawnerLocation.getWorld() == null
                                || finalSpawnerLocation.getBlock().getType() != org.bukkit.Material.SPAWNER
                                || !plugin.getDataManager().hasUpgrades(finalSpawnerLocation)
                                || plugin.getDataManager().getSpawnerData(finalSpawnerLocation).getQuantityLevel() <= 0) {
                            return;
                        }
                        for (int i = 0; i < mobsToSpawn; i++) {
                            if (countNearbyMobs(finalSpawnerLocation, entityType) >= maxMobs) {
                                break;
                            }
                            Location spawnLoc = findSpawnLocation(finalSpawnerLocation, finalRadiusBlocks, null);

                            if (spawnLoc != null) {
                                World world = finalSpawnerLocation.getWorld();
                                if (world != null) {
                                    world.spawnEntity(spawnLoc, entityType);
                                }
                            }
                        }
                    }
                }.runTaskLater(plugin, 1L);
            }
        }
    }

    /**
     * Find a valid spawn location within the given radius
     */
    private Location findSpawnLocation(Location center, int radius, Entity entity) {
        World world = center.getWorld();
        if (world == null)
            return null;

        for (int attempts = 0; attempts < 10; attempts++) {
            int x = center.getBlockX() + random.nextInt(radius * 2 + 1) - radius;
            int z = center.getBlockZ() + random.nextInt(radius * 2 + 1) - radius;
            int y = center.getBlockY();

            // Find a suitable Y level (check 3 blocks up and down)
            for (int dy = -3; dy <= 3; dy++) {
                Location loc = new Location(world, x + 0.5, y + dy, z + 0.5);
                Block block = loc.getBlock();
                Block below = block.getRelative(0, -1, 0);
                Block above = block.getRelative(0, 1, 0);

                // Check if there's solid ground and space to spawn
                if (below.getType().isSolid() &&
                        block.getType().isAir() &&
                        above.getType().isAir()) {
                    return loc;
                }
            }
        }

        // No valid air spot found within the radius. Return null so the caller SKIPS this
        // mob rather than spawning it inside a wall or the ground.
        return null;
    }

    /**
     * Count nearby mobs of the same type
     */
    private int countNearbyMobs(Location center, EntityType type) {
        World world = center.getWorld();
        if (world == null)
            return 0;

        int count = 0;
        for (Entity entity : world.getNearbyEntities(center, 32, 16, 32)) {
            if (entity.getType() == type && entity instanceof LivingEntity) {
                count++;
            }
        }
        return count;
    }
}
