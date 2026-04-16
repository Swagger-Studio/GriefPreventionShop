package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import com.swaggerstudio.griefpreventionshop.commands.MainCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Field;
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
        String mainCommand = config.getString("commands.main", "claimshop");
        List<String> aliases = config.getStringList("commands.aliases");

        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            // 1. Unregister old commands to allow "instant" change without restart
            unregisterCommand(commandMap, mainCommand);
            for (String alias : aliases) {
                unregisterCommand(commandMap, alias);
            }

            // 2. Register newest version
            MainCommand cmd = new MainCommand(plugin, mainCommand);
            cmd.setAliases(aliases);
            commandMap.register(plugin.getName(), cmd);
            
        } catch (NoSuchFieldException | IllegalAccessException e) {
            plugin.getLogger().severe("Could not register commands dynamically: " + e.getMessage());
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
