package com.kitsune.upgradablespawners.listeners;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import com.kitsune.upgradablespawners.gui.SpawnerMenuHolder;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SHOULD A: the build-access guard. Right-clicking a spawner only opens the upgrade GUI
 * when the player could build (break) there. A protection plugin that cancels the dummy
 * break probe blocks the GUI; with no such plugin, the GUI opens.
 */
class BuildAccessTest {

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

    /** A stand-in claim plugin that cancels every block break. */
    private static class DenyBreakListener implements Listener {
        @EventHandler
        public void onBreak(BlockBreakEvent event) {
            event.setCancelled(true);
        }
    }

    private Block placeSpawner() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.SPAWNER);
        CreatureSpawner state = (CreatureSpawner) block.getState();
        state.setSpawnedType(EntityType.ZOMBIE);
        state.update(true, true);
        return block;
    }

    private PlayerMock rightClickingPlayer() {
        PlayerMock player = server.addPlayer();
        // Ensure the upgrade permission regardless of default-perm handling, and an empty
        // main hand so the GUI path (not a block interaction) is taken.
        player.addAttachment(plugin, "upgradablespawners.upgrade", true);
        player.getInventory().setItemInMainHand(null);
        return player;
    }

    private void rightClick(PlayerMock player, Block block) {
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block,
                BlockFace.UP);
        server.getPluginManager().callEvent(event);
    }

    private boolean upgradeGuiOpen(PlayerMock player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        return top != null && top.getHolder() instanceof SpawnerMenuHolder;
    }

    @Test
    void protectedSpawner_guiBlocked() {
        server.getPluginManager().registerEvents(new DenyBreakListener(), plugin);

        Block block = placeSpawner();
        PlayerMock player = rightClickingPlayer();

        rightClick(player, block);

        assertFalse(upgradeGuiOpen(player), "GUI must not open when a plugin cancels the build probe");
    }

    @Test
    void unprotectedSpawner_guiOpens() {
        Block block = placeSpawner();
        PlayerMock player = rightClickingPlayer();

        rightClick(player, block);

        assertTrue(upgradeGuiOpen(player), "GUI should open when the player has build access");
    }

    @Test
    void offHandInteractionWithAnItem_doesNotOpenGui() {
        Block block = placeSpawner();
        PlayerMock player = rightClickingPlayer();
        player.getInventory().setItemInOffHand(new ItemStack(Material.ZOMBIE_SPAWN_EGG));

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block,
                BlockFace.UP, EquipmentSlot.OFF_HAND);
        server.getPluginManager().callEvent(event);

        assertFalse(upgradeGuiOpen(player),
                "an off-hand interaction with an item must not open the upgrade GUI");
    }

    @Test
    void buildAccessRevokedAfterOpening_doesNotUpgrade() {
        Block block = placeSpawner();
        PlayerMock player = rightClickingPlayer();
        rightClick(player, block);
        player.getInventory().addItem(new ItemStack(Material.IRON_BLOCK, 64));

        server.getPluginManager().registerEvents(new DenyBreakListener(), plugin);
        player.simulateInventoryClick(com.kitsune.upgradablespawners.gui.SpawnerGUI.RADIUS_SLOT);

        assertEquals(0, plugin.getDataManager().getSpawnerData(block.getLocation()).getRadiusLevel(),
                "the click must re-check build access");
    }
}
