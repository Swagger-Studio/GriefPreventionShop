package com.swaggerstudio.griefpreventionshop.provider;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

/** Detects which claim plugin is installed and builds the matching {@link ClaimProvider}. */
public final class ClaimProviderFactory {

    private ClaimProviderFactory() {
    }

    /**
     * Detection priority:
     * <ol>
     *     <li>GriefPrevention3D (Folia-compatible) — preferred when present.</li>
     *     <li>Standard GriefPrevention.</li>
     *     <li>Neither — returns {@code null} and the caller disables the plugin.</li>
     * </ol>
     *
     * @return the detected provider, or {@code null} if no supported claim plugin is installed.
     */
    public static ClaimProvider detect(Plugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();

        if (pm.getPlugin("GriefPrevention3D") != null) {
            return new GriefPrevention3DProvider();
        }
        if (pm.getPlugin("GriefPrevention") != null) {
            return new GriefPreventionProvider();
        }
        return null;
    }
}
