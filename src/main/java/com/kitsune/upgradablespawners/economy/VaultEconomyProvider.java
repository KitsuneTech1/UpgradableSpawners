package com.kitsune.upgradablespawners.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Vault adapter kept isolated from classes that must load without Vault. */
public final class VaultEconomyProvider implements EconomyProvider {

    private final Economy economy;

    private VaultEconomyProvider(Economy economy) {
        this.economy = economy;
    }

    public static EconomyProvider find(org.bukkit.plugin.Plugin plugin) {
        RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);
        return registration == null || registration.getProvider() == null
                ? null
                : new VaultEconomyProvider(registration.getProvider());
    }

    @Override
    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    @Override
    public PaymentResult withdraw(Player player, double amount) {
        var response = economy.withdrawPlayer(player, amount);
        return new PaymentResult(response != null && response.transactionSuccess());
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }
}
