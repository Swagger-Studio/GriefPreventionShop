package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatInputListener implements Listener {

    private final GriefPreventionShop plugin;
    private final Map<UUID, Long> activeSessions = new ConcurrentHashMap<>();
    private final long TIMEOUT_MS = 30000; // 30 seconds

    public ChatInputListener(GriefPreventionShop plugin) {
        this.plugin = plugin;
    }

    public void startSession(Player player) {
        activeSessions.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!activeSessions.containsKey(uuid)) return;

        event.setCancelled(true);

        long startTime = activeSessions.remove(uuid);
        if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
            plugin.getMessageManager().sendMessage(player, "shop.cancelled-timeout");
            return;
        }

        String input = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (input.equalsIgnoreCase("cancel")) {
            plugin.getMessageManager().sendMessage(player, "shop.cancelled");
            return;
        }

        try {
            int amount = Integer.parseInt(input);
            int min = plugin.getConfigManager().getMinPurchase();
            int max = plugin.getConfigManager().getMaxPurchase();

            if (amount < min || amount > max) {
                plugin.getMessageManager().sendMessage(player, "error.limit-exceeded", 
                    "<min>", String.valueOf(min), 
                    "<max>", String.valueOf(max));
                return;
            }

            // Return to GUI with the new amount
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getGuiManager().setAmount(player, amount);
                plugin.getMessageManager().sendMessage(player, "shop.amount-set", "<amount>", String.valueOf(amount));
            });

        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "error.invalid-amount");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activeSessions.remove(event.getPlayer().getUniqueId());
    }
}
