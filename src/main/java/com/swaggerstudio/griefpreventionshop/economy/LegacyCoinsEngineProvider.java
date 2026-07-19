package com.swaggerstudio.griefpreventionshop.economy;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import org.bukkit.entity.Player;
import java.lang.reflect.Method;

public class LegacyCoinsEngineProvider implements EconomyProvider {
    private Object ceCurrency = null;
    private Method ceGetBalanceMethod = null;
    private Method ceWithdrawMethod = null;
    private String failureReason = null;
    private final String currencyId;

    public LegacyCoinsEngineProvider(GriefPreventionShop plugin) {
        this.currencyId = plugin.getCurrencyManager().getExcellentEconomyCurrencyId();
        if (plugin.getServer().getPluginManager().getPlugin("CoinsEngine") == null) {
            failureReason = "CoinsEngine plugin is not installed/enabled.";
            return;
        }
        try {
            Class<?> coinsEngineAPIClass = Class.forName("su.nightexpress.coinsengine.api.CoinsEngineAPI");
            Class<?> currencyClass = Class.forName("su.nightexpress.coinsengine.api.currency.Currency");
            Method getCurrencyMethod = coinsEngineAPIClass.getMethod("getCurrency", String.class);
            ceCurrency = getCurrencyMethod.invoke(null, currencyId);
            
            if (ceCurrency != null) {
                ceGetBalanceMethod = coinsEngineAPIClass.getMethod("getBalance", Player.class, currencyClass);
                try {
                    ceWithdrawMethod = coinsEngineAPIClass.getMethod("removeBalance", Player.class, currencyClass, double.class);
                } catch (Exception e) {
                    ceWithdrawMethod = coinsEngineAPIClass.getMethod("withdraw", Player.class, currencyClass, double.class);
                }
            } else {
                failureReason = "Currency ID '" + currencyId + "' is not registered in CoinsEngine.";
            }
        } catch (Throwable t) {
            failureReason = "Failed to hook into legacy CoinsEngine: " + t.getMessage();
        }
    }

    @Override
    public String getName() {
        return "CoinsEngine";
    }

    @Override
    public boolean isHooked() {
        return ceCurrency != null && ceGetBalanceMethod != null && ceWithdrawMethod != null;
    }

    @Override
    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public boolean hasBalance(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (!isHooked()) return false;
        try {
            ceWithdrawMethod.invoke(null, player, ceCurrency, amount);
            return true; // removeBalance returns void
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public double getBalance(Player player) {
        if (!isHooked()) return 0.0;
        try {
            return (double) ceGetBalanceMethod.invoke(null, player, ceCurrency);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
