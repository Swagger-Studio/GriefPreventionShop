package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.entity.Player;

public class ClaimManager {

    public ClaimManager(GriefPreventionShop plugin) {
    }

    public void addClaimBlocks(Player player, int amount) {
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() + amount);
        
        int currentBonus = playerData.getBonusClaimBlocks();
        playerData.setBonusClaimBlocks(currentBonus + amount);
        
        // Ensure changes are saved (GP usually saves periodically, but good to check)
        GriefPrevention.instance.dataStore.savePlayerData(player.getUniqueId(), playerData);
    }

    public int getClaimBlocks(Player player) {
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        return playerData.getAccruedClaimBlocks() + playerData.getBonusClaimBlocks();
    }
}
