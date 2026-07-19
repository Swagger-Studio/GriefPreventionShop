package com.swaggerstudio.griefpreventionshop.economy;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultProvider implements EconomyProvider {
    private Economy vaultEcon = null;
    private String failureReason = null;

    public VaultProvider(GriefPreventionShop plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            failureReason = "Vault plugin is not installed/enabled.";
            return;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            vaultEcon = rsp.getProvider();
        }
        if (vaultEcon == null) {
            failureReason = "Economy service provider for Vault was not found.";
        }
    }

    @Override
    public String getName() {
        return "Vault";
    }

    @Override
    public boolean isHooked() {
        return vaultEcon != null;
    }

    @Override
    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public boolean hasBalance(Player player, double amount) {
        return isHooked() && vaultEcon.has(player, amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return isHooked() && vaultEcon.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public double getBalance(Player player) {
        return isHooked() ? vaultEcon.getBalance(player) : 0.0;
    }
}
