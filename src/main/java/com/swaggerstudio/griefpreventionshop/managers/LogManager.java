package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class LogManager {

    private final GriefPreventionShop plugin;
    private final File logsFolder;

    public LogManager(GriefPreventionShop plugin) {
        this.plugin = plugin;
        this.logsFolder = new File(plugin.getDataFolder(), "logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
    }

    public void logPurchase(String playerName, int amount, double price, String world) {
        if (!plugin.getConfig().getBoolean("logging.enabled", true)) return;

        CompletableFuture.runAsync(() -> {
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            File logFile = new File(logsFolder, date + ".txt");
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String entry = String.format("[%s] PLAYER: %s | BLOCKS: %d | PRICE: %.2f | WORLD: %s\n",
                    time, playerName, amount, price, world);

            try (FileWriter fw = new FileWriter(logFile, true)) {
                fw.write(entry);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to write to log file: " + e.getMessage());
            }
        });
    }
}
