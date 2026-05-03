package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import com.swaggerstudio.griefpreventionshop.utils.NumberUtil;
import com.swaggerstudio.griefpreventionshop.utils.ShopHolder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {

    private final GriefPreventionShop plugin;
    private FileConfiguration menuConfig;
    private final File menuFile;

    public HistoryManager(GriefPreventionShop plugin) {
        this.plugin = plugin;
        this.menuFile = new File(plugin.getDataFolder(), "menus/history-menu.yml");
        reload();
    }

    public void reload() {
        if (!menuFile.exists()) {
            menuFile.getParentFile().mkdirs();
            plugin.saveResource("menus/history-menu.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(menuFile);
    }

    public void addEntry(Player player, int amount, double price) {
        Connection conn = plugin.getDatabaseManager().getConnection();
        if (conn == null) {
            plugin.getLogger().warning("Could not log purchase: Database connection is not available.");
            return;
        }

        String sql = "INSERT INTO gpshop_history (player_uuid, timestamp, amount, price, world) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setLong(2, System.currentTimeMillis());
            ps.setInt(3, amount);
            ps.setDouble(4, price);
            ps.setString(5, player.getWorld().getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to log purchase to database: " + e.getMessage());
        }
    }

    public void openHistoryGUI(Player player) {
        String titleStr = menuConfig.getString("gui.title", "&6Purchase History");
        int size = menuConfig.getInt("gui.size", 54);
        Inventory inv = Bukkit.createInventory(new ShopHolder(), size, plugin.getMessageManager().parseColors(titleStr));

        Connection conn = plugin.getDatabaseManager().getConnection();
        if (conn == null) {
            player.sendMessage(plugin.getMessageManager().parseColors("&cHistory database is currently unavailable."));
            player.openInventory(inv);
            return;
        }

        String symbol = plugin.getConfigManager().getCurrencySymbol();
        String tz = plugin.getConfig().getString("history.timezone", "Asia/Kolkata");
        ZoneId zoneId = ZoneId.of(tz);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        ConfigurationSection itemSec = menuConfig.getConfigurationSection("gui.item");
        Material mat = Material.valueOf(itemSec.getString("material", "PAPER"));

        String sql = "SELECT * FROM gpshop_history WHERE player_uuid = ? ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setInt(2, size);
            
            ResultSet rs = ps.executeQuery();
            int slot = 0;
            while (rs.next()) {
                long ts = rs.getLong("timestamp");
                int amount = rs.getInt("amount");
                double price = rs.getDouble("price");
                String world = rs.getString("world");
                int id = rs.getInt("id");

                ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), zoneId);
                String formattedDate = dateTime.format(formatter);

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String name = itemSec.getString("display-name", "&eTransaction #<id>")
                            .replace("<id>", String.valueOf(id));
                    meta.displayName(plugin.getMessageManager().parseColors(name));

                    List<Component> lore = new ArrayList<>();
                    for (String line : itemSec.getStringList("lore")) {
                        lore.add(plugin.getMessageManager().parseColors(line
                                .replace("<date>", formattedDate)
                                .replace("<amount>", String.valueOf(amount))
                                .replace("<price>", NumberUtil.formatCurrency(price, symbol))
                                .replace("<world>", world)));
                    }
                    meta.lore(lore);
                    // 1. Apply all available flags
                    for (org.bukkit.inventory.ItemFlag flag : org.bukkit.inventory.ItemFlag.values()) {
                        meta.addItemFlags(flag);
                    }

                    // 2. Try to apply 1.20.5+ flags
                    try {
                        meta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
                        meta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf("HIDE_STORED_ENCHANTS"));
                    } catch (Exception ignored) {}

                    // 3. Clear attribute modifiers
                    try {
                        meta.setAttributeModifiers(com.google.common.collect.HashMultimap.create());
                    } catch (Exception ignored) {}

                    item.setItemMeta(meta);
                }
                inv.setItem(slot++, item);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load history from database: " + e.getMessage());
        }

        player.openInventory(inv);
    }
}
