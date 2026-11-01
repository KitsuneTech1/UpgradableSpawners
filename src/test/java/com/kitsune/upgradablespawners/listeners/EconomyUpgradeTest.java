package com.kitsune.upgradablespawners.listeners;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import com.kitsune.upgradablespawners.data.SpawnerData;
import com.kitsune.upgradablespawners.gui.SpawnerGUI;
import com.kitsune.upgradablespawners.testutil.FakeEconomy;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises the Vault economy payment mode: money is deducted on upgrade, an upgrade is
 * blocked when the player cannot afford it, and the classic items mode still works.
 */
class EconomyUpgradeTest {

    private ServerMock server;
    private UpgradableSpawners plugin;
    private WorldMock world;
    private FakeEconomy economy;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        // A registrant plugin is needed to register the economy service before our
        // plugin loads.
        PluginMock provider = MockBukkit.createMockPlugin("EconomyProvider");
        economy = new FakeEconomy();
        server.getServicesManager().register(Economy.class, economy, provider, ServicePriority.Normal);

        plugin = MockBukkit.load(UpgradableSpawners.class);
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void useVaultMode() {
        plugin.getConfig().set("economy.mode", "vault");
        plugin.reloadSettings();
        assertEquals("vault", plugin.getEconomyMode(), "vault mode should stay active once economy is hooked");
    }

    private Block placeSpawner(int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.SPAWNER);
        CreatureSpawner state = (CreatureSpawner) block.getState();
        state.setSpawnedType(EntityType.ZOMBIE);
        state.update(true, true);
        return block;
    }

    private void clickRadius(PlayerMock player, Location loc) {
        plugin.getSpawnerGUI().openGUI(player, loc);
        player.simulateInventoryClick(SpawnerGUI.RADIUS_SLOT);
    }

    @Test
    void vaultMode_deductsMoneyAndUpgrades() {
        useVaultMode();
        Block block = placeSpawner(0, 64, 0);
        Location loc = block.getLocation();

        PlayerMock player = server.addPlayer();
        economy.setBalance(player, 10_000.0);

        clickRadius(player, loc);

        SpawnerData data = plugin.getDataManager().getSpawnerData(loc);
        assertEquals(1, data.getRadiusLevel(), "radius should upgrade to level 1 when paid with money");
        // radius level 1 money cost from config is 100.0
        assertEquals(9_900.0, economy.getBalance(player), 0.0001, "money cost should be withdrawn");
    }

    @Test
    void vaultMode_insufficientFundsBlocksUpgrade() {
        useVaultMode();
        Block block = placeSpawner(0, 64, 0);
        Location loc = block.getLocation();

        PlayerMock player = server.addPlayer();
        economy.setBalance(player, 10.0); // less than the 100.0 radius level-1 cost

        clickRadius(player, loc);

        SpawnerData data = plugin.getDataManager().getSpawnerData(loc);
        assertEquals(0, data.getRadiusLevel(), "radius should NOT upgrade without enough money");
        assertEquals(10.0, economy.getBalance(player), 0.0001, "no money should be withdrawn on a failed upgrade");
    }

    @Test
    void vaultMode_failedWithdrawalDoesNotUpgrade() {
        useVaultMode();
        Block block = placeSpawner(0, 64, 0);
        Location loc = block.getLocation();

        PlayerMock player = server.addPlayer();
        economy.setBalance(player, 10_000.0);
        economy.setWithdrawFailure(true);

        clickRadius(player, loc);

        SpawnerData data = plugin.getDataManager().getSpawnerData(loc);
        assertEquals(0, data.getRadiusLevel(), "a failed economy transaction must not upgrade the spawner");
        assertEquals(10_000.0, economy.getBalance(player), 0.0001,
                "a failed economy transaction must not charge the player");
    }

    @Test
    void itemsMode_stillChargesItems() {
        // Default config mode is items; economy is hooked but must not be used.
        assertEquals("items", plugin.getEconomyMode(), "default mode should be items");

        Block block = placeSpawner(0, 64, 0);
        Location loc = block.getLocation();

        PlayerMock player = server.addPlayer();
        economy.setBalance(player, 10_000.0);
        player.getInventory().addItem(new ItemStack(Material.IRON_BLOCK, 64));

        clickRadius(player, loc);

        SpawnerData data = plugin.getDataManager().getSpawnerData(loc);
        assertEquals(1, data.getRadiusLevel(), "radius should upgrade when paid with items");

        int ironLeft = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.IRON_BLOCK) {
                ironLeft += item.getAmount();
            }
        }
        // radius level 1 costs 4 iron blocks
        assertEquals(60, ironLeft, "4 iron blocks should be consumed in items mode");
        assertEquals(10_000.0, economy.getBalance(player), 0.0001, "no money should be touched in items mode");
    }
}
