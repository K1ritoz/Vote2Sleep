package com.github.k1ritoz.vote2Sleep.platform;

import org.bukkit.Bukkit;

public class PlatformDetector {
    public static PlatformAdapter createAdapter() {

        // Check for Folia first (most specific)
        if (isFoliaPresent()) {
            return new FoliaAdapter();
        }

        // Check for Purpur
        if (isPurpurPresent()) {
            return new PurpurAdapter();
        }

        // Check for Paper
        if (isPaperPresent()) {
            return new PaperAdapter();
        }

        // Check for Sponge
        if (isSpongePresent()) {
            return new SpongeAdapter();
        }

        // Fallback to Bukkit/Spigot
        return new BukkitAdapter();
    }

    private static boolean isFoliaPresent() {
        try {
            // Check for Folia-specific classes
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");

            // Try to access Folia schedulers via reflection to be safe
            try {
                Bukkit.class.getMethod("getGlobalRegionScheduler");
                Bukkit.class.getMethod("getRegionScheduler");
                Bukkit.class.getMethod("getAsyncScheduler");
                return true;
            } catch (NoSuchMethodException e) {
                return false;
            }
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isPurpurPresent() {
        try {
            // Try to detect via Purpur-specific classes
            Class.forName("org.purpurmc.purpur.PurpurConfig");
            Class.forName("org.purpurmc.purpur.PurpurServer");
            return true;
        } catch (ClassNotFoundException e) {
            // Fallback: check if version contains "purpur"
            String version = Bukkit.getVersion().toLowerCase();
            String name = Bukkit.getName().toLowerCase();
            if (version.contains("purpur") || name.contains("purpur")) {
                return true;
            }
            return false;
        }
    }

    private static boolean isPaperPresent() {
        try {
            // Check for newer Paper config
            Class.forName("io.papermc.paper.configuration.Configuration");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                // Check for older Paper config
                Class.forName("com.destroystokyo.paper.PaperConfig");
                return true;
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }
    }

    private static boolean isSpongePresent() {
        try {
            Class.forName("org.spongepowered.api.Sponge");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}