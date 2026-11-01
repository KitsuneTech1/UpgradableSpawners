package com.kitsune.upgradablespawners.listeners;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import com.kitsune.upgradablespawners.data.SpawnerData;
import com.kitsune.upgradablespawners.gui.SpawnerGUI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Locks in the holder-based menu detection (FIX 3): the upgrade GUI is identified by its
 * SpawnerMenuHolder, not by its title. A real menu upgrades; a look-alike inventory that
 * merely shares the title but has no holder is ignored.
 */
class GUIHolderTest {

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

    private Block placeSpawner(int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.SPAWNER);
        CreatureSpawner state = (CreatureSpawner) block.getState();
        state.setSpawnedType(EntityType.ZOMBIE);
        state.update(true, true);
        return block;
    }

    private int countIron(PlayerMock player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.IRON_BLOCK) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @Test
    void realMenu_upgradesViaHolder() {
        Block block = placeSpawner(0, 64, 0);
        Location loc = block.getLocation();

        PlayerMock player = server.addPlayer();
        player.getInventory().addItem(new ItemStack(Material.IRON_BLOCK, 64));

        plugin.getSpawnerGUI().openGUI(player, loc);
        player.simulateInventoryClick(SpawnerGUI.RADIUS_SLOT);

        SpawnerData data = plugin.getDataManager().getSpawnerData(loc);
        assertEquals(1, data.getRadiusLevel(), "clicking the real holder-backed menu should upgrade");
        assertEquals(60, countIron(player), "4 iron blocks should be consumed by the real upgrade");
    }

    @Test
    void lookAlikeTitleWithoutHolder_isIgnored() {
        Block block = placeSpawner(5, 64, 5);
        Location loc = block.getLocation();

        PlayerMock player = server.addPlayer();
        player.getInventory().addItem(new ItemStack(Material.IRON_BLOCK, 64));

        // Same title, but a plain inventory with NO SpawnerMenuHolder (null holder).
        Inventory fake = Bukkit.createInventory(null, 27, SpawnerGUI.GUI_TITLE);
        fake.setItem(SpawnerGUI.RADIUS_SLOT, new ItemStack(Material.IRON_INGOT));
        player.openInventory(fake);
        player.simulateInventoryClick(SpawnerGUI.RADIUS_SLOT);

        SpawnerData data = plugin.getDataManager().getSpawnerData(loc);
        assertEquals(0, data.getRadiusLevel(), "a title-only look-alike inventory must NOT trigger an upgrade");
        assertEquals(64, countIron(player), "no items should be consumed by the look-alike inventory");
    }

    @Test
    void staleMenuAfterSpawnerRemoval_doesNotChargeOrUpgrade() {
        Block block = placeSpawner(10, 64, 10);
        Location loc = block.getLocation();

        PlayerMock player = server.addPlayer();
        player.getInventory().addItem(new ItemStack(Material.IRON_BLOCK, 64));
        plugin.getSpawnerGUI().openGUI(player, loc);
        block.setType(Material.AIR);

        player.simulateInventoryClick(SpawnerGUI.RADIUS_SLOT);

        SpawnerData data = plugin.getDataManager().getSpawnerData(loc);
        assertEquals(0, data.getRadiusLevel(), "a stale menu must not upgrade a removed spawner");
        assertEquals(64, countIron(player), "a stale menu must not consume upgrade items");
    }

    @Test
    void permissionRevokedAfterOpening_doesNotUpgrade() {
        Block block = placeSpawner(15, 64, 15);
        Location loc = block.getLocation();
        PlayerMock player = server.addPlayer();
        player.getInventory().addItem(new ItemStack(Material.IRON_BLOCK, 64));
        player.addAttachment(plugin, "upgradablespawners.upgrade", true);
        plugin.getSpawnerGUI().openGUI(player, loc);
        player.addAttachment(plugin, "upgradablespawners.upgrade", false);

        player.simulateInventoryClick(SpawnerGUI.RADIUS_SLOT);

        assertEquals(0, plugin.getDataManager().getSpawnerData(loc).getRadiusLevel(),
                "the click must re-check upgrade permission");
        assertEquals(64, countIron(player), "a revoked player must not be charged");
    }
}
