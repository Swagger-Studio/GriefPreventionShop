package com.swaggerstudio.griefpreventionshop.economy;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in Vault Economy implementation used when Vault or VaultUnlocked is present
 * on the server but no external economy plugin (like EssentialsX, TheNewEconomy, etc.)
 * has registered an Economy service provider.
 *
 * <p>Registers into Bukkit's ServicesManager at ServicePriority.Lowest, ensuring
 * that any external economy plugin installed later automatically overrides this.</p>
 */
public class InternalVaultEconomy extends AbstractEconomy {

    private final GriefPreventionShop plugin;
    private final File storageFile;
    private final YamlConfiguration storageConfig;
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameToUuid = new ConcurrentHashMap<>();
    private double defaultStartingBalance = 1000.0;

    public InternalVaultEconomy(GriefPreventionShop plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "balances.yml");
        if (!storageFile.exists()) {
            try {
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                storageFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create balances.yml: " + e.getMessage());
            }
        }
        this.storageConfig = YamlConfiguration.loadConfiguration(storageFile);
        loadBalances();
    }

    private void loadBalances() {
        if (storageConfig.contains("default-balance")) {
            this.defaultStartingBalance = storageConfig.getDouble("default-balance", 1000.0);
        } else {
            storageConfig.set("default-balance", 1000.0);
            saveStorage();
        }

        if (storageConfig.isConfigurationSection("balances")) {
            for (String key : storageConfig.getConfigurationSection("balances").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    double bal = storageConfig.getDouble("balances." + key, defaultStartingBalance);
                    balances.put(uuid, bal);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private synchronized void saveStorage() {
        CompletableFuture.runAsync(() -> {
            try {
                synchronized (storageConfig) {
                    storageConfig.save(storageFile);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save balances.yml: " + e.getMessage());
            }
        });
    }

    public void registerService() {
        try {
            plugin.getServer().getServicesManager().register(
                    net.milkbowl.vault.economy.Economy.class,
                    this,
                    plugin,
                    ServicePriority.Lowest
            );
            plugin.getLogger().info("Activated built-in Vault economy provider for VaultUnlocked!");
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register built-in Vault economy provider: " + t.getMessage());
        }
    }

    private UUID resolveUuid(String playerName) {
        if (playerName == null || playerName.isEmpty()) return null;
        UUID cached = nameToUuid.get(playerName.toLowerCase());
        if (cached != null) return cached;

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer != null && offlinePlayer.getUniqueId() != null) {
            nameToUuid.put(playerName.toLowerCase(), offlinePlayer.getUniqueId());
            return offlinePlayer.getUniqueId();
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "VaultUnlocked";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return String.format("%.2f", amount);
    }

    @Override
    public String currencyNamePlural() {
        return "Money";
    }

    @Override
    public String currencyNameSingular() {
        return "Money";
    }

    @Override
    public boolean hasAccount(String playerName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    @Override
    public double getBalance(String playerName) {
        UUID uuid = resolveUuid(playerName);
        return uuid != null ? getBalance(uuid) : defaultStartingBalance;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) return defaultStartingBalance;
        return getBalance(player.getUniqueId());
    }

    public double getBalance(UUID uuid) {
        return balances.computeIfAbsent(uuid, k -> {
            storageConfig.set("balances." + uuid.toString(), defaultStartingBalance);
            saveStorage();
            return defaultStartingBalance;
        });
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        UUID uuid = resolveUuid(playerName);
        if (uuid == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Player not found");
        }
        return withdraw(uuid, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (player == null || player.getUniqueId() == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Invalid player");
        }
        return withdraw(player.getUniqueId(), amount);
    }

    public EconomyResponse withdraw(UUID uuid, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(uuid), ResponseType.FAILURE, "Cannot withdraw negative amount");
        }
        double current = getBalance(uuid);
        if (current < amount) {
            return new EconomyResponse(0, current, ResponseType.FAILURE, "Insufficient balance");
        }
        double newBal = current - amount;
        balances.put(uuid, newBal);
        storageConfig.set("balances." + uuid.toString(), newBal);
        saveStorage();
        return new EconomyResponse(amount, newBal, ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        UUID uuid = resolveUuid(playerName);
        if (uuid == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Player not found");
        }
        return deposit(uuid, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (player == null || player.getUniqueId() == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Invalid player");
        }
        return deposit(player.getUniqueId(), amount);
    }

    public EconomyResponse deposit(UUID uuid, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(uuid), ResponseType.FAILURE, "Cannot deposit negative amount");
        }
        double current = getBalance(uuid);
        double newBal = current + amount;
        balances.put(uuid, newBal);
        storageConfig.set("balances." + uuid.toString(), newBal);
        saveStorage();
        return new EconomyResponse(amount, newBal, ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }
}
