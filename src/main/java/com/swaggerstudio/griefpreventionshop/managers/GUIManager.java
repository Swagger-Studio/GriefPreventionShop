package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import com.swaggerstudio.griefpreventionshop.utils.NumberUtil;
import com.swaggerstudio.griefpreventionshop.utils.ShopHolder;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GUIManager {

    private final GriefPreventionShop plugin;
    private FileConfiguration shopConfig;
    private final Map<UUID, Integer> playerCart = new ConcurrentHashMap<>();

    public GUIManager(GriefPreventionShop plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "shop-menu.yml");
        if (!file.exists()) {
            plugin.saveResource("shop-menu.yml", false);
        }
        shopConfig = YamlConfiguration.loadConfiguration(file);
        plugin.getHistoryManager().reload();
    }

    public void openShop(Player player) {
        playerCart.putIfAbsent(player.getUniqueId(), 1);
        updateInventory(player);
    }

    public void updateInventory(Player player) {
        String menuTitle = shopConfig.getString("gui.title", "Shop");
        int menuSize = shopConfig.getInt("gui.size", 27);
        int currentAmount = playerCart.getOrDefault(player.getUniqueId(), 0);

        Inventory inventory = Bukkit.createInventory(new ShopHolder(), menuSize, plugin.getMessageManager().parseColors(menuTitle.replace("<amount>", String.valueOf(currentAmount))));

        // 1. Load Add Buttons
        loadSection(inventory, "gui.buttons.add", currentAmount);
        // 2. Load Remove Buttons
        loadSection(inventory, "gui.buttons.remove", currentAmount);
        // 3. Load Custom Input
        loadSingleItem(inventory, "gui.custom-input", currentAmount);
        // 4. Load Info
        loadSingleItem(inventory, "gui.info", currentAmount);
        // 5. Load Nav Cancel
        loadSingleItem(inventory, "gui.navigation.cancel", currentAmount);
        // 6. Load Nav Confirm
        loadSingleItem(inventory, "gui.navigation.confirm", currentAmount);
        // 7. Load Stats
        loadStatsItem(inventory, player, currentAmount);
        // 8. Global Filler
        fillBackground(inventory);

        player.openInventory(inventory);
    }

    private void fillBackground(Inventory inv) {
        ConfigurationSection fillerSec = shopConfig.getConfigurationSection("gui.filler");
        if (fillerSec == null) return;

        ItemStack filler = createItem(fillerSec, 0, null);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, filler);
            }
        }
    }

    private void loadStatsItem(Inventory inv, Player player, int currentAmount) {
        ConfigurationSection statsSec = shopConfig.getConfigurationSection("gui.stats");
        if (statsSec == null) return;

        int slot = statsSec.getInt("slot");
        inv.setItem(slot, createItem(statsSec, currentAmount, player));
    }

    private void loadSection(Inventory inv, String path, int currentAmount) {
        ConfigurationSection section = shopConfig.getConfigurationSection(path);
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSec = section.getConfigurationSection(key);
            if (itemSec == null) continue;
            
            int slot = itemSec.getInt("slot");
            inv.setItem(slot, createItem(itemSec, currentAmount, null));
        }
    }

    private void loadSingleItem(Inventory inv, String path, int currentAmount) {
        ConfigurationSection section = shopConfig.getConfigurationSection(path);
        if (section == null) return;

        int slot = section.getInt("slot");
        inv.setItem(slot, createItem(section, currentAmount, null));
    }

    private ItemStack createItem(ConfigurationSection section, int currentAmount, Player owner) {
        Material material = Material.valueOf(section.getString("material", "STONE").toUpperCase());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Special handling for Player Head
            if (material == Material.PLAYER_HEAD && owner != null && meta instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(owner);
            }

            String name = section.getString("display-name", "");
            meta.displayName(plugin.getMessageManager().parseColors(replacePlaceholders(name, currentAmount, owner)));

            List<String> lore = section.getStringList("lore");
            meta.lore(lore.stream()
                    .map(line -> replacePlaceholders(line, currentAmount, owner))
                    .map(line -> plugin.getMessageManager().parseColors(line))
                    .collect(Collectors.toList()));
            
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String replacePlaceholders(String text, int amount, Player player) {
        double unitPrice = plugin.getConfigManager().getPricePerBlock();
        double total = Math.abs(amount) * unitPrice;
        String symbol = plugin.getConfigManager().getCurrencySymbol();
        
        String result = text.replace("<amount>", String.valueOf(amount))
                   .replace("<unit_price>", NumberUtil.formatCurrency(unitPrice, symbol))
                   .replace("<total>", NumberUtil.formatCurrency(total, symbol));

        if (player != null) {
            double balance = plugin.getEconomyManager().getBalance(player);
            int blocks = plugin.getClaimManager().getClaimBlocks(player);
            result = result.replace("<player>", player.getName())
                           .replace("<money>", NumberUtil.formatCurrency(balance, symbol))
                           .replace("<blocks>", String.valueOf(blocks));
        }
        
        return result;
    }

    public void handleAction(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        int currentAmount = playerCart.getOrDefault(uuid, 0);

        // Check Add
        if (checkAndModify(player, "gui.buttons.add", slot, currentAmount, true)) return;
        // Check Remove
        if (checkAndModify(player, "gui.buttons.remove", slot, currentAmount, false)) return;
        // Check Custom
        if (checkStatic(player, "gui.custom-input", slot)) {
            player.closeInventory();
            plugin.getChatInputListener().startSession(player);
            return;
        }
        // Check Cancel
        if (checkStatic(player, "gui.navigation.cancel", slot)) {
            player.closeInventory();
            playerCart.remove(uuid);
            plugin.getMessageManager().sendMessage(player, "shop.cancelled");
            return;
        }
        // Check Confirm
        if (checkStatic(player, "gui.navigation.confirm", slot)) {
            // Validation: Empty Cart
            if (currentAmount <= 0) {
                plugin.getMessageManager().sendMessage(player, "error.invalid-amount");
                return;
            }

            // Validation: Limits
            int min = plugin.getConfigManager().getMinPurchase();
            int max = plugin.getConfigManager().getMaxPurchase();

            if (currentAmount < min || currentAmount > max) {
                plugin.getMessageManager().sendMessage(player, "error.limit-exceeded", 
                    "<min>", String.valueOf(min), 
                    "<max>", String.valueOf(max));
                return;
            }

            player.closeInventory();
            processPurchase(player, currentAmount);
            playerCart.remove(uuid);
            return;
        }
    }

    private boolean checkAndModify(Player player, String path, int slot, int current, boolean add) {
        ConfigurationSection section = shopConfig.getConfigurationSection(path);
        if (section == null) return false;

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSec = section.getConfigurationSection(key);
            if (itemSec != null && itemSec.getInt("slot") == slot) {
                int mod = Math.abs(itemSec.getInt("amount", 0));
                int next = add ? current + mod : Math.max(0, current - mod);
                playerCart.put(player.getUniqueId(), next);
                updateInventory(player);
                return true;
            }
        }
        return false;
    }

    private boolean checkStatic(Player player, String path, int slot) {
        ConfigurationSection section = shopConfig.getConfigurationSection(path);
        return section != null && section.getInt("slot") == slot;
    }

    private void processPurchase(Player player, int amount) {
        double pricePerBlock = plugin.getConfigManager().getPricePerBlock();
        double totalPrice = amount * pricePerBlock;
        String symbol = plugin.getConfigManager().getCurrencySymbol();

        if (!plugin.getEconomyManager().hasBalance(player, totalPrice)) {
            plugin.getMessageManager().playSound(player, "insufficient-funds");
            plugin.getMessageManager().sendMessage(player, "error.not-enough-money", "<price>", NumberUtil.formatCurrency(totalPrice, symbol));
            return;
        }

        if (plugin.getEconomyManager().withdraw(player, totalPrice)) {
            plugin.getClaimManager().addClaimBlocks(player, amount);
            plugin.getLogManager().logPurchase(player.getName(), amount, totalPrice, player.getWorld().getName());
            plugin.getHistoryManager().addEntry(player, amount, totalPrice);
            plugin.getWebhookManager().sendPurchaseNotification(player, amount, totalPrice);
            plugin.getMessageManager().playSound(player, "purchase");
            plugin.getMessageManager().sendMessage(player, "shop.purchase-success", "<amount>", String.valueOf(amount), "<price>", NumberUtil.formatCurrency(totalPrice, symbol));
        } else {
            plugin.getMessageManager().sendMessage(player, "error.transaction-failed");
        }
    }

    public void setAmount(Player player, int amount) {
        playerCart.put(player.getUniqueId(), amount);
        updateInventory(player);
    }
}
