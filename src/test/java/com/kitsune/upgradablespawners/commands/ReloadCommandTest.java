package com.kitsune.upgradablespawners.commands;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the /uspawner reload subcommand runs cleanly and re-reads settings.
 */
class ReloadCommandTest {

    private ServerMock server;
    private UpgradableSpawners plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(UpgradableSpawners.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void reloadCommandExecutesWithoutError() {
        // Console sender has all permissions, so this reaches the reload branch.
        boolean handled = server.dispatchCommand(server.getConsoleSender(), "uspawner reload");
        assertTrue(handled, "reload subcommand should be handled");
        // Settings re-read cleanly; default mode is items.
        assertTrue(plugin.getEconomyMode().equals("items"),
                "after a reload with default config, mode should be items");
    }

    @Test
    void infoCommandUsesInfoPermissionForPlayers() {
        assertEquals("upgradablespawners.info", plugin.getCommand("uspawner").getPermission(),
                "the command metadata should use the documented info permission");
    }

    @Test
    void invalidEconomyModeFallsBackToItems() {
        plugin.getConfig().set("economy.mode", "unsupported");
        plugin.reloadSettings();

        assertEquals("items", plugin.getEconomyMode(), "invalid economy modes should fail safely to items");
    }
}
