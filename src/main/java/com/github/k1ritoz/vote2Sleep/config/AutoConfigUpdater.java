package com.github.k1ritoz.vote2Sleep.config;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class AutoConfigUpdater {

    private final Vote2Sleep plugin;

    public AutoConfigUpdater(Vote2Sleep plugin) {
        this.plugin = plugin;
    }

    /**
     * Automatically updates config by comparing with default config from resources
     */
    public boolean updateConfigIfNeeded() {
        try {
            File userConfigFile = new File(plugin.getDataFolder(), "config.yml");
            if (!userConfigFile.exists()) {
                return false; // Let normal config creation handle this
            }

            // Load user config and default config
            FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userConfigFile);
            FileConfiguration defaultConfig = getDefaultConfig();

            if (defaultConfig == null) {
                plugin.getLogger().warning("Could not load default config for comparison");
                return false;
            }

            // Get current versions
            int userConfigVersion = userConfig.getInt("config-version", 1);
            int defaultConfigVersion = defaultConfig.getInt("config-version", 1);

            // Check if update is needed based on version
            if (userConfigVersion >= defaultConfigVersion) {
                return false; // No update needed
            }

            // Also check for missing keys as backup
            Set<String> missingKeys = findMissingKeys(userConfig, defaultConfig);

            if (missingKeys.isEmpty() && userConfigVersion >= defaultConfigVersion) {
                // Update version number if keys are present but version is old
                userConfig.set("config-version", defaultConfigVersion);
                userConfig.save(userConfigFile);
                return false;
            }

            plugin.getLogger().info("Updating configuration from version " + userConfigVersion + " to " + defaultConfigVersion);
            plugin.getLogger().info("Found " + missingKeys.size() + " new configuration options");

            // Backup current config
            backupConfig(userConfigFile);

            // Update config while preserving structure and comments
            updateConfigWithComments(userConfigFile, defaultConfig, missingKeys);

            // Update the version in the user config
            FileConfiguration updatedConfig = YamlConfiguration.loadConfiguration(userConfigFile);
            updatedConfig.set("config-version", defaultConfigVersion);
            updatedConfig.save(userConfigFile);

            plugin.getLogger().info("Configuration updated successfully! Added: " + missingKeys);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Error updating configuration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates messages by comparing with default messages from resources
     */
    public boolean updateMessagesIfNeeded(String language) {
        try {
            String fileName = "messages_" + language + ".yml";
            File userMessagesFile = new File(plugin.getDataFolder(), fileName);

            if (!userMessagesFile.exists()) {
                return false; // Let normal message creation handle this
            }

            // Load user messages and default messages
            FileConfiguration userMessages = YamlConfiguration.loadConfiguration(userMessagesFile);
            FileConfiguration defaultMessages = getDefaultMessages(language);

            if (defaultMessages == null) {
                plugin.getLogger().warning("Could not load default messages for " + language);
                return false;
            }

            // Get current versions
            int userMessageVersion = userMessages.getInt("message-version", 1);
            int defaultMessageVersion = defaultMessages.getInt("message-version", 1);

            // Check if update is needed based on version
            if (userMessageVersion >= defaultMessageVersion) {
                return false; // No update needed
            }

            // Also check for missing keys as backup
            Set<String> missingKeys = findMissingKeys(userMessages, defaultMessages);

            if (missingKeys.isEmpty() && userMessageVersion >= defaultMessageVersion) {
                // Update version number if keys are present but version is old
                userMessages.set("message-version", defaultMessageVersion);
                userMessages.save(userMessagesFile);
                return false;
            }

            plugin.getLogger().info("Updating " + language + " messages from version " + userMessageVersion + " to " + defaultMessageVersion);
            plugin.getLogger().info("Found " + missingKeys.size() + " new message keys");

            // Backup current messages
            backupMessages(userMessagesFile, language);

            // Update messages while preserving structure and comments
            updateMessagesWithComments(userMessagesFile, defaultMessages, missingKeys);

            // Update the version in the user messages
            FileConfiguration updatedMessages = YamlConfiguration.loadConfiguration(userMessagesFile);
            updatedMessages.set("message-version", defaultMessageVersion);
            updatedMessages.save(userMessagesFile);

            plugin.getLogger().info("Messages updated successfully! Added: " + missingKeys);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Error updating messages for " + language + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private FileConfiguration getDefaultConfig() {
        try (InputStream stream = plugin.getResource("config.yml")) {
            if (stream == null) return null;

            // Create temporary file to load the default config
            File tempFile = File.createTempFile("vote2sleep-default-config", ".yml");
            Files.copy(stream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            FileConfiguration config = YamlConfiguration.loadConfiguration(tempFile);
            tempFile.delete();

            return config;
        } catch (IOException e) {
            return null;
        }
    }

    private FileConfiguration getDefaultMessages(String language) {
        try (InputStream stream = plugin.getResource("messages_" + language + ".yml")) {
            if (stream == null) return null;

            // Create temporary file to load the default messages
            File tempFile = File.createTempFile("vote2sleep-default-messages", ".yml");
            Files.copy(stream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            FileConfiguration messages = YamlConfiguration.loadConfiguration(tempFile);
            tempFile.delete();

            return messages;
        } catch (IOException e) {
            return null;
        }
    }

    private Set<String> findMissingKeys(FileConfiguration userConfig, FileConfiguration defaultConfig) {
        Set<String> missingKeys = new HashSet<>();
        Set<String> defaultKeys = getAllKeys(defaultConfig);
        Set<String> userKeys = getAllKeys(userConfig);

        // Find keys that exist in default but not in user config
        for (String key : defaultKeys) {
            if (!userKeys.contains(key)) {
                // Skip version keys as they're automatically managed
                if (!key.equals("config-version") && !key.equals("message-version")) {
                    missingKeys.add(key);
                }
            }
        }

        return missingKeys;
    }

    private Set<String> getAllKeys(FileConfiguration config) {
        Set<String> keys = new HashSet<>();
        getAllKeysRecursive(config.getRoot(), "", keys);
        return keys;
    }

    private void getAllKeysRecursive(org.bukkit.configuration.ConfigurationSection section, String prefix, Set<String> keys) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (section.isConfigurationSection(key)) {
                getAllKeysRecursive(section.getConfigurationSection(key), fullKey, keys);
            } else {
                keys.add(fullKey);
            }
        }
    }

    private void backupConfig(File configFile) throws IOException {
        File backupFile = new File(plugin.getDataFolder(), "config.yml.backup." + System.currentTimeMillis());
        Files.copy(configFile.toPath(), backupFile.toPath());
        plugin.getLogger().info("Configuration backed up to: " + backupFile.getName());
    }

    private void backupMessages(File messagesFile, String language) throws IOException {
        File backupFile = new File(plugin.getDataFolder(), "messages_" + language + ".yml.backup." + System.currentTimeMillis());
        Files.copy(messagesFile.toPath(), backupFile.toPath());
        plugin.getLogger().info("Messages backed up to: " + backupFile.getName());
    }

    /**
     * Updates config file while preserving comments and structure
     */
    private void updateConfigWithComments(File userConfigFile, FileConfiguration defaultConfig, Set<String> missingKeys) throws IOException {
        // Simpler and more reliable approach:
        // 1. Load user values
        // 2. Copy default config structure
        // 3. Override with user values
        // 4. Write clean, organized result

        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userConfigFile);

        // Create a temporary file with the default structure
        File tempFile = File.createTempFile("vote2sleep-config-update", ".yml");

        try {
            // Copy default config to temp file
            try (InputStream defaultStream = plugin.getResource("config.yml")) {
                Files.copy(defaultStream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // Load the clean default structure
            FileConfiguration cleanConfig = YamlConfiguration.loadConfiguration(tempFile);

            // Override default values with user's custom values
            transferUserValues(userConfig, cleanConfig);

            // Save the clean, organized config
            cleanConfig.save(userConfigFile);

        } finally {
            // Clean up temp file
            tempFile.delete();
        }
    }

    /**
     * Transfer user's custom values to the clean config structure
     */
    private void transferUserValues(FileConfiguration userConfig, FileConfiguration cleanConfig) {
        // Get all keys from user config (excluding version)
        Set<String> userKeys = getAllKeys(userConfig);

        for (String key : userKeys) {
            if (!key.equals("config-version") && !key.equals("message-version")) {
                // If the key exists in clean config, transfer the user's value
                if (cleanConfig.contains(key)) {
                    Object userValue = userConfig.get(key);
                    cleanConfig.set(key, userValue);

                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("Preserved user setting: " + key + " = " + userValue);
                    }
                }
            }
        }
    }

    /**
     * Updates messages file while preserving comments and structure
     */
    private void updateMessagesWithComments(File userMessagesFile, FileConfiguration defaultMessages, Set<String> missingKeys) throws IOException {
        // Same clean approach for messages
        FileConfiguration userMessages = YamlConfiguration.loadConfiguration(userMessagesFile);

        String language = userMessagesFile.getName().replace("messages_", "").replace(".yml", "");

        // Create a temporary file with the default structure
        File tempFile = File.createTempFile("vote2sleep-messages-update", ".yml");

        try {
            // Copy default messages to temp file
            try (InputStream defaultStream = plugin.getResource("messages_" + language + ".yml")) {
                Files.copy(defaultStream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // Load the clean default structure
            FileConfiguration cleanMessages = YamlConfiguration.loadConfiguration(tempFile);

            // Override default values with user's custom values
            transferUserValues(userMessages, cleanMessages);

            // Save the clean, organized messages
            cleanMessages.save(userMessagesFile);

        } finally {
            // Clean up temp file
            tempFile.delete();
        }
    }
}