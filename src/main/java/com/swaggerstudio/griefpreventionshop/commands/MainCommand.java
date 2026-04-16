package com.swaggerstudio.griefpreventionshop.commands;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MainCommand extends BukkitCommand {

    private final GriefPreventionShop plugin;

    public MainCommand(GriefPreventionShop plugin, String name) {
        super(name);
        this.plugin = plugin;
        this.description = "Main command for GriefPreventionShop";
        this.usageMessage = "/" + name + " [reload]";
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("griefpreventionshop.admin")) {
                if (sender instanceof Player) {
                    plugin.getMessageManager().sendMessage((Player) sender, "error.no-permission");
                } else {
                    sender.sendMessage("You don't have permission!");
                }
                return true;
            }
            plugin.reloadPlugin();
            if (sender instanceof Player) {
                plugin.getMessageManager().sendMessage((Player) sender, "admin.reloaded");
            } else {
                sender.sendMessage("Plugin reloaded successfully!");
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        String historyCmd = plugin.getConfig().getString("history.command", "claimhistory");

        // Check if root command is history OR if subcommand is history
        if (commandLabel.equalsIgnoreCase(historyCmd) || (args.length > 0 && args[0].equalsIgnoreCase("history"))) {
            if (!player.hasPermission("griefpreventionshop.history")) {
                plugin.getMessageManager().sendMessage(player, "error.no-permission");
                return true;
            }
            plugin.getHistoryManager().openHistoryGUI(player);
            return true;
        }

        if (!player.hasPermission("griefpreventionshop.use")) {
            plugin.getMessageManager().sendMessage(player, "error.no-permission");
            return true;
        }

        // Open GUI
        plugin.getGuiManager().openShop(player);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("griefpreventionshop.admin")) {
                completions.add("reload");
            }
        }
        return completions;
    }
}
