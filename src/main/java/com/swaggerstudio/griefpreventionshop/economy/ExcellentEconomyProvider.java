package com.swaggerstudio.griefpreventionshop.economy;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;

public class ExcellentEconomyProvider implements EconomyProvider {
    private ExcellentEconomyAPI api = null;
    private String currencyId;
    private String failureReason = null;

    public ExcellentEconomyProvider(GriefPreventionShop plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("ExcellentEconomy") == null) {
            failureReason = "ExcellentEconomy plugin is not installed/enabled.";
            return;
        }
        try {
            RegisteredServiceProvider<ExcellentEconomyAPI> provider = Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class);
            if (provider != null) {
                this.api = provider.getProvider();
            }
            if (this.api == null) {
                failureReason = "ExcellentEconomyAPI registered service provider was not found.";
                return;
            }
            this.currencyId = plugin.getCurrencyManager().getExcellentEconomyCurrencyId();
            if (!this.api.hasCurrency(currencyId)) {
                failureReason = "Currency ID '" + currencyId + "' is not registered in ExcellentEconomy.";
                this.api = null; // Mark as failed
            }
        } catch (Throwable t) {
            failureReason = "Failed to initialize ExcellentEconomy hook: " + t.getMessage();
            this.api = null;
        }
    }

    @Override
    public String getName() {
        return "ExcellentEconomy";
    }

    @Override
    public boolean isHooked() {
        return api != null;
    }

    @Override
    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public boolean hasBalance(Player player, double amount) {
        if (!isHooked()) return false;
        try {
            return api.getBalance(player, currencyId) >= amount;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (!isHooked()) return false;
        try {
            return api.withdraw(player, currencyId, amount);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public double getBalance(Player player) {
        if (!isHooked()) return 0.0;
        try {
            return api.getBalance(player, currencyId);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
