package com.kitsune.upgradablespawners.economy;

import org.bukkit.entity.Player;

/**
 * Small plugin-owned abstraction so the main plugin can run without Vault installed.
 */
public interface EconomyProvider {

    double getBalance(Player player);

    PaymentResult withdraw(Player player, double amount);

    String format(double amount);

    record PaymentResult(boolean success) {
    }
}
