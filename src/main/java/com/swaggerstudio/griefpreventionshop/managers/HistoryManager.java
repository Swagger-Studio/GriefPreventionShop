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
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HistoryManager {

    private final GriefPreventionShop plugin;
    private FileConfiguration historyConfig;
    private final File historyFile;
    private FileConfiguration menuConfig;
    private final File menuFile;

    public HistoryManager(GriefPreventionShop plugin) {
        this.plugin = plugin;
        this.historyFile = new File(plugin.getDataFolder(), "data/history.yml");
        this.menuFile = new File(plugin.getDataFolder(), "menus/history-menu.yml");
        reload();
    }

    public void reload() {
        if (!historyFile.exists()) {
            try {
                historyFile.getParentFile().mkdirs();
                historyFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create history.yml!");
            }
        }
        historyConfig = YamlConfiguration.loadConfiguration(historyFile);

        if (!menuFile.exists()) {
            menuFile.getParentFile().mkdirs();
            plugin.saveResource("menus/history-menu.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(menuFile);
    }

    public void addEntry(Player player, int amount, double price) {
        String uuid = player.getUniqueId().toString();
        List<Map<?, ?>> entries = historyConfig.getMapList(uuid);
        
        Map<String, Object> entry = new HashMap<>();
        entry.put("timestamp", System.currentTimeMillis());
        entry.put("amount", amount);
        entry.put("price", price);
        entry.put("world", player.getWorld().getName());

        entries.add(0, entry);

        int max = plugin.getConfig().getInt("history.max-entries", 50);
        if (entries.size() > max) {
            entries = entries.subList(0, max);
        }

        historyConfig.set(uuid, entries);
        save();
    }

    private void save() {
        try {
            historyConfig.save(historyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save history.yml!");
        }
    }

    public void openHistoryGUI(Player player) {
        String titleStr = menuConfig.getString("gui.title", "&6Purchase History");
        int size = menuConfig.getInt("gui.size", 54);
        Inventory inv = Bukkit.createInventory(new ShopHolder(), size, plugin.getMessageManager().parseColors(titleStr));

        String uuid = player.getUniqueId().toString();
        List<Map<?, ?>> entries = historyConfig.getMapList(uuid);
        String symbol = plugin.getConfigManager().getCurrencySymbol();

        // Timezone setup
        String tz = plugin.getConfig().getString("history.timezone", "Asia/Kolkata");
        ZoneId zoneId = ZoneId.of(tz);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        ConfigurationSection itemSec = menuConfig.getConfigurationSection("gui.item");
        Material mat = Material.valueOf(itemSec.getString("material", "PAPER"));

        for (int i = 0; i < Math.min(entries.size(), size); i++) {
            Map<?, ?> data = entries.get(i);
            long ts = (long) data.get("timestamp");
            int amount = (int) data.get("amount");
            double price = (double) data.get("price");
            String world = (String) data.get("world");

            ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), zoneId);
            String formattedDate = dateTime.format(formatter);

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String name = itemSec.getString("display-name", "&eTransaction #<id>")
                        .replace("<id>", String.valueOf(entries.size() - i));
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
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }
}
