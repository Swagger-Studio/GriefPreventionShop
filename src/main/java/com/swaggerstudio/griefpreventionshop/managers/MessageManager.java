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

        String type = section.getString("type", "chat").toLowerCase();
        String msg = section.getString("message", "");
        String subtitle = section.getString("subtitle", "");

        // Apply placeholders
        if (placeholders.length > 0 && placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                msg = msg.replace(placeholders[i], placeholders[i + 1]);
                subtitle = subtitle.replace(placeholders[i], placeholders[i + 1]);
            }
        }

        Component component = parseColors(msg);
        Component subComponent = parseColors(subtitle);

        switch (type) {
            case "title":
                player.showTitle(Title.title(component, subComponent));
                break;
            case "actionbar":
                player.sendActionBar(component);
                break;
            case "all":
                player.sendMessage(component);
                player.sendActionBar(component);
                player.showTitle(Title.title(component, subComponent));
                break;
            default:
                player.sendMessage(component);
                break;
        }
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

        // 2. Handle Legacy & -> § (Adventure handles § automatically if parsed correctly, 
        // but MiniMessage doesn't. We'll use LegacyComponentSerializer to bridge if needed, 
        // OR just rely on MiniMessage's ability to handle tags.)
        
        // A better approach for hybrid support:
        return miniMessage.deserialize(text.replace("&", "§"));
    }
}
