package com.kitsune.upgradablespawners.placeholder;

import com.kitsune.upgradablespawners.UpgradableSpawners;
import com.kitsune.upgradablespawners.data.SpawnerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion exposing the upgrade levels of the spawner a player is looking
 * at:
 *
 * <ul>
 * <li>%upgradablespawners_radius%</li>
 * <li>%upgradablespawners_speed%</li>
 * <li>%upgradablespawners_quantity%</li>
 * </ul>
 *
 * Each returns "0" when the player is not looking at a tracked (upgraded) spawner.
 */
public class SpawnersExpansion extends PlaceholderExpansion {

    private final UpgradableSpawners plugin;

    public SpawnersExpansion(UpgradableSpawners plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "upgradablespawners";
    }

    @Override
    public String getAuthor() {
        return "Kitsune";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    // Keep the expansion registered across PlaceholderAPI reloads (we register it manually
    // at enable, not from the expansion cloud).
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "0";
        }

        Block target = player.getTargetBlockExact(6);
        if (target == null || target.getType() != Material.SPAWNER) {
            return "0";
        }

        // Only read data for spawners we actually track, so looking at a plain spawner
        // does not create an empty entry.
        if (!plugin.getDataManager().hasUpgrades(target.getLocation())) {
            return "0";
        }

        SpawnerData data = plugin.getDataManager().getSpawnerData(target.getLocation());
        switch (params.toLowerCase()) {
            case "radius":
                return String.valueOf(data.getRadiusLevel());
            case "speed":
                return String.valueOf(data.getSpeedLevel());
            case "quantity":
                return String.valueOf(data.getQuantityLevel());
            default:
                return null;
        }
    }
}
