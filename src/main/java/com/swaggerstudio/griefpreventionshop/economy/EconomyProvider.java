package com.swaggerstudio.griefpreventionshop.economy;

import org.bukkit.entity.Player;

public interface EconomyProvider {
    /**
     * @return name of the provider
     */
    String getName();

    /**
     * Checks if provider is initialized and successfully hooked.
     */
    boolean isHooked();

    /**
     * Returns failure reason if initialization failed.
     */
    String getFailureReason();

    /**
     * Checks if the player has at least the specified amount.
     */
    boolean hasBalance(Player player, double amount);

    /**
     * Deducts the specified amount from the player's balance.
     */
    boolean withdraw(Player player, double amount);

    /**
     * Gets the player's current balance.
     */
    double getBalance(Player player);
}
