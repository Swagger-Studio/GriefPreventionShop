package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import com.swaggerstudio.griefpreventionshop.utils.NumberUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WebhookManager {

    private final GriefPreventionShop plugin;
    private FileConfiguration config;
    private final File file;
    private final HttpClient httpClient;

    public WebhookManager(GriefPreventionShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "webhook.yml");
        this.httpClient = HttpClient.newHttpClient();
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("webhook.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void sendPurchaseNotification(Player player, int amount, double price) {
        if (!config.getBoolean("enabled", false)) return;

        String url = config.getString("url", "");
        if (url == null || url.isEmpty() || url.contains("YOUR_WEBHOOK_URL")) return;

        CompletableFuture.runAsync(() -> {
            try {
                String payload = buildJsonPayload(player, amount, price);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    private String buildJsonPayload(Player player, int amount, double price) {
        String symbol = plugin.getConfigManager().getCurrencySymbol();
        String formattedPrice = NumberUtil.formatCurrency(price, symbol);
        Location loc = player.getLocation();

        String title = config.getString("embed.title", "New Purchase");
        int color = hexToInt(config.getString("embed.color", "#FFC427"));
        String footer = config.getString("footer", "GriefPreventionShop");

        StringBuilder json = new StringBuilder();
        json.append("{ \"embeds\": [{ ");
        json.append("\"title\": \"").append(escapeJson(title)).append("\", ");
        json.append("\"color\": ").append(color).append(", ");
        
        // Thumbnail (Avatar)
        if (config.getBoolean("embed.thumbnail", true)) {
            String avatarUrl = "https://mc-heads.net/avatar/" + player.getUniqueId();
            json.append("\"thumbnail\": { \"url\": \"").append(avatarUrl).append("\" }, ");
        }

        // Fields
        json.append("\"fields\": [");
        @SuppressWarnings("unchecked")
        List<ConfigurationSection> fields = (List<ConfigurationSection>) config.getList("embed.fields");
        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                Object obj = fields.get(i);
                if (obj instanceof ConfigurationSection) {
                    ConfigurationSection field = (ConfigurationSection) obj;
                    String name = field.getString("name", "");
                    String value = field.getString("value", "")
                            .replace("<player>", player.getName())
                            .replace("<amount>", String.valueOf(amount))
                            .replace("<price>", formattedPrice)
                            .replace("<world>", loc.getWorld().getName())
                            .replace("<x>", String.valueOf(loc.getBlockX()))
                            .replace("<y>", String.valueOf(loc.getBlockY()))
                            .replace("<z>", String.valueOf(loc.getBlockZ()));
                    boolean inline = field.getBoolean("inline", true);

                    json.append("{ \"name\": \"").append(escapeJson(name)).append("\", ");
                    json.append("\"value\": \"").append(escapeJson(value)).append("\", ");
                    json.append("\"inline\": ").append(inline).append(" }");
                    if (i < fields.size() - 1) json.append(", ");
                }
            }
        }
        json.append("], ");
        
        // Footer
        json.append("\"footer\": { \"text\": \"").append(escapeJson(footer)).append("\" }");
        
        json.append(" }] }");
        return json.toString();
    }

    private int hexToInt(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 16761895; // Default color
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"");
    }
}
