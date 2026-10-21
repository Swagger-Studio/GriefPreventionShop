package com.swaggerstudio.griefpreventionshop.economy;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;

public class VaultProvider implements EconomyProvider {
    private final GriefPreventionShop plugin;
    private net.milkbowl.vault.economy.Economy legacyVault = null;
    private net.milkbowl.vault2.economy.Economy modernVault = null;
    private InternalVaultEconomy internalVault = null;
    private String failureReason = null;
    private String providerName = "Vault";

    public VaultProvider(GriefPreventionShop plugin) {
        this.plugin = plugin;

        boolean hasVault = plugin.getServer().getPluginManager().getPlugin("Vault") != null;
        boolean hasVaultUnlocked = plugin.getServer().getPluginManager().getPlugin("VaultUnlocked") != null;

        if (!hasVault && !hasVaultUnlocked) {
            failureReason = "Neither Vault nor VaultUnlocked plugin is installed/enabled.";
            return;
        }

        // 1. Try to hook Modern VaultUnlocked (Vault 2.x API) from an external plugin
        try {
            RegisteredServiceProvider<net.milkbowl.vault2.economy.Economy> rspModern =
                    plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault2.economy.Economy.class);
            if (rspModern != null) {
                this.modernVault = rspModern.getProvider();
            }
            if (this.modernVault == null) {
                java.util.Collection<RegisteredServiceProvider<net.milkbowl.vault2.economy.Economy>> allModern =
                        plugin.getServer().getServicesManager().getRegistrations(net.milkbowl.vault2.economy.Economy.class);
                if (allModern != null && !allModern.isEmpty()) {
                    for (RegisteredServiceProvider<net.milkbowl.vault2.economy.Economy> rsp : allModern) {
                        if (rsp != null && rsp.getProvider() != null) {
                            this.modernVault = rsp.getProvider();
                            break;
                        }
                    }
                }
            }
            if (this.modernVault != null) {
                this.providerName = "VaultUnlocked";
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not query VaultUnlocked (v2) service: " + t.getMessage());
        }

        // 2. Try to hook Legacy Vault (Vault 1.x API) from an external plugin if modern provider was not found
        if (this.modernVault == null) {
            try {
                RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rspLegacy =
                        plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (rspLegacy != null) {
                    this.legacyVault = rspLegacy.getProvider();
                }
                if (this.legacyVault == null) {
                    java.util.Collection<RegisteredServiceProvider<net.milkbowl.vault.economy.Economy>> allLegacy =
                            plugin.getServer().getServicesManager().getRegistrations(net.milkbowl.vault.economy.Economy.class);
                    if (allLegacy != null && !allLegacy.isEmpty()) {
                        for (RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp : allLegacy) {
                            if (rsp != null && rsp.getProvider() != null) {
                                this.legacyVault = rsp.getProvider();
                                break;
                            }
                        }
                    }
                }
                if (this.legacyVault != null) {
                    this.providerName = "Vault";
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Could not query Vault (v1) service: " + t.getMessage());
            }
        }

        // 3. Fallback: If Vault or VaultUnlocked is installed but has no external economy provider,
        // activate our built-in Vault economy provider so the server and shop work seamlessly!
        if (this.modernVault == null && this.legacyVault == null) {
            this.internalVault = new InternalVaultEconomy(plugin);
            this.internalVault.registerService();
            this.providerName = hasVaultUnlocked ? "VaultUnlocked" : "Vault";
            this.failureReason = null;
        }
    }

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    public boolean isHooked() {
        return modernVault != null || legacyVault != null || internalVault != null;
    }

    @Override
    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public boolean hasBalance(Player player, double amount) {
        if (!isHooked() || player == null) {
            return false;
        }
        if (modernVault != null) {
            return modernVault.has(plugin.getName(), player.getUniqueId(), BigDecimal.valueOf(amount));
        }
        if (legacyVault != null) {
            return legacyVault.has(player, amount);
        }
        if (internalVault != null) {
            return internalVault.has(player, amount);
        }
        return false;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (!isHooked() || player == null) {
            return false;
        }
        if (modernVault != null) {
            net.milkbowl.vault2.economy.EconomyResponse response =
                    modernVault.withdraw(plugin.getName(), player.getUniqueId(), BigDecimal.valueOf(amount));
            return response != null && response.transactionSuccess();
        }
        if (legacyVault != null) {
            return legacyVault.withdrawPlayer(player, amount).transactionSuccess();
        }
        if (internalVault != null) {
            return internalVault.withdrawPlayer(player, amount).transactionSuccess();
        }
        return false;
    }

    @Override
    public double getBalance(Player player) {
        if (!isHooked() || player == null) {
            return 0.0;
        }
        if (modernVault != null) {
            BigDecimal balance = modernVault.balance(plugin.getName(), player.getUniqueId());
            return balance != null ? balance.doubleValue() : 0.0;
        }
        if (legacyVault != null) {
            return legacyVault.getBalance(player);
        }
        if (internalVault != null) {
            return internalVault.getBalance(player);
        }
        return 0.0;
    }
}

