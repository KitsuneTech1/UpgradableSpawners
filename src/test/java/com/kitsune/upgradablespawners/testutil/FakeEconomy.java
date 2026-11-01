package com.kitsune.upgradablespawners.testutil;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A minimal in-memory Vault {@link Economy} for tests. Only the player-balance paths the
 * plugin actually uses (getBalance / withdrawPlayer / format) carry real behavior; the
 * rest return sane defaults. Balances are keyed by player UUID.
 */
public class FakeEconomy implements Economy {

    private final Map<UUID, Double> balances = new HashMap<>();
    private boolean withdrawFailure;

    public void setBalance(OfflinePlayer player, double amount) {
        balances.put(player.getUniqueId(), amount);
    }

    public void setWithdrawFailure(boolean withdrawFailure) {
        this.withdrawFailure = withdrawFailure;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return balances.getOrDefault(player.getUniqueId(), 0.0);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        double current = getBalance(player);
        if (withdrawFailure) {
            return new EconomyResponse(0, current, ResponseType.FAILURE, "Synthetic transaction failure");
        }
        if (amount < 0) {
            return new EconomyResponse(0, current, ResponseType.FAILURE, "Cannot withdraw a negative amount");
        }
        if (current < amount) {
            return new EconomyResponse(0, current, ResponseType.FAILURE, "Insufficient funds");
        }
        double updated = current - amount;
        balances.put(player.getUniqueId(), updated);
        return new EconomyResponse(amount, updated, ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        double updated = getBalance(player) + amount;
        balances.put(player.getUniqueId(), updated);
        return new EconomyResponse(amount, updated, ResponseType.SUCCESS, null);
    }

    @Override
    public String format(double amount) {
        return "$" + amount;
    }

    // ---- Metadata ----

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "FakeEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String currencyNamePlural() {
        return "dollars";
    }

    @Override
    public String currencyNameSingular() {
        return "dollar";
    }

    // ---- Account / balance overloads (delegate to the UUID-keyed store) ----

    @Override
    public boolean hasAccount(String playerName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    @Override
    public double getBalance(String playerName) {
        return 0.0;
    }

    @Override
    public double getBalance(String playerName, String world) {
        return 0.0;
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return false;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return false;
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    // ---- Bank operations (unsupported) ----

    @Override
    public EconomyResponse createBank(String name, String player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    // ---- Account creation ----

    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    private EconomyResponse notImplemented() {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Not implemented in FakeEconomy");
    }
}
