package com.github.k1ritoz.vote2Sleep.managers;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

public class MessageManager {

    private final Vote2Sleep plugin;
    private FileConfiguration messages;
    private File messagesFile;
    private String prefix;
    private String currentLanguage;

    public MessageManager(Vote2Sleep plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        // Get language from config
        this.currentLanguage = plugin.getConfigManager().getLanguage();

        // Ensure messages file exists
        ensureMessagesFile();

        // Load messages
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
        setDefaults();
        this.prefix = getMessage("prefix");

        plugin.getLogger().info("Loaded messages for language: " + currentLanguage);
    }

    private void ensureMessagesFile() {
        String fileName = "messages_" + currentLanguage + ".yml";
        this.messagesFile = new File(plugin.getDataFolder(), fileName);

        if (!messagesFile.exists()) {
            // Try to copy from resources
            try {
                InputStream resourceStream = plugin.getResource(fileName);
                if (resourceStream != null) {
                    Files.copy(resourceStream, messagesFile.toPath());
                    plugin.getLogger().info("Created " + fileName + " from template");
                } else {
                    // Fallback to English if language file doesn't exist
                    plugin.getLogger().warning("Language file " + fileName + " not found, falling back to English");
                    this.currentLanguage = "en";
                    fileName = "messages_en.yml";
                    this.messagesFile = new File(plugin.getDataFolder(), fileName);

                    if (!messagesFile.exists()) {
                        resourceStream = plugin.getResource(fileName);
                        if (resourceStream != null) {
                            Files.copy(resourceStream, messagesFile.toPath());
                        } else {
                            // Create default file if English also doesn't exist
                            createDefaultMessagesFile();
                        }
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create messages file: " + e.getMessage());
                createDefaultMessagesFile();
            }
        }
    }

    private void createDefaultMessagesFile() {
        try {
            messagesFile.createNewFile();
            this.messages = YamlConfiguration.loadConfiguration(messagesFile);
            setDefaultMessages();
            messages.save(messagesFile);
            plugin.getLogger().info("Created default messages file");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create default messages file: " + e.getMessage());
        }
    }

    private void setDefaults() {
        if (messages.getKeys(false).isEmpty()) {
            setDefaultMessages();
            saveMessages();
        }
    }

    private void setDefaultMessages() {
        // Default messages (English)
        messages.addDefault("prefix", "&8[&bVote2Sleep&8] &r");
        messages.addDefault("world-not-enabled", "&cVote2Sleep is not enabled in this world!");
        messages.addDefault("player-exempt", "&cYou are exempt from sleep voting!");
        messages.addDefault("cannot-sleep-now", "&cYou can only vote for sleep during night or storms!");
        messages.addDefault("already-voted", "&cYou have already voted for sleep!");
        messages.addDefault("vote-removed", "&aYour sleep vote has been removed!");
        messages.addDefault("vote-cast", "&a{current}&7/&a{required} &7players voted for sleep! &8(&c{remaining} &7remaining)");
        messages.addDefault("skip-countdown", "&eNight will be skipped in &a{seconds} &eseconds...");
        messages.addDefault("vote-timeout", "&cSleep vote has timed out!");
        messages.addDefault("night-skipped", "&aThe night has been skipped! Sweet dreams! &f✨");
        messages.addDefault("boss-bar-title", "Sleep Vote: {current}/{required}");
        messages.addDefault("world-enabled", "&aVote2Sleep enabled for world &e{world}&a!");
        messages.addDefault("world-disabled", "&cVote2Sleep disabled for world &e{world}&c!");
        messages.addDefault("already-enabled", "&cVote2Sleep is already enabled in this world!");
        messages.addDefault("already-disabled", "&cVote2Sleep is already disabled in this world!");
        messages.addDefault("no-permission", "&cYou don't have permission to use this command!");
        messages.addDefault("reload-success", "&aVote2Sleep configuration reloaded successfully!");
        messages.addDefault("statistics-cleared", "&aStatistics have been cleared!");
        messages.addDefault("votes-cancelled-by-admin", "&cSleep votes have been cancelled by {admin}!");
        messages.addDefault("force-skip-by-admin", "&eNight has been force-skipped by {admin}!");
        messages.addDefault("weather-cleared-votes-cancelled", "&cSleep votes cancelled because the weather cleared!");
        messages.addDefault("world-enabled-notice", "&aVote2Sleep is enabled in this world! Use /sleep to vote.");
        messages.addDefault("world-disabled-notice", "&cVote2Sleep is disabled in this world.");

        // Status messages
        messages.addDefault("status.header", "&b&l=== Vote2Sleep Status ===");
        messages.addDefault("status.world", "&7World: &e{world}");
        messages.addDefault("status.current-votes", "&7Current Votes: &a{current}&7/&a{required}");
        messages.addDefault("status.you-voted", "&7You Voted: {status}");
        messages.addDefault("status.can-sleep", "&7Can Sleep: {status}");
        messages.addDefault("status.progress", "&7Progress: &a{progress}%");
        messages.addDefault("status.time-remaining", "&7Time Remaining: &e{time} seconds");
        messages.addDefault("status-yes", "&aYes");
        messages.addDefault("status-no", "&cNo");

        // Stats messages
        messages.addDefault("stats.header", "&b&l=== Vote2Sleep Statistics ===");
        messages.addDefault("stats.plugin-version", "&7Plugin Version: &e{version}");
        messages.addDefault("stats.platform", "&7Platform: &e{platform}");
        messages.addDefault("stats.enabled-worlds", "&7Enabled Worlds: &a{count}");
        messages.addDefault("stats.server-tps", "&7Server TPS: &a{tps}");
        messages.addDefault("stats.database-status", "&7Database: {status}");
        messages.addDefault("stats.total-skips", "&7Total Night Skips: &e{skips}");

        // Command help
        messages.addDefault("help.header", "&b&l=== Vote2Sleep Help ===");
        messages.addDefault("help.vote", "&e/sleep &7- Vote to skip night or storm");
        messages.addDefault("help.status", "&e/sleep status &7- Check current vote status");
        messages.addDefault("help.enable", "&e/sleep enable &7- Enable plugin in current world");
        messages.addDefault("help.disable", "&e/sleep disable &7- Disable plugin in current world");
        messages.addDefault("help.reload", "&e/sleep reload &7- Reload configuration");
        messages.addDefault("help.stats", "&e/sleep stats &7- View plugin statistics");
        messages.addDefault("help.cancel", "&e/sleep cancel &7- Cancel current votes");
        messages.addDefault("help.force", "&e/sleep force &7- Force skip night/storm");
        messages.addDefault("help.help", "&e/sleep help &7- Show this help message");

        // Action bar messages
        messages.addDefault("vote-cast-actionbar", "&a{player} &7voted for sleep! &8(&a{current}&7/&a{required}&8)");

        // Title messages
        messages.addDefault("skip-title", "&a&lNight Skipped!");
        messages.addDefault("skip-subtitle", "&eSleep tight! &f✨");

        messages.options().copyDefaults(true);
    }

    public String getMessage(String key) {
        String message = messages.getString(key, key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);

        // Replace custom placeholders first
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return message;
    }

    public String processPlaceholders(Player player, String message) {
        // Process PlaceholderAPI placeholders if available
        if (plugin.getHooksManager().isPlaceholderAPIHooked()) {
            try {
                // Use reflection to avoid hard dependency
                Class<?> placeholderAPIClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                java.lang.reflect.Method setPlaceholdersMethod = placeholderAPIClass.getMethod("setPlaceholders", Player.class, String.class);
                message = (String) setPlaceholdersMethod.invoke(null, player, message);
            } catch (Exception e) {
                // Only log if debug mode is enabled to avoid spam
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("Error processing PlaceholderAPI placeholders: " + e.getMessage());
                }
            }
        }
        return message;
    }

    public void sendMessage(Player player, String key) {
        String message = getMessage(key);
        if (!message.isEmpty() && !message.equals(key)) {
            message = processPlaceholders(player, message);
            player.sendMessage(prefix + message);
        }
    }

    public void sendMessage(Player player, String key, Map<String, String> placeholders) {
        String message = getMessage(key, placeholders);
        if (!message.isEmpty() && !message.equals(key)) {
            message = processPlaceholders(player, message);
            player.sendMessage(prefix + message);
        }
    }

    public void sendWorldMessage(World world, String key) {
        String message = getMessage(key);
        if (!message.isEmpty() && !message.equals(key)) {
            for (Player player : world.getPlayers()) {
                String processedMessage = processPlaceholders(player, message);
                player.sendMessage(prefix + processedMessage);
            }
        }
    }

    public void sendWorldMessage(World world, String key, Map<String, String> placeholders) {
        String message = getMessage(key, placeholders);
        if (!message.isEmpty() && !message.equals(key)) {
            for (Player player : world.getPlayers()) {
                String processedMessage = processPlaceholders(player, message);
                player.sendMessage(prefix + processedMessage);
            }
        }
    }

    public String getBossBarTitle(int current, int required) {
        String title = getMessage("boss-bar-title", Map.of(
                "current", String.valueOf(current),
                "required", String.valueOf(required)
        ));

        // For boss bars, we can't easily process per-player placeholders, so use a generic approach
        if (plugin.getHooksManager().isPlaceholderAPIHooked()) {
            try {
                // Use reflection to avoid hard dependency and use null player for global placeholders only
                Class<?> placeholderAPIClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                java.lang.reflect.Method setPlaceholdersMethod = placeholderAPIClass.getMethod("setPlaceholders", Player.class, String.class);
                title = (String) setPlaceholdersMethod.invoke(null, null, title);
            } catch (Exception e) {
                // Silently ignore errors for boss bar placeholders
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("Error processing boss bar placeholders: " + e.getMessage());
                }
            }
        }

        return title;
    }

