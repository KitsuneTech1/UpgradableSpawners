package com.kitsune.upgradablespawners.data;

import org.bukkit.Location;

import java.util.Objects;

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
        this.radiusLevel = Math.max(0, radiusLevel);
        this.speedLevel = Math.max(0, speedLevel);
        this.quantityLevel = Math.max(0, quantityLevel);
    }

    public String getLocationKey() {
        return locationKey;
    }

    public synchronized int getRadiusLevel() {
        return radiusLevel;
    }

    public synchronized void setRadiusLevel(int radiusLevel) {
        this.radiusLevel = Math.max(0, radiusLevel);
    }

    public synchronized int getSpeedLevel() {
        return speedLevel;
    }

    public synchronized void setSpeedLevel(int speedLevel) {
        this.speedLevel = Math.max(0, speedLevel);
    }

    public synchronized int getQuantityLevel() {
        return quantityLevel;
    }

    public synchronized void setQuantityLevel(int quantityLevel) {
        this.quantityLevel = Math.max(0, quantityLevel);
    }

    public synchronized boolean hasAnyUpgrades() {
        return radiusLevel > 0 || speedLevel > 0 || quantityLevel > 0;
    }

    public synchronized Levels snapshot() {
        return new Levels(radiusLevel, speedLevel, quantityLevel);
    }

    public record Levels(int radius, int speed, int quantity) {
    }

    /**
     * Create a location key from a Location object
     */
    public static String createKey(Location location) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(location.getWorld(), "location.world");
        return location.getWorld().getName() + ":" +
                location.getBlockX() + ":" +
                location.getBlockY() + ":" +
                location.getBlockZ();
    }
}
