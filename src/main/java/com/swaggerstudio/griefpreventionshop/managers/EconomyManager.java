package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final GriefPreventionShop plugin;
    private Economy econ = null;

    public EconomyManager(GriefPreventionShop plugin) {
        this.plugin = plugin;
        if (!setupEconomy()) {
            plugin.getLogger().severe("Disabled due to no Vault dependency found!");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public boolean hasBalance(OfflinePlayer player, double amount) {
        return econ.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }

    public void deposit(OfflinePlayer player, double amount) {
        econ.depositPlayer(player, amount);
    }
    
    public String format(double amount) {
        return econ.format(amount);
    }
}
