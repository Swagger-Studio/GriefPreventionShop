package com.swaggerstudio.griefpreventionshop;

import com.swaggerstudio.griefpreventionshop.managers.*;
import com.swaggerstudio.griefpreventionshop.provider.ClaimProviderFactory;
import com.swaggerstudio.griefpreventionshop.utils.SchedulerUtil;
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

        // 1. Initialize Core Managers
        this.logManager = new LogManager(this);
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.currencyManager = new CurrencyManager(this);

        // Initialize claim and economy managers early to detect hooks
        this.economyManager = new EconomyManager(this);
        this.claimManager = new ClaimManager(this);

        // 2. Log Hook Statuses
        logHookStatuses();

        // 3. Initial Dependency Check
        if (!checkDependencies()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 4. Initialize Remaining Managers
        this.historyManager = new HistoryManager(this);
        this.webhookManager = new WebhookManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.guiManager = new GUIManager(this);
        this.chatInputListener = new ChatInputListener(this);

        // 5. Professional Startup Banner
        sendStartupBanner();

        // 6. Register Commands & Listeners
        registerCommands();
        registerListeners();

        // 7. Initialize bStats
        int pluginId = 30792;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(
                new SimplePie("price_per_block", () -> String.valueOf(configManager.getPricePerBlock())));
    }

    private void sendStartupBanner() {
        String version = getDescription().getVersion();
        String provider = claimManager != null ? claimManager.getProviderName() : "None";
        String software = Bukkit.getName();
        String folia = SchedulerUtil.isFolia() ? "&aYes &7(region-threaded)" : "&7No";

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(
                messageManager.parseColors(" &#FFC427&l╔═══════════════════════════════════════════════════╗"));
        Bukkit.getConsoleSender().sendMessage(messageManager.parseColors(" &#FFC427&l║  &f&lGriefPreventionShop &8- &7v"
                + String.format("%-10s", version) + "                &#FFC427&l║"));
        Bukkit.getConsoleSender().sendMessage(messageManager
                .parseColors(" &#FFC427&l║  &7Premium Claim Shop Addon by Kanhaiya             &#FFC427&l║"));
        Bukkit.getConsoleSender().sendMessage(
                messageManager.parseColors(" &#FFC427&l╠═══════════════════════════════════════════════════╣"));
        Bukkit.getConsoleSender().sendMessage(messageManager
                .parseColors(" &#FFC427&l║  &fStatus: &a&lONLINE"));
        Bukkit.getConsoleSender().sendMessage(messageManager
                .parseColors(" &#FFC427&l║  &fProvider: &e" + provider));
        Bukkit.getConsoleSender().sendMessage(messageManager
                .parseColors(" &#FFC427&l║  &fServer: &e" + software));
        Bukkit.getConsoleSender().sendMessage(messageManager
                .parseColors(" &#FFC427&l║  &fFolia: " + folia));
        Bukkit.getConsoleSender().sendMessage(messageManager
                .parseColors(" &#FFC427&l║  &fWebsite: &e&nhttps://kanhaiya.wtf/"));
        Bukkit.getConsoleSender().sendMessage(messageManager
                .parseColors(" &#FFC427&l║  &fDiscord: &e&nhttps://discord.gg/Yxq6H8cb"));
        Bukkit.getConsoleSender().sendMessage(
                messageManager.parseColors(" &#FFC427&l╚═══════════════════════════════════════════════════╝"));
        Bukkit.getConsoleSender().sendMessage("");
    }

    private boolean checkDependencies() {
        boolean gp = claimManager != null && claimManager.isReady();
        boolean econ = economyManager != null && economyManager.isHooked();

        if (!gp || !econ) {
            Bukkit.getConsoleSender().sendMessage(messageManager.parseColors(" &#FFC427&l║  &c&lCRITICAL ERROR: MISSING DEPENDENCIES"));
            if (!gp) {
                Bukkit.getConsoleSender().sendMessage(messageManager.parseColors(
                        " &#FFC427&l║  &e♯ &fGriefPrevention / GriefPrevention3D: &cNEITHER FOUND"));
            }
            if (!econ) {
                String missingEcon = getCurrencyManager().getActiveCurrency();
                boolean vaultInstalled = Bukkit.getPluginManager().getPlugin("Vault") != null
                        || Bukkit.getPluginManager().getPlugin("VaultUnlocked") != null;

                if ((missingEcon.equalsIgnoreCase("Vault") || missingEcon.equalsIgnoreCase("VaultUnlocked")) && vaultInstalled) {
                    Bukkit.getConsoleSender().sendMessage(messageManager.parseColors(
                            " &#FFC427&l║  &e♯ &f" + missingEcon + ": &ePlugin is loaded, but has NO registered Economy Provider!"));
                    Bukkit.getConsoleSender().sendMessage(messageManager.parseColors(
                            " &#FFC427&l║  &e♯ &7Vault is only an API bridge. Install &aTheNewEconomy &7(Folia) or &aXConomy &7(Paper/Folia)"));
                    Bukkit.getConsoleSender().sendMessage(messageManager.parseColors(
                            " &#FFC427&l║  &e♯ &7Or set &factive-currency: \"auto\" &7or &f\"Experience\" &7in currencies.yml."));
                } else {
                    Bukkit.getConsoleSender().sendMessage(messageManager.parseColors(
                            " &#FFC427&l║  &e♯ &f" + missingEcon + ": &cNOT FOUND (Active Currency)"));
                }
            }
            return false;
        }
        return true;
    }

    private void logHookStatuses() {
        String activeEcon = getCurrencyManager().getActiveCurrency();
        boolean econHooked = economyManager != null && economyManager.isHooked();
        
        // Claim Provider Hook Status
        String claimProvider = claimManager != null ? claimManager.getProviderName() : "None";
        if (claimManager != null && claimManager.isReady() && !claimProvider.equals("None")) {
            Bukkit.getConsoleSender().sendMessage(messageManager.parseColors("&a✔ &fClaim Provider: &a" + claimProvider));
        } else {
            Bukkit.getConsoleSender().sendMessage(messageManager.parseColors("&c✘ &fClaim Provider: &cNone"));
        }

        // Economy Provider Hook Status
        if (econHooked) {
            String displayedEcon = economyManager.getProviderName();
            if (activeEcon.equals("ExcellentEconomy") && !displayedEcon.equals("ExcellentEconomy")) {
                displayedEcon = "ExcellentEconomy (CoinsEngine fallback)";
            }
            Bukkit.getConsoleSender().sendMessage(messageManager.parseColors("&a✔ &fEconomy Provider: &a" + displayedEcon));
            Bukkit.getConsoleSender().sendMessage(messageManager.parseColors("&a✔ &fHook Status: &aSuccessfully Hooked"));
        } else {
            Bukkit.getConsoleSender().sendMessage(messageManager.parseColors("&c✘ &fEconomy Provider: &c" + activeEcon));
            Bukkit.getConsoleSender().sendMessage(messageManager.parseColors("&c✘ &fHook Status: &cFailed"));
            String reason = economyManager != null ? economyManager.getFailureReason() : "Economy provider could not be initialized.";
            Bukkit.getConsoleSender().sendMessage(messageManager.parseColors("&7Reason: " + reason));
            boolean vaultInstalled = Bukkit.getPluginManager().getPlugin("Vault") != null
                    || Bukkit.getPluginManager().getPlugin("VaultUnlocked") != null;
            if ((activeEcon.equalsIgnoreCase("Vault") || activeEcon.equalsIgnoreCase("VaultUnlocked")) && vaultInstalled) {
                Bukkit.getConsoleSender().sendMessage(messageManager.parseColors("&eTip: Run /vault-info to see economy status. Install &aTheNewEconomy &eor &aXConomy&e!"));
            }
        }
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

        // Log hook statuses on reload
        logHookStatuses();

        // Re-initialize GUI manager
        this.guiManager = new GUIManager(this);

        // Send banner again for visual confirmation of hooks
        sendStartupBanner();

        // Re-register commands in case aliases changed
        registerCommands();
        getLogger().info("Reload complete! All modifications have been applied.");
    }
}