    public void sendActionBar(Player player, String key) {
        sendActionBar(player, key, Map.of());
    }

    public void sendActionBar(Player player, String key, Map<String, String> placeholders) {
        if (!plugin.getConfigManager().isActionBarEnabled()) return;

        String message = getMessage(key, placeholders);
        if (!message.isEmpty() && !message.equals(key)) {
            // Process PlaceholderAPI placeholders first
            message = processPlaceholders(player, message);

            try {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(message));
            } catch (Exception e) {
                // Fallback to chat message if action bar is not supported
                player.sendMessage(message);
            }
        }
    }

    private void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages file: " + e.getMessage());
        }
    }

    public void reload() {
        loadMessages();
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Change language at runtime
     */
    public boolean changeLanguage(String newLanguage) {
        String oldLanguage = this.currentLanguage;
        this.currentLanguage = newLanguage;

        try {
            // Force reload of messages file
            String fileName = "messages_" + newLanguage + ".yml";
            File newMessagesFile = new File(plugin.getDataFolder(), fileName);

            // Check if the new language file exists
            if (!newMessagesFile.exists()) {
                // Try to copy from resources
                try {
                    InputStream resourceStream = plugin.getResource(fileName);
                    if (resourceStream != null) {
                        Files.copy(resourceStream, newMessagesFile.toPath());
                        plugin.getLogger().info("Created " + fileName + " from template");
                    } else {
                        plugin.getLogger().warning("Language file " + fileName + " not found in resources");
                        // Revert language
                        this.currentLanguage = oldLanguage;
                        return false;
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to create language file: " + e.getMessage());
                    this.currentLanguage = oldLanguage;
                    return false;
                }
            }

            // Update the messages file reference
            this.messagesFile = newMessagesFile;

            // Reload messages from the new file
            this.messages = YamlConfiguration.loadConfiguration(messagesFile);
            setDefaults();
            this.prefix = getMessage("prefix");

            plugin.getLogger().info("Changed language from " + oldLanguage + " to " + newLanguage);
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to change language to " + newLanguage + ", reverting to " + oldLanguage + ": " + e.getMessage());
            this.currentLanguage = oldLanguage;

            // Reload the old language
            try {
                loadMessages();
            } catch (Exception revertError) {
                plugin.getLogger().severe("Failed to revert to old language: " + revertError.getMessage());
            }
            return false;
        }
    }
}