package com.github.k1ritoz.vote2Sleep.managers;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import com.github.k1ritoz.vote2Sleep.config.AutoConfigUpdater;
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
    private AutoConfigUpdater autoUpdater;

    public MessageManager(Vote2Sleep plugin) {
        this.plugin = plugin;
        this.autoUpdater = new AutoConfigUpdater(plugin);
    }

    public void loadMessages() {
        // Get language from config
        this.currentLanguage = plugin.getConfigManager().getLanguage();

        // Ensure messages file exists
        ensureMessagesFile();

        // Load messages
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
        setDefaults();

        // Automatically update messages if needed (preserves comments and structure)
        autoUpdater.updateMessagesIfNeeded(currentLanguage);

        // Reload after update
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

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
        // Default messages (English) - only set if they don't exist
        if (!messages.contains("prefix")) messages.set("prefix", "&8[&bVote2Sleep&8] &r");
        if (!messages.contains("world-not-enabled")) messages.set("world-not-enabled", "&cVote2Sleep is not enabled in this world!");
        if (!messages.contains("player-exempt")) messages.set("player-exempt", "&cYou are exempt from sleep voting!");
        if (!messages.contains("cannot-sleep-now")) messages.set("cannot-sleep-now", "&cYou can only vote for sleep during night or storms!");
        if (!messages.contains("already-voted")) messages.set("already-voted", "&cYou have already voted for sleep!");
        if (!messages.contains("vote-removed")) messages.set("vote-removed", "&aYour sleep vote has been removed!");
        if (!messages.contains("vote-cast")) messages.set("vote-cast", "&a{current}&7/&a{required} &7players voted for sleep! &8(&c{remaining} &7remaining)");
        if (!messages.contains("skip-countdown")) messages.set("skip-countdown", "&eNight will be skipped in &a{seconds} &eseconds...");
        if (!messages.contains("vote-timeout")) messages.set("vote-timeout", "&cSleep vote has timed out!");
        if (!messages.contains("night-skipped")) messages.set("night-skipped", "&aThe night has been skipped! Sweet dreams! &f✨");
        if (!messages.contains("storm-skipped")) messages.set("storm-skipped", "&aThe storm has been cleared! The sun is shining again! &e☀");
        if (!messages.contains("boss-bar-title")) messages.set("boss-bar-title", "Sleep Vote: {current}/{required}");
        if (!messages.contains("world-enabled")) messages.set("world-enabled", "&aVote2Sleep enabled for world &e{world}&a!");
        if (!messages.contains("world-disabled")) messages.set("world-disabled", "&cVote2Sleep disabled for world &e{world}&c!");
        if (!messages.contains("already-enabled")) messages.set("already-enabled", "&cVote2Sleep is already enabled in this world!");
        if (!messages.contains("already-disabled")) messages.set("already-disabled", "&cVote2Sleep is already disabled in this world!");
        if (!messages.contains("no-permission")) messages.set("no-permission", "&cYou don't have permission to use this command!");
        if (!messages.contains("reload-success")) messages.set("reload-success", "&aVote2Sleep configuration reloaded successfully!");
        if (!messages.contains("statistics-cleared")) messages.set("statistics-cleared", "&aStatistics have been cleared!");
        if (!messages.contains("votes-cancelled-by-admin")) messages.set("votes-cancelled-by-admin", "&cSleep votes have been cancelled by {admin}!");
        if (!messages.contains("force-skip-by-admin")) messages.set("force-skip-by-admin", "&eNight has been force-skipped by {admin}!");
        if (!messages.contains("weather-cleared-votes-cancelled")) messages.set("weather-cleared-votes-cancelled", "&cSleep votes cancelled because the weather cleared!");
        if (!messages.contains("world-enabled-notice")) messages.set("world-enabled-notice", "&aVote2Sleep is enabled in this world! Use /sleep to vote.");
        if (!messages.contains("world-disabled-notice")) messages.set("world-disabled-notice", "&cVote2Sleep is disabled in this world.");

        // Effects messages
        if (!messages.contains("lightning-disabled")) messages.set("lightning-disabled", "&eLightning effects have been disabled for achievement reasons.");
        if (!messages.contains("effects-disabled")) messages.set("effects-disabled", "&eVisual effects have been disabled in configuration.");

        // Status messages
        if (!messages.contains("status.header")) messages.set("status.header", "&b&l=== Vote2Sleep Status ===");
        if (!messages.contains("status.world")) messages.set("status.world", "&7World: &e{world}");
        if (!messages.contains("status.current-votes")) messages.set("status.current-votes", "&7Current Votes: &a{current}&7/&a{required}");
        if (!messages.contains("status.you-voted")) messages.set("status.you-voted", "&7You Voted: {status}");
        if (!messages.contains("status.can-sleep")) messages.set("status.can-sleep", "&7Can Sleep: {status}");
        if (!messages.contains("status.progress")) messages.set("status.progress", "&7Progress: &a{progress}%");
        if (!messages.contains("status.time-remaining")) messages.set("status.time-remaining", "&7Time Remaining: &e{time} seconds");
        if (!messages.contains("status-yes")) messages.set("status-yes", "&aYes");
        if (!messages.contains("status-no")) messages.set("status-no", "&cNo");

        // Stats messages
        if (!messages.contains("stats.header")) messages.set("stats.header", "&b&l=== Vote2Sleep Statistics ===");
        if (!messages.contains("stats.plugin-version")) messages.set("stats.plugin-version", "&7Plugin Version: &e{version}");
        if (!messages.contains("stats.platform")) messages.set("stats.platform", "&7Platform: &e{platform}");
        if (!messages.contains("stats.enabled-worlds")) messages.set("stats.enabled-worlds", "&7Enabled Worlds: &a{count}");
        if (!messages.contains("stats.server-tps")) messages.set("stats.server-tps", "&7Server TPS: &a{tps}");
        if (!messages.contains("stats.database-status")) messages.set("stats.database-status", "&7Database: {status}");
        if (!messages.contains("stats.total-skips")) messages.set("stats.total-skips", "&7Total Night Skips: &e{skips}");

        // Command help
        if (!messages.contains("help.header")) messages.set("help.header", "&b&l=== Vote2Sleep Help ===");
        if (!messages.contains("help.vote")) messages.set("help.vote", "&e/sleep &7- Vote to skip night or storm");
        if (!messages.contains("help.status")) messages.set("help.status", "&e/sleep status &7- Check current vote status");
        if (!messages.contains("help.enable")) messages.set("help.enable", "&e/sleep enable &7- Enable plugin in current world");
        if (!messages.contains("help.disable")) messages.set("help.disable", "&e/sleep disable &7- Disable plugin in current world");
        if (!messages.contains("help.reload")) messages.set("help.reload", "&e/sleep reload &7- Reload configuration");
        if (!messages.contains("help.stats")) messages.set("help.stats", "&e/sleep stats &7- View plugin statistics");
        if (!messages.contains("help.cancel")) messages.set("help.cancel", "&e/sleep cancel &7- Cancel current votes");
        if (!messages.contains("help.force")) messages.set("help.force", "&e/sleep force &7- Force skip night/storm");
        if (!messages.contains("help.help")) messages.set("help.help", "&e/sleep help &7- Show this help message");

        // Action bar messages
        if (!messages.contains("vote-cast-actionbar")) messages.set("vote-cast-actionbar", "&a{player} &7voted for sleep! &8(&a{current}&7/&a{required}&8)");

        // Title messages
        if (!messages.contains("skip-title")) messages.set("skip-title", "&a&lNight Skipped!");
        if (!messages.contains("skip-subtitle")) messages.set("skip-subtitle", "&eSleep tight! &f✨");
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

            // Auto-update messages for the new language
            autoUpdater.updateMessagesIfNeeded(newLanguage);
            this.messages = YamlConfiguration.loadConfiguration(messagesFile);

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