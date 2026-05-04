package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.UUID;

public class EconomyManager {

    private final GriefPreventionShop plugin;
    private Economy vaultEcon = null;
    private PlayerPointsAPI ppAPI = null;
    
    // Reflection-safe API handling
    private Object eeAPI = null;
    private Method eeGetBalanceMethod = null;
    private Method eeWithdrawMethod = null;
    private Object eeCurrency = null;
    
    private Class<?> coinsEngineAPIClass = null;
    private Method ceGetBalanceMethod = null;
    private Method ceWithdrawMethod = null;
    private Object ceCurrency = null;
    
    private boolean isEEHooked = false;
    private boolean isCEHooked = false;

    public EconomyManager(GriefPreventionShop plugin) {
        this.plugin = plugin;
        setupVault();
        setupPlayerPoints();
        setupEE();
    }

    private void setupVault() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) vaultEcon = rsp.getProvider();
    }

    private void setupPlayerPoints() {
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlayerPoints")) {
            ppAPI = PlayerPoints.getInstance().getAPI();
        }
    }

    private void setupEE() {
        String currencyId = plugin.getCurrencyManager().getExcellentEconomyCurrencyId();
        plugin.getLogger().info("Initializing hooks for currency: " + currencyId);
        
        // 1. Try ExcellentEconomy (New API)
        try {
            Class<?> apiClass = Class.forName("su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI");
            RegisteredServiceProvider<?> rsp = Bukkit.getServer().getServicesManager().getRegistration(apiClass);
            if (rsp != null) {
                eeAPI = rsp.getProvider();
                Class<?> currencyClass = Class.forName("su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency");
                Method getCurrencyMethod = apiClass.getMethod("getCurrency", String.class);
                eeCurrency = getCurrencyMethod.invoke(eeAPI, currencyId);
                
                if (eeCurrency != null) {
                    eeGetBalanceMethod = apiClass.getMethod("getBalance", UUID.class, currencyClass);
                    // Try to find withdrawAsync or withdraw
                    try {
                        eeWithdrawMethod = apiClass.getMethod("withdrawAsync", UUID.class, currencyClass, double.class);
                    } catch (Exception e) {
                        eeWithdrawMethod = apiClass.getMethod("withdraw", UUID.class, currencyClass, double.class);
                    }
                    isEEHooked = true;
                    plugin.getLogger().info("Successfully hooked into ExcellentEconomy API!");
                    return;
                }
            }
        } catch (Exception ignored) {}

        // 2. Try CoinsEngine (Old API)
        try {
            coinsEngineAPIClass = Class.forName("su.nightexpress.coinsengine.api.CoinsEngineAPI");
            Class<?> currencyClass = Class.forName("su.nightexpress.coinsengine.api.currency.Currency");
            Method getCurrencyMethod = coinsEngineAPIClass.getMethod("getCurrency", String.class);
            ceCurrency = getCurrencyMethod.invoke(null, currencyId);
            
            if (ceCurrency != null) {
                // Patterns found in AxAuctions: getBalance(Player, Currency) and removeBalance(Player, Currency, double)
                ceGetBalanceMethod = coinsEngineAPIClass.getMethod("getBalance", Player.class, currencyClass);
                try {
                    ceWithdrawMethod = coinsEngineAPIClass.getMethod("removeBalance", Player.class, currencyClass, double.class);
                } catch (Exception e) {
                    ceWithdrawMethod = coinsEngineAPIClass.getMethod("withdraw", Player.class, currencyClass, double.class);
                }
                isCEHooked = true;
                plugin.getLogger().info("Successfully hooked into CoinsEngine API!");
            }
        } catch (Exception ignored) {}
        
        if (!isEEHooked && !isCEHooked) {
            plugin.getLogger().warning("Failed to find currency '" + currencyId + "' in ExcellentEconomy or CoinsEngine.");
        }
    }

    public void logStatus() {
        String active = plugin.getCurrencyManager().getActiveCurrency();
        String status = "&cNOT FOUND";
        String statusRaw = "NOT FOUND";

        if (active.equals("Vault") && vaultEcon != null) { status = "&aCONNECTED"; statusRaw = "CONNECTED"; }
        else if (active.equals("PlayerPoints") && ppAPI != null) { status = "&aCONNECTED"; statusRaw = "CONNECTED"; }
        else if (active.equals("Experience")) { status = "&aCONNECTED (Vanilla)"; statusRaw = "CONNECTED (Vanilla)"; }
        else if (active.equals("ExcellentEconomy")) {
            if (isEEHooked) { status = "&aCONNECTED"; statusRaw = "CONNECTED"; }
            else if (isCEHooked) { status = "&aCONNECTED (Legacy)"; statusRaw = "CONNECTED (Legacy)"; }
        }

        // Calculate padding to keep the banner border aligned
        // Base width is approx 51 chars. "Economy Hook: " (14) + active + " -> " (4) + statusRaw
        int length = 14 + active.length() + 4 + statusRaw.length();
        int padding = Math.max(0, 42 - length);
        String spaces = " ".repeat(padding);

        Bukkit.getConsoleSender().sendMessage(plugin.getMessageManager().parseColors(" &#FFC427&l║  &fEconomy Hook: &7" + active + " &8➟ " + status + spaces + " &#FFC427&l║"));
    }

    public boolean hasBalance(Player player, double amount) {
        String active = plugin.getCurrencyManager().getActiveCurrency();

        switch (active) {
            case "Vault":
                return vaultEcon != null && vaultEcon.has(player, amount);
            case "PlayerPoints":
                return ppAPI != null && ppAPI.look(player.getUniqueId()) >= (int) amount;
            case "ExcellentEconomy":
                return getEEBalance(player) >= amount;
            case "Experience":
                return getPlayerExp(player) >= (int) amount;
            default:
                return false;
        }
    }

    public boolean withdraw(Player player, double amount) {
        String active = plugin.getCurrencyManager().getActiveCurrency();

        switch (active) {
            case "Vault":
                return vaultEcon != null && vaultEcon.withdrawPlayer(player, amount).transactionSuccess();
            case "PlayerPoints":
                return ppAPI != null && ppAPI.take(player.getUniqueId(), (int) amount);
            case "ExcellentEconomy":
                return eeWithdraw(player, amount);
            case "Experience":
                if (getPlayerExp(player) >= (int) amount) {
                    player.setTotalExperience(player.getTotalExperience() - (int) amount);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private double getEEBalance(Player player) {
        if (isEEHooked && eeAPI != null && eeGetBalanceMethod != null) {
            try {
                return (double) eeGetBalanceMethod.invoke(eeAPI, player.getUniqueId(), eeCurrency);
            } catch (Exception ignored) {}
        }
        
        if (isCEHooked && ceGetBalanceMethod != null) {
            try {
                return (double) ceGetBalanceMethod.invoke(null, player, ceCurrency);
            } catch (Exception ignored) {}
        }
        
        return 0;
    }

    private boolean eeWithdraw(Player player, double amount) {
        if (isEEHooked && eeAPI != null && eeWithdrawMethod != null) {
            try {
                Object result = eeWithdrawMethod.invoke(eeAPI, player.getUniqueId(), eeCurrency, amount);
                // withdrawAsync returns CompletableFuture, withdraw returns boolean
                if (result instanceof java.util.concurrent.CompletableFuture) return true; 
                if (result instanceof Boolean) return (boolean) result;
                return true;
            } catch (Exception ignored) {}
        }

        if (isCEHooked && ceWithdrawMethod != null) {
            try {
                ceWithdrawMethod.invoke(null, player, ceCurrency, amount);
                return true; // removeBalance returns void
            } catch (Exception ignored) {}
        }

        return false;
    }

    private int getPlayerExp(Player player) {
        return player.getTotalExperience();
    }

    public double getBalance(Player player) {
        String active = plugin.getCurrencyManager().getActiveCurrency();
        switch (active) {
            case "Vault":
                return vaultEcon != null ? vaultEcon.getBalance(player) : 0;
            case "PlayerPoints":
                return ppAPI != null ? ppAPI.look(player.getUniqueId()) : 0;
            case "ExcellentEconomy":
                return getEEBalance(player);
            case "Experience":
                return getPlayerExp(player);
            default:
                return 0;
        }
    }
}
