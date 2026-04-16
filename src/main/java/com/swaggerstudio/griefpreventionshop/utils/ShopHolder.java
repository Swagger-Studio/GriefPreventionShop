package com.swaggerstudio.griefpreventionshop.utils;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ShopHolder implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
        return null; // Not needed for identification
    }
}
