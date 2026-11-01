package com.kitsune.upgradablespawners;

import com.kitsune.upgradablespawners.data.SpawnerDataManager;
import com.kitsune.upgradablespawners.economy.EconomyProvider;
import com.kitsune.upgradablespawners.gui.SpawnerGUI;
import com.kitsune.upgradablespawners.listeners.GUIListener;
import com.kitsune.upgradablespawners.listeners.SpawnerClickListener;
import com.kitsune.upgradablespawners.listeners.SpawnerMechanicsListener;
import org.bukkit.plugin.java.JavaPlugin;

public class UpgradableSpawners extends JavaPlugin {

    private static UpgradableSpawners instance;
    private SpawnerDataManager dataManager;
    private SpawnerGUI spawnerGUI;

    // Optional Vault economy hook. Null when Vault or an economy provider is absent.
    private EconomyProvider economy;
    // Effective payment mode: items | vault | both. Downgraded to items if money is
    // requested but no economy is available.
    private String economyMode = "items";

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        dataManager = new SpawnerDataManager(this);
        spawnerGUI = new SpawnerGUI(this);

        reloadSettings();

        getServer().getPluginManager().registerEvents(new SpawnerClickListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnerMechanicsListener(this), this);

        getCommand("uspawner").setExecutor(new com.kitsune.upgradablespawners.commands.SpawnerCommand(this));

        setupPlaceholders();

        startAutosaveTask();

        getLogger().info("UpgradableSpawners enabled! Right-click spawners to upgrade them.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            // Full rebuild + write so on-disk data always matches memory at shutdown.
            dataManager.saveAllData();
        }
        getLogger().info("UpgradableSpawners disabled!");
    }

    /**
     * Re-reads config-derived settings (payment mode + economy hook). Safe to call at
     * enable time and again from the reload command.
     */
    public void reloadSettings() {
        economyMode = getConfig().getString("economy.mode", "items").toLowerCase();
        if (!economyMode.equals("items") && !economyMode.equals("vault") && !economyMode.equals("both")) {
            getLogger().warning("Unknown economy.mode '" + economyMode + "'. Falling back to 'items' mode.");
            economyMode = "items";
        }
        economy = null;

        if (economyMode.equals("vault") || economyMode.equals("both")) {
            if (!hookEconomy()) {
                getLogger().warning("economy.mode is '" + economyMode
                        + "' but no Vault economy provider was found. Falling back to 'items' mode.");
                economyMode = "items";
            }
        }
    }

    /**
     * Attempts to resolve a Vault economy provider from the services manager. Guarded
     * against NoClassDefFoundError so a server without Vault installed does not crash.
     */
    private boolean hookEconomy() {
        try {
            economy = com.kitsune.upgradablespawners.economy.VaultEconomyProvider.find(this);
            return economy != null;
        } catch (NoClassDefFoundError e) {
            // Vault classes are not on the runtime classpath (Vault not installed).
            return false;
        }
    }

    /**
     * Registers the PlaceholderAPI expansion when PlaceholderAPI is installed. Guarded by
     * a plugin-presence check plus a NoClassDefFoundError catch so a server without
     * PlaceholderAPI enables cleanly.
     */
    private void setupPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            new com.kitsune.upgradablespawners.placeholder.SpawnersExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        } catch (NoClassDefFoundError e) {
            getLogger().warning("PlaceholderAPI was detected but its classes are unavailable; "
                    + "skipping the placeholder hook.");
        }
    }

    private void startAutosaveTask() {
        long seconds = getConfig().getLong("data.autosave-interval-seconds", 60L);
        if (seconds <= 0) {
            return;
        }
        if (seconds > Long.MAX_VALUE / 20L) {
            getLogger().warning("data.autosave-interval-seconds is too large; autosave is disabled.");
            return;
        }
        long ticks = seconds * 20L;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (dataManager != null) {
                dataManager.flush();
            }
        }, ticks, ticks);
    }

    public static UpgradableSpawners getInstance() {
        return instance;
    }

    public SpawnerDataManager getDataManager() {
        return dataManager;
    }

    public SpawnerGUI getSpawnerGUI() {
        return spawnerGUI;
    }

    /**
     * The Vault economy provider, or null when no economy is hooked.
     */
    public EconomyProvider getEconomy() {
        return economy;
    }

    /**
     * Effective payment mode: items, vault, or both.
     */
    public String getEconomyMode() {
        return economyMode;
    }
}
