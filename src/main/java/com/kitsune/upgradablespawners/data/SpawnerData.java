package com.kitsune.upgradablespawners.data;

import org.bukkit.Location;

/**
 * Stores upgrade levels for a single spawner
 */
public class SpawnerData {

    private final String locationKey;
    private int radiusLevel;
    private int speedLevel;
    private int quantityLevel;

    public SpawnerData(String locationKey) {
        this.locationKey = locationKey;
        this.radiusLevel = 0;
        this.speedLevel = 0;
        this.quantityLevel = 0;
    }

    public SpawnerData(String locationKey, int radiusLevel, int speedLevel, int quantityLevel) {
        this.locationKey = locationKey;
        this.radiusLevel = radiusLevel;
        this.speedLevel = speedLevel;
        this.quantityLevel = quantityLevel;
    }

    public String getLocationKey() {
        return locationKey;
    }

    public int getRadiusLevel() {
        return radiusLevel;
    }

    public void setRadiusLevel(int radiusLevel) {
        this.radiusLevel = radiusLevel;
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public void setSpeedLevel(int speedLevel) {
        this.speedLevel = speedLevel;
    }

    public int getQuantityLevel() {
        return quantityLevel;
    }

    public void setQuantityLevel(int quantityLevel) {
        this.quantityLevel = quantityLevel;
    }

    public boolean hasAnyUpgrades() {
        return radiusLevel > 0 || speedLevel > 0 || quantityLevel > 0;
    }

    /**
     * Create a location key from a Location object
     */
    public static String createKey(Location location) {
        return location.getWorld().getName() + ":" +
                location.getBlockX() + ":" +
                location.getBlockY() + ":" +
                location.getBlockZ();
    }
}
