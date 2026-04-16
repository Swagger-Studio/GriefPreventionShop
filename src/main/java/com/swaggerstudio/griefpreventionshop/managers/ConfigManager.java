package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import com.swaggerstudio.griefpreventionshop.commands.MainCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final GriefPreventionShop plugin;
    private FileConfiguration config;

    public ConfigManager(GriefPreventionShop plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getPrefix() {
        return config.getString("prefix", "&#FFC427&lCLAIM SHOP &8➟");
    }

    public String getCurrencySymbol() {
        return config.getString("currency-symbol", "$");
    }

    public double getPricePerBlock() {
        return config.getDouble("price-per-block", 100.0);
    }

    public int getMinPurchase() {
        return config.getInt("limits.min-purchase", 1);
    }

    public int getMaxPurchase() {
        return config.getInt("limits.max-purchase", 1000000);
    }

    public void registerCommands() {
        // Register Main Command
        String mainCmdName = plugin.getConfig().getString("commands.main", "gpshop");
        List<String> mainAliases = plugin.getConfig().getStringList("commands.aliases");
        registerDynamicCommand(mainCmdName, mainAliases);

        // Register History Command
        if (plugin.getConfig().getBoolean("history.enabled", true)) {
            String histCmdName = plugin.getConfig().getString("history.command", "claimhistory");
            registerDynamicCommand(histCmdName, new ArrayList<>());
        }
    }

    private void registerDynamicCommand(String name, List<String> aliases) {
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            // 1. Unregister old command
            unregisterCommand(commandMap, name);
            for (String alias : aliases) {
                unregisterCommand(commandMap, alias);
            }

            // 2. Register newest version
            MainCommand cmd = new MainCommand(plugin, name);
            cmd.setAliases(aliases);
            commandMap.register(plugin.getName(), cmd);
            
        } catch (NoSuchFieldException | IllegalAccessException e) {
            plugin.getLogger().severe("Could not register command " + name + " dynamically: " + e.getMessage());
        }
    }

    private void unregisterCommand(CommandMap commandMap, String name) {
        try {
            Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);
            knownCommands.remove(name);
            knownCommands.remove(plugin.getName().toLowerCase() + ":" + name);
        } catch (Exception e) {
            // Fallback for different server software versions
        }
    }
}
