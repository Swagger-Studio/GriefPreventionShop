package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {

    private final GriefPreventionShop plugin;
    private FileConfiguration messages;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public MessageManager(GriefPreventionShop plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public void sendMessage(Player player, String path, String... placeholders) {
        ConfigurationSection section = messages.getConfigurationSection("messages." + path);
        if (section == null || !section.getBoolean("enabled", true)) return;

        String typeStr = section.getString("type", "chat").toLowerCase();
        String[] types = typeStr.split("\\s*,\\s*");

        for (String type : types) {
            switch (type) {
                case "title":
                    handleTitle(player, section, placeholders);
                    break;
                case "actionbar":
                    handleActionBar(player, section, placeholders);
                    break;
                default:
                    String msg = section.getString("message", "");
                    player.sendMessage(parseColors(applyPlaceholders(msg, placeholders)));
                    break;
            }
        }
    }

    private void handleTitle(Player player, ConfigurationSection section, String... placeholders) {
        String main, sub;
        int fadeIn = 10, stay = 70, fadeOut = 20;

        if (section.isConfigurationSection("title")) {
            ConfigurationSection titleSec = section.getConfigurationSection("title");
            main = titleSec.getString("main", "");
            sub = titleSec.getString("sub", "");
            fadeIn = titleSec.getInt("duration.fade_in", 10);
            stay = titleSec.getInt("duration.stay", 70);
            fadeOut = titleSec.getInt("duration.fade_out", 20);
        } else {
            main = section.getString("message", "");
            sub = section.getString("subtitle", "");
        }

        Component mainComp = parseColors(applyPlaceholders(main, placeholders));
        Component subComp = parseColors(applyPlaceholders(sub, placeholders));
        
        Title.Times times = Title.Times.times(
                java.time.Duration.ofMillis(fadeIn),
                java.time.Duration.ofMillis(stay),
                java.time.Duration.ofMillis(fadeOut)
        );
        
        player.showTitle(Title.title(mainComp, subComp, times));
    }

    private void handleActionBar(Player player, ConfigurationSection section, String... placeholders) {
        String msg;
        if (section.isConfigurationSection("actionbar")) {
            msg = section.getConfigurationSection("actionbar").getString("message", "");
        } else {
            msg = section.getString("message", "");
        }
        player.sendActionBar(parseColors(applyPlaceholders(msg, placeholders)));
    }

    private String applyPlaceholders(String text, String... placeholders) {
        if (text == null || text.isEmpty() || placeholders.length == 0 || placeholders.length % 2 != 0) {
            return text;
        }
        for (int i = 0; i < placeholders.length; i += 2) {
            text = text.replace(placeholders[i], placeholders[i + 1]);
        }
        return text;
    }

    public Component parseColors(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // 1. Handle Hex &#RRGGBB -> MiniMessage <color:#RRGGBB>
        Matcher matcher = hexPattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<color:#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);
        text = sb.toString();

        // 2. Handle Legacy & -> MiniMessage tags
        // This ensures compatibility with both standard & codes and modern tags.
        text = text.replace("&0", "<black>")
                   .replace("&1", "<dark_blue>")
                   .replace("&2", "<dark_green>")
                   .replace("&3", "<dark_aqua>")
                   .replace("&4", "<dark_red>")
                   .replace("&5", "<dark_purple>")
                   .replace("&6", "<gold>")
                   .replace("&7", "<gray>")
                   .replace("&8", "<dark_gray>")
                   .replace("&9", "<blue>")
                   .replace("&a", "<green>")
                   .replace("&b", "<aqua>")
                   .replace("&c", "<red>")
                   .replace("&d", "<light_purple>")
                   .replace("&e", "<yellow>")
                   .replace("&f", "<white>")
                   .replace("&l", "<bold>")
                   .replace("&m", "<strikethrough>")
                   .replace("&n", "<underlined>")
                   .replace("&o", "<italic>")
                   .replace("&r", "<reset>");

        return miniMessage.deserialize("<!italic>" + text);
    }
}
