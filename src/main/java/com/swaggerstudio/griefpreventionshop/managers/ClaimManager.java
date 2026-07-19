package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import com.swaggerstudio.griefpreventionshop.provider.ClaimProvider;
import com.swaggerstudio.griefpreventionshop.provider.ClaimProviderFactory;
import com.swaggerstudio.griefpreventionshop.utils.SchedulerUtil;
import org.bukkit.entity.Player;

/**
 * Thin delegator over a {@link ClaimProvider}. Routes all claim-block operations through the
 * provider abstraction and wraps writes so they are Folia-safe.
 */
public class ClaimManager {

    private final GriefPreventionShop plugin;
    private final ClaimProvider provider;

    public ClaimManager(GriefPreventionShop plugin) {
        this.plugin = plugin;
        this.provider = ClaimProviderFactory.detect(plugin);
    }

    /** @return true when a supported claim plugin was detected. */
    public boolean isReady() {
        return provider != null;
    }

    /** @return the active provider's name, or {@code "None"} when none was detected. */
    public String getProviderName() {
        return provider != null ? provider.getName() : "None";
    }

    public void addClaimBlocks(Player player, int amount) {
        if (provider == null) return;
        // GriefPrevention's datastore is global state; on Folia it must be mutated on the
        // global region thread. On normal servers this runs inline when already on the main thread.
        SchedulerUtil.runGlobal(plugin, () -> provider.addClaimBlocks(player, amount));
    }

    public int getClaimBlocks(Player player) {
        if (provider == null) return 0;
        return provider.getClaimBlocks(player);
    }
}
