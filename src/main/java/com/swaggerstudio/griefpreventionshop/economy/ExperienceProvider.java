package com.swaggerstudio.griefpreventionshop.economy;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import org.bukkit.entity.Player;

public class ExperienceProvider implements EconomyProvider {

    public ExperienceProvider(GriefPreventionShop plugin) {
    }

    @Override
    public String getName() {
        return "Experience";
    }

    @Override
    public boolean isHooked() {
        return true;
    }

    @Override
    public String getFailureReason() {
        return null;
    }

    @Override
    public boolean hasBalance(Player player, double amount) {
        return player.getTotalExperience() >= (int) amount;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        int exp = player.getTotalExperience();
        if (exp >= (int) amount) {
            player.setTotalExperience(exp - (int) amount);
            return true;
        }
        return false;
    }

    @Override
    public double getBalance(Player player) {
        return player.getTotalExperience();
    }
}
