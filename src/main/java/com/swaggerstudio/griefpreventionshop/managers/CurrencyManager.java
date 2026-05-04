package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class CurrencyManager {

    private final GriefPreventionShop plugin;
    private FileConfiguration config;
    private File file;

    public CurrencyManager(GriefPreventionShop plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.file = new File(plugin.getDataFolder(), "currencies.yml");
        if (!file.exists()) {
            plugin.saveResource("currencies.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getActiveCurrency() {
        return config.getString("active-currency", "Vault");
    }

    public String getDisplayGui(String currency) {
        String name = config.getString("currencies." + currency + ".raw", currency);
        return "&f%price% &#AAFFFF" + name;
    }

    public String getExcellentEconomyCurrencyId() {
        return config.getString("currencies.ExcellentEconomy.currency-id", "coins");
    }
}
