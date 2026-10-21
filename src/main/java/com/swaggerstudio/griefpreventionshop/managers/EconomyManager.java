package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import com.swaggerstudio.griefpreventionshop.economy.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class EconomyManager {

    private final GriefPreventionShop plugin;
    private EconomyProvider provider = null;

    public EconomyManager(GriefPreventionShop plugin) {
        this.plugin = plugin;
        setupProvider();
    }

    private void setupProvider() {
        String active = plugin.getCurrencyManager().getActiveCurrency();
        
        if (active.equalsIgnoreCase("auto")) {
            // 1. Try Vault / VaultUnlocked if an economy provider is registered
            VaultProvider vp = new VaultProvider(plugin);
            if (vp.isHooked()) {
                this.provider = vp;
                return;
            }

            // 2. Try PlayerPoints
            if (Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
                PlayerPointsProvider pp = new PlayerPointsProvider(plugin);
                if (pp.isHooked()) {
                    this.provider = pp;
                    return;
                }
            }

            // 3. Try ExcellentEconomy / CoinsEngine
            if (Bukkit.getPluginManager().getPlugin("ExcellentEconomy") != null) {
                try {
                    EconomyProvider ee = (EconomyProvider) Class.forName("com.swaggerstudio.griefpreventionshop.economy.ExcellentEconomyProvider")
                            .getConstructor(GriefPreventionShop.class)
                            .newInstance(plugin);
                    if (ee.isHooked()) {
                        this.provider = ee;
                        return;
                    }
                } catch (Throwable ignored) {}
            }
            if (Bukkit.getPluginManager().getPlugin("CoinsEngine") != null) {
                LegacyCoinsEngineProvider ce = new LegacyCoinsEngineProvider(plugin);
                if (ce.isHooked()) {
                    this.provider = ce;
                    return;
                }
            }

            // 4. Default fallback to Experience (Vanilla XP)
            this.provider = new ExperienceProvider(plugin);
            return;
        }

        switch (active) {
            case "Vault":
            case "VaultUnlocked":
                this.provider = new VaultProvider(plugin);
                break;
            case "PlayerPoints":
                this.provider = new PlayerPointsProvider(plugin);
                break;
            case "Experience":
                this.provider = new ExperienceProvider(plugin);
                break;
            case "ExcellentEconomy":
                // 1. Try ExcellentEconomy
                if (Bukkit.getPluginManager().getPlugin("ExcellentEconomy") != null) {
                    try {
                        this.provider = (EconomyProvider) Class.forName("com.swaggerstudio.griefpreventionshop.economy.ExcellentEconomyProvider")
                                .getConstructor(GriefPreventionShop.class)
                                .newInstance(plugin);
                    } catch (Throwable t) {
                        plugin.getLogger().warning("Failed to instantiate ExcellentEconomyProvider: " + t.getMessage());
                    }
                }
                // 2. Try legacy CoinsEngine
                if ((this.provider == null || !this.provider.isHooked()) && Bukkit.getPluginManager().getPlugin("CoinsEngine") != null) {
                    this.provider = new LegacyCoinsEngineProvider(plugin);
                }
                
                // 3. Fallback/Fail if still null/not hooked
                if (this.provider == null) {
                    this.provider = new EconomyProvider() {
                        @Override
                        public String getName() { return "ExcellentEconomy"; }
                        @Override
                        public boolean isHooked() { return false; }
                        @Override
                        public String getFailureReason() { return "Neither ExcellentEconomy nor CoinsEngine plugin is enabled."; }
                        @Override
                        public boolean hasBalance(Player p, double a) { return false; }
                        @Override
                        public boolean withdraw(Player p, double a) { return false; }
                        @Override
                        public double getBalance(Player p) { return 0.0; }
                    };
                }
                break;
            default:
                break;
        }
    }

    public boolean isHooked() {
        return provider != null && provider.isHooked();
    }

    public String getProviderName() {
        return provider != null ? provider.getName() : "None";
    }

    public void logStatus() {
        // Obsolete: console hook log status is now handled cleanly on startup by GriefPreventionShop.java
    }

    public String getFailureReason() {
        return provider != null ? provider.getFailureReason() : "No economy provider configured.";
    }

    public boolean hasBalance(Player player, double amount) {
        return provider != null && provider.hasBalance(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        return provider != null && provider.withdraw(player, amount);
    }

    public double getBalance(Player player) {
        return provider != null ? provider.getBalance(player) : 0.0;
    }
}
