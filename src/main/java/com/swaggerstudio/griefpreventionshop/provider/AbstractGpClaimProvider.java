package com.swaggerstudio.griefpreventionshop.provider;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.entity.Player;

/**
 * Shared implementation for GriefPrevention-family backends.
 *
 * <p>GriefPrevention and GriefPrevention3D expose an identical API surface
 * ({@code me.ryanhamshire.GriefPrevention.{GriefPrevention,PlayerData,DataStore}} with the same
 * method signatures), so the real logic lives here exactly once. The concrete subclasses differ
 * only by reported name; this base class is the single seam where a future divergence between the
 * two plugins would be overridden.</p>
 */
public abstract class AbstractGpClaimProvider implements ClaimProvider {

    /** @return the provider name reported to the user/banner. */
    protected abstract String providerName();

    @Override
    public final String getName() {
        return providerName();
    }

    @Override
    public boolean isAvailable() {
        return GriefPrevention.instance != null && GriefPrevention.instance.dataStore != null;
    }

    @Override
    public int getClaimBlocks(Player player) {
        PlayerData data = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        return data.getAccruedClaimBlocks() + data.getBonusClaimBlocks();
    }

    @Override
    public int getBonusClaimBlocks(Player player) {
        PlayerData data = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        return data.getBonusClaimBlocks();
    }

    @Override
    public int getAccruedClaimBlocks(Player player) {
        PlayerData data = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        return data.getAccruedClaimBlocks();
    }

    @Override
    public void addClaimBlocks(Player player, int amount) {
        PlayerData data = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        int currentBonus = data.getBonusClaimBlocks();
        data.setBonusClaimBlocks(currentBonus + amount);

        // GP saves periodically, but persist immediately so the purchase is durable.
        GriefPrevention.instance.dataStore.savePlayerData(player.getUniqueId(), data);
    }
}
