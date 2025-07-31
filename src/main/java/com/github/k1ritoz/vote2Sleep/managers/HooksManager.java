package com.github.k1ritoz.vote2Sleep.managers;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.Bukkit;

public class HooksManager {

    private final Vote2Sleep plugin;
    private boolean placeholderAPIHooked = false;
    private boolean worldGuardHooked = false;
    private boolean essentialsHooked = false;
    private Vote2SleepPlaceholders placeholderExpansion = null;

    public HooksManager(Vote2Sleep plugin) {
        this.plugin = plugin;
    }

    public void initializeHooks() {
        // PlaceholderAPI - with improved detection and error handling
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                // Check if PlaceholderAPI is actually enabled
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    // Try to access PlaceholderAPI classes to ensure they're available
                    Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
                    Class.forName("me.clip.placeholderapi.PlaceholderAPI");

                    // Create and register the expansion
                    placeholderExpansion = new Vote2SleepPlaceholders(plugin);

                    // Try to register with error handling
                    if (placeholderExpansion.register()) {
                        placeholderAPIHooked = true;
                        plugin.getLogger().info("Successfully hooked into PlaceholderAPI");
                    } else {
                        plugin.getLogger().warning("Failed to register PlaceholderAPI expansion");
                    }
                } else {
                    plugin.getLogger().info("PlaceholderAPI found but not enabled");
                }
            } catch (ClassNotFoundException e) {
                plugin.getLogger().warning("PlaceholderAPI classes not found: " + e.getMessage());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into PlaceholderAPI: " + e.getMessage());
                e.printStackTrace(); // More detailed error logging
            }
        } else {
            plugin.getLogger().info("PlaceholderAPI not found");
        }

        // WorldGuard
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
                    worldGuardHooked = true;
                    plugin.getLogger().info("WorldGuard detected - region checking enabled");
                } else {
                    plugin.getLogger().info("WorldGuard found but not enabled");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error checking WorldGuard: " + e.getMessage());
            }
        }

        // Essentials
        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
            try {
                if (Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
                    essentialsHooked = true;
                    plugin.getLogger().info("Essentials detected - AFK checking enabled");
                } else {
                    plugin.getLogger().info("Essentials found but not enabled");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error checking Essentials: " + e.getMessage());
            }
        }

        // Log final hook status
        plugin.getLogger().info("Hooks initialized - PlaceholderAPI: " + placeholderAPIHooked +
                ", WorldGuard: " + worldGuardHooked +
                ", Essentials: " + essentialsHooked);
    }

    /**
     * Re-initialize hooks (useful for plugin reloads)
     */
    public void reinitializeHooks() {
        // Unregister existing expansion if it exists
        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.unregister();
            } catch (Exception e) {
                plugin.getLogger().warning("Error unregistering PlaceholderAPI expansion: " + e.getMessage());
            }
        }

        // Reset states
        placeholderAPIHooked = false;
        worldGuardHooked = false;
        essentialsHooked = false;
        placeholderExpansion = null;

        // Re-initialize
        initializeHooks();
    }

    /**
     * Check if a specific plugin is available and enabled
     */
    public boolean isPluginAvailable(String pluginName) {
        try {
            return Bukkit.getPluginManager().getPlugin(pluginName) != null &&
                    Bukkit.getPluginManager().isPluginEnabled(pluginName);
        } catch (Exception e) {
            return false;
        }
    }

    // Getters
    public boolean isPlaceholderAPIHooked() {
        return placeholderAPIHooked;
    }

    public boolean isWorldGuardHooked() {
        return worldGuardHooked;
    }

    public boolean isEssentialsHooked() {
        return essentialsHooked;
    }

    public Vote2SleepPlaceholders getPlaceholderExpansion() {
        return placeholderExpansion;
    }

    /**
     * Cleanup method for plugin shutdown
     */
    public void cleanup() {
        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.unregister();
                plugin.getLogger().info("PlaceholderAPI expansion unregistered");
            } catch (Exception e) {
                plugin.getLogger().warning("Error unregistering PlaceholderAPI expansion: " + e.getMessage());
            }
        }
    }
}