package com.swaggerstudio.griefpreventionshop;

import com.swaggerstudio.griefpreventionshop.managers.*;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class GriefPreventionShop extends JavaPlugin {

    private static GriefPreventionShop instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private EconomyManager economyManager;
    private ClaimManager claimManager;
    private GUIManager guiManager;
    private LogManager logManager;
    private HistoryManager historyManager;
    private WebhookManager webhookManager;
    private DatabaseManager databaseManager;
    private ChatInputListener chatInputListener;
    private CurrencyManager currencyManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Initialize Early Managers
        this.logManager = new LogManager(this);
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.currencyManager = new CurrencyManager(this);

        // 2. Initial Dependency Check
        if (!checkDependencies()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Initialize Remaining Managers (Economy needed for banner)
        this.economyManager = new EconomyManager(this);
        this.claimManager = new ClaimManager(this);
        this.historyManager = new HistoryManager(this);
        this.webhookManager = new WebhookManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.guiManager = new GUIManager(this);
        this.chatInputListener = new ChatInputListener(this);

        // 4. Professional Startup Banner
        sendStartupBanner();

        // 5. Register Commands & Listeners
        registerCommands();
        registerListeners();

        // 6. Initialize bStats
        int pluginId = 30792;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(
                new SimplePie("price_per_block", () -> String.valueOf(configManager.getPricePerBlock())));
    }

    private void sendStartupBanner() {
        String version = getDescription().getVersion();
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(
                messageManager.parseColors(" &#FFC427&l╔═══════════════════════════════════════════════════╗"));
        Bukkit.getConsoleSender().sendMessage(messageManager.parseColors(" &#FFC427&l║  &f&lGriefPreventionShop &8- &7v"
                + String.format("%-10s", version) + "                &#FFC427&l║"));
        Bukkit.getConsoleSender().sendMessage(messageManager
                .parseColors(" &#FFC427&l║  &7Premium Claim Shop Addon by SwaggerStudio        &#FFC427&l║"));
        Bukkit.getConsoleSender().sendMessage(
                messageManager.parseColors(" &#FFC427&l╠═══════════════════════════════════════════════════╣"));
        Bukkit.getConsoleSender().sendMessage(messageManager
                .parseColors(" &#FFC427&l║  &fStatus: &a&lONLINE                                   &#FFC427&l║"));
        Bukkit.getConsoleSender().sendMessage(messageManager
                .parseColors(" &#FFC427&l║  &fDiscord: &e&nhttps://discord.gg/Yxq6H8cb&r             &#FFC427&l║"));
        Bukkit.getConsoleSender().sendMessage(
                messageManager.parseColors(" &#FFC427&l╚═══════════════════════════════════════════════════╝"));
        Bukkit.getConsoleSender().sendMessage("");
    }

    private boolean checkDependencies() {
        boolean gp = Bukkit.getPluginManager().getPlugin("GriefPrevention") != null;
        String active = getCurrencyManager().getActiveCurrency();
        boolean econPresent = true;
        String missingEcon = "";

        if (active.equals("Vault") && Bukkit.getPluginManager().getPlugin("Vault") == null) {
            econPresent = false;
            missingEcon = "Vault";
        } else if (active.equals("PlayerPoints") && Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
            econPresent = false;
            missingEcon = "PlayerPoints";
        } else if (active.equals("ExcellentEconomy") &&
                Bukkit.getPluginManager().getPlugin("ExcellentEconomy") == null &&
                Bukkit.getPluginManager().getPlugin("CoinsEngine") == null) {
            econPresent = false;
            missingEcon = "ExcellentEconomy (or CoinsEngine)";
        }

        if (!gp || !econPresent) {
            Bukkit.getConsoleSender()
                    .sendMessage(messageManager.parseColors(" &#FFC427&l║  &c&lCRITICAL ERROR: MISSING DEPENDENCIES"));
            if (!gp)
                Bukkit.getConsoleSender()
                        .sendMessage(messageManager.parseColors(" &#FFC427&l║  &e♯ &fGriefPrevention: &cNOT FOUND"));
            if (!econPresent)
                Bukkit.getConsoleSender().sendMessage(messageManager
                        .parseColors(" &#FFC427&l║  &e♯ &f" + missingEcon + ": &cNOT FOUND (Active Currency)"));
            return false;
        }
        return true;
    }

    private void registerCommands() {
        configManager.registerCommands();
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(chatInputListener, this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("GriefPreventionShop has been disabled!");
    }

    public static GriefPreventionShop getInstance() {
        return instance;
    }

    public ChatInputListener getChatInputListener() {
        return chatInputListener;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public WebhookManager getWebhookManager() {
        return webhookManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    public void reloadPlugin() {
        getLogger().info("Reloading GriefPreventionShop configs and menus...");
        configManager.reload();
        messageManager.reload();
        historyManager.reload();
        webhookManager.reload();
        currencyManager.reload();

        // Re-initialize economy manager to refresh hooks
        this.economyManager = new EconomyManager(this);

        // Re-initialize GUI manager
        this.guiManager = new GUIManager(this);

        // Send banner again for visual confirmation of hooks
        sendStartupBanner();

        // Re-register commands in case aliases changed
        registerCommands();
        getLogger().info("Reload complete! All modifications have been applied.");
    }
}
