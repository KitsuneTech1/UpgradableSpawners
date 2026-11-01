package com.kitsune.upgradablespawners.listeners;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import com.kitsune.upgradablespawners.data.SpawnerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the break-exploit: an upgraded spawner used to always drop a working
 * upgraded-spawner item on break, even without Silk Touch. Vanilla only drops
 * spawners with Silk Touch, so this let players relocate/dupe spawners for free.
 */
class SpawnerBreakTest {

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

    /**
     * Places an upgraded spawner and returns its block.
     */
    private Block placeUpgradedSpawner(int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.SPAWNER);

        CreatureSpawner state = (CreatureSpawner) block.getState();
        state.setSpawnedType(EntityType.ZOMBIE);
        state.update(true, true);

        // Register upgrade data for this location via the plugin's data manager.
        Location loc = block.getLocation();
        SpawnerData data = plugin.getDataManager().getSpawnerData(loc);
        data.setRadiusLevel(2);
        data.setSpeedLevel(1);
        data.setQuantityLevel(3);

        assertTrue(plugin.getDataManager().hasUpgrades(loc), "upgrade data should be registered");
        return block;
    }

    /**
     * Counts SPAWNER item entities currently dropped in the world.
     */
    private List<Item> droppedSpawnerItems() {
        return world.getEntities().stream()
                .filter(e -> e instanceof Item)
                .map(e -> (Item) e)
                .filter(i -> i.getItemStack().getType() == Material.SPAWNER)
                .toList();
    }

    @Test
    void breakWithoutSilkTouch_dropsNothing() {
        Block block = placeUpgradedSpawner(0, 64, 0);

        PlayerMock player = server.addPlayer();
        ItemStack plainPick = new ItemStack(Material.DIAMOND_PICKAXE);
        player.getInventory().setItemInMainHand(plainPick);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        server.getPluginManager().callEvent(event);

        assertEquals(0, droppedSpawnerItems().size(),
                "no spawner should drop when broken without Silk Touch");
        // Upgrade data is still removed either way.
        assertTrue(!plugin.getDataManager().hasUpgrades(block.getLocation()),
                "upgrade data should be removed after break");
    }

    @Test
    void breakWithSilkTouch_dropsUpgradedSpawner() {
        Block block = placeUpgradedSpawner(0, 64, 0);
        Location loc = block.getLocation();

        PlayerMock player = server.addPlayer();
        ItemStack silkPick = new ItemStack(Material.DIAMOND_PICKAXE);
        silkPick.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
        player.getInventory().setItemInMainHand(silkPick);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        server.getPluginManager().callEvent(event);

        List<Item> drops = droppedSpawnerItems();
        assertEquals(1, drops.size(), "exactly one spawner should drop with Silk Touch");

        ItemStack dropped = drops.get(0).getItemStack();
        PersistentDataContainer container = dropped.getItemMeta().getPersistentDataContainer();

        NamespacedKey radiusKey = new NamespacedKey(plugin, "spawner_radius");
        NamespacedKey speedKey = new NamespacedKey(plugin, "spawner_speed");
        NamespacedKey quantityKey = new NamespacedKey(plugin, "spawner_quantity");

        Integer radius = container.get(radiusKey, PersistentDataType.INTEGER);
        Integer speed = container.get(speedKey, PersistentDataType.INTEGER);
        Integer quantity = container.get(quantityKey, PersistentDataType.INTEGER);

        assertNotNull(radius, "dropped spawner should carry radius level");
        assertEquals(2, radius, "radius level should survive the drop");
        assertEquals(1, speed, "speed level should survive the drop");
        assertEquals(3, quantity, "quantity level should survive the drop");

        // Upgrade data is removed from the manager once the block is broken.
        assertTrue(!plugin.getDataManager().hasUpgrades(loc),
                "upgrade data should be removed after break");
    }

    @Test
    void cancelledPlace_doesNotRestoreUpgradeData() {
        Block source = placeUpgradedSpawner(0, 64, 0);
        ItemStack item = plugin.getDataManager().createSpawnerItem(source.getLocation(), EntityType.ZOMBIE);

        Block target = world.getBlockAt(5, 64, 5);
        target.setType(Material.SPAWNER);
        PlayerMock player = server.addPlayer();
        BlockPlaceEvent event = new BlockPlaceEvent(target, target.getState(), target.getRelative(BlockFace.DOWN),
                item, player, true);
        event.setCancelled(true);
        server.getPluginManager().callEvent(event);

        assertTrue(!plugin.getDataManager().hasUpgrades(target.getLocation()),
                "a cancelled placement must not restore upgrade data");
    }
}
