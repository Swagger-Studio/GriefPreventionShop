package com.swaggerstudio.griefpreventionshop.economy;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;

public class PlayerPointsProvider implements EconomyProvider {
    private PlayerPointsAPI ppAPI = null;
    private String failureReason = null;

    public PlayerPointsProvider(GriefPreventionShop plugin) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("PlayerPoints")) {
            failureReason = "PlayerPoints plugin is not installed/enabled.";
            return;
        }
        try {
            ppAPI = PlayerPoints.getInstance().getAPI();
        } catch (Exception e) {
            failureReason = "Failed to access PlayerPoints API: " + e.getMessage();
        }
    }

    @Override
    public String getName() {
        return "PlayerPoints";
    }

    @Override
    public boolean isHooked() {
        return ppAPI != null;
    }

    @Override
    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public boolean hasBalance(Player player, double amount) {
        return isHooked() && ppAPI.look(player.getUniqueId()) >= (int) amount;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return isHooked() && ppAPI.take(player.getUniqueId(), (int) amount);
    }

    @Override
    public double getBalance(Player player) {
        return isHooked() ? ppAPI.look(player.getUniqueId()) : 0.0;
    }
}
