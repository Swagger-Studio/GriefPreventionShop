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
    private ChatInputListener chatInputListener;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Initialize Managers (Early ones for banner usage)
        this.logManager = new LogManager(this);
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);

        // 2. Professional Startup Banner
        sendStartupBanner();

        // 3. Initial Dependency Check
        if (!checkDependencies()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 4. Initialize Remaining Managers
        this.economyManager = new EconomyManager(this);
        this.claimManager = new ClaimManager(this);
        this.logManager = new LogManager(this);
        this.historyManager = new HistoryManager(this);
        this.webhookManager = new WebhookManager(this);
        this.guiManager = new GUIManager(this); // GUIManager.reload() needs History/Webhook
        this.chatInputListener = new ChatInputListener(this);

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
        Bukkit.getConsoleSender().sendMessage(messageManager.parseColors(""));
        Bukkit.getConsoleSender().sendMessage(
                messageManager.parseColors(" &#FFC427&l╔═════════════════════════════════════════════════╗"));
        Bukkit.getConsoleSender().sendMessage(messageManager.parseColors(
                " &#FFC427&l║  &f&lGriefPreventionShop &8- &7v" + version + "                   &#FFC427&l║"));
        Bukkit.getConsoleSender().sendMessage(messageManager
                .parseColors(" &#FFC427&l║  &7Premium Claim Shop Addon by SwaggerStudio      &#FFC427&l║"));
        Bukkit.getConsoleSender().sendMessage(
                messageManager.parseColors(" &#FFC427&l╠═════════════════════════════════════════════════╣"));
        Bukkit.getConsoleSender().sendMessage(messageManager
                .parseColors(" &#FFC427&l║  &fStatus: &a&lONLINE                                 &#FFC427&l║"));
        Bukkit.getConsoleSender().sendMessage(messageManager
                .parseColors(" &#FFC427&l║  &fDiscord: &e&nhttps://discord.gg/Yxq6H8cb&r           &#FFC427&l║"));
        Bukkit.getConsoleSender().sendMessage(
                messageManager.parseColors(" &#FFC427&l╚═════════════════════════════════════════════════╝"));
        Bukkit.getConsoleSender().sendMessage(messageManager.parseColors(""));
    }

    private boolean checkDependencies() {
        boolean gp = Bukkit.getPluginManager().getPlugin("GriefPrevention") != null;
        boolean vault = Bukkit.getPluginManager().getPlugin("Vault") != null;

        if (!gp || !vault) {
            Bukkit.getConsoleSender()
                    .sendMessage(messageManager.parseColors(" &#FFC427&l║  &c&lCRITICAL ERROR: MISSING DEPENDENCIES"));
            if (!gp)
                Bukkit.getConsoleSender()
                        .sendMessage(messageManager.parseColors(" &#FFC427&l║  &e♯ &fGriefPrevention: &cNOT FOUND"));
            if (!vault)
                Bukkit.getConsoleSender()
                        .sendMessage(messageManager.parseColors(" &#FFC427&l║  &e♯ &fVault: &cNOT FOUND"));
            return false;
        }
        return true;
    }

    private void registerCommands() {
        // We will register commands dynamically based on config aliases
        configManager.registerCommands();
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(chatInputListener, this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
    }

    @Override
    public void onDisable() {
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

    public void reloadPlugin() {
        getLogger().info("Reloading GriefPreventionShop configs and menus...");
        configManager.reload();
        messageManager.reload();
        guiManager.reload();
        historyManager.reload();
        webhookManager.reload();
        
        // Re-register commands in case aliases changed
        registerCommands();
        getLogger().info("Reload complete! All modifications have been applied.");
    }
}
