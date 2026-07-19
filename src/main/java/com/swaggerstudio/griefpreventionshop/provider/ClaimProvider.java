package com.swaggerstudio.griefpreventionshop.provider;

import org.bukkit.entity.Player;

/**
 * Abstraction over the underlying claim plugin so all claim-block operations are routed
 * through a single seam. Implemented for both standard GriefPrevention and GriefPrevention3D.
 *
 * <p>Shop logic never talks to a claim plugin directly — only to this interface — so adding a
 * new claim backend means adding a provider, not duplicating shop logic.</p>
 */
public interface ClaimProvider {

    /** Display name of the active provider, e.g. {@code "GriefPrevention"} or {@code "GriefPrevention3D"}. */
    String getName();

    /** @return the player's total usable claim blocks (accrued + bonus). */
    int getClaimBlocks(Player player);

    /** Grants {@code amount} bonus claim blocks to the player and persists the change. */
    void addClaimBlocks(Player player, int amount);

    /** @return true when the backing claim plugin is loaded and ready to serve requests. */
    boolean isAvailable();

    /** @return the player's bonus claim blocks only. Default-provided for future use. */
    default int getBonusClaimBlocks(Player player) {
        return 0;
    }

    /** @return the player's accrued claim blocks only. Default-provided for future use. */
    default int getAccruedClaimBlocks(Player player) {
        return 0;
    }
}
