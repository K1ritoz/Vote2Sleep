package com.github.k1ritoz.vote2Sleep.config;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

public class AutoConfigUpdater {

    private static final int MAX_BACKUPS_PER_FILE = 5;
    private static final String BACKUP_FOLDER_NAME = "backups";

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

            String userConfigVersion = getVersion(userConfig, "config-version");
            String defaultConfigVersion = getVersion(defaultConfig, "config-version");
            Set<String> missingKeys = findMissingKeys(userConfig, defaultConfig);
            int versionComparison = compareVersions(userConfigVersion, defaultConfigVersion);
            boolean versionBehind = versionComparison < 0;

            if (versionComparison > 0) {
                plugin.getLogger().warning("User config version " + userConfigVersion + " is newer than bundled version " + defaultConfigVersion + ". Skipping auto-update to avoid downgrading the file.");
                return false;
            }

            if (!versionBehind && missingKeys.isEmpty()) {
                return false;
            }

            if (missingKeys.isEmpty()) {
                userConfig.set("config-version", defaultConfigVersion);
                userConfig.save(userConfigFile);
                plugin.getLogger().info("Configuration version updated from " + userConfigVersion + " to " + defaultConfigVersion);
                return true;
            }

            plugin.getLogger().info("Updating configuration from version " + userConfigVersion + " to " + defaultConfigVersion);
            plugin.getLogger().info("Found " + missingKeys.size() + " new configuration options");

            // Backup current config
            backupConfig(userConfigFile);

            // Update config while preserving structure and comments
            updateConfigWithComments(userConfigFile);

            // Update the version in the user config
            FileConfiguration updatedConfig = YamlConfiguration.loadConfiguration(userConfigFile);
            updatedConfig.set("config-version", defaultConfigVersion);
            updatedConfig.save(userConfigFile);

            plugin.getLogger().info("Configuration updated successfully! Added: " + missingKeys);
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating configuration", e);
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

            String userMessageVersion = getVersion(userMessages, "message-version");
            String defaultMessageVersion = getVersion(defaultMessages, "message-version");
            Set<String> missingKeys = findMissingKeys(userMessages, defaultMessages);
            int versionComparison = compareVersions(userMessageVersion, defaultMessageVersion);
            boolean versionBehind = versionComparison < 0;

            if (versionComparison > 0) {
                plugin.getLogger().warning("User message version " + userMessageVersion + " for " + language + " is newer than bundled version " + defaultMessageVersion + ". Skipping auto-update to avoid downgrading the file.");
                return false;
            }

            if (!versionBehind && missingKeys.isEmpty()) {
                return false;
            }

            if (missingKeys.isEmpty()) {
                userMessages.set("message-version", defaultMessageVersion);
                userMessages.save(userMessagesFile);
                plugin.getLogger().info("Message version for " + language + " updated from " + userMessageVersion + " to " + defaultMessageVersion);
                return true;
            }

            plugin.getLogger().info("Updating " + language + " messages from version " + userMessageVersion + " to " + defaultMessageVersion);
            plugin.getLogger().info("Found " + missingKeys.size() + " new message keys");

            // Backup current messages
            backupMessages(userMessagesFile, language);

            // Update messages while preserving structure and comments
            updateMessagesWithComments(userMessagesFile);

            // Update the version in the user messages
            FileConfiguration updatedMessages = YamlConfiguration.loadConfiguration(userMessagesFile);
            updatedMessages.set("message-version", defaultMessageVersion);
            updatedMessages.save(userMessagesFile);

            plugin.getLogger().info("Messages updated successfully! Added: " + missingKeys);
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating messages for " + language, e);
            return false;
        }
    }

    private FileConfiguration getDefaultConfig() {
        try (InputStream stream = plugin.getResource("config.yml")) {
            if (stream == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return null;
        }
    }

    private FileConfiguration getDefaultMessages(String language) {
        try (InputStream stream = plugin.getResource("messages_" + language + ".yml")) {
            if (stream == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return null;
        }
    }

    private String getVersion(FileConfiguration config, String key) {
        Object version = config.get(key);
        return version == null ? "1.0" : String.valueOf(version);
    }

    private int compareVersions(String currentVersion, String defaultVersion) {
        int[] currentParts = parseVersion(currentVersion);
        int[] defaultParts = parseVersion(defaultVersion);
        int maxLength = Math.max(currentParts.length, defaultParts.length);

        for (int i = 0; i < maxLength; i++) {
            int current = i < currentParts.length ? currentParts[i] : 0;
            int latest = i < defaultParts.length ? defaultParts[i] : 0;

            if (current != latest) {
                return Integer.compare(current, latest);
            }
        }

        return 0;
    }

    private int[] parseVersion(String version) {
        String[] parts = version.split("\\.");
        int[] parsed = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String numericPart = parts[i].replaceAll("[^0-9]", "");
            parsed[i] = numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        }

        return parsed;
    }

    private Set<String> findMissingKeys(FileConfiguration userConfig, FileConfiguration defaultConfig) {
        Set<String> missingKeys = new TreeSet<>();
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
        ConfigurationSection root = config.getRoot();
        if (root != null) {
            getAllKeysRecursive(root, "", keys);
        }
        return keys;
    }

    private void getAllKeysRecursive(ConfigurationSection section, String prefix, Set<String> keys) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (section.isConfigurationSection(key)) {
                ConfigurationSection childSection = section.getConfigurationSection(key);
                if (childSection != null) {
                    getAllKeysRecursive(childSection, fullKey, keys);
                }
            } else {
                keys.add(fullKey);
            }
        }
    }

    private void backupConfig(File configFile) throws IOException {
        File backupFile = backupFile(configFile, "config.yml");
        plugin.getLogger().info("Configuration backed up to: " + backupFile.getName());
    }

    private void backupMessages(File messagesFile, String language) throws IOException {
        File backupFile = backupFile(messagesFile, "messages_" + language + ".yml");
        plugin.getLogger().info("Messages backed up to: " + backupFile.getName());
    }

    private File backupFile(File sourceFile, String fileName) throws IOException {
        File backupDirectory = new File(plugin.getDataFolder(), BACKUP_FOLDER_NAME);
        Files.createDirectories(backupDirectory.toPath());

        File backupFile = new File(backupDirectory, fileName + ".backup." + System.currentTimeMillis());
        Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        cleanupOldBackups(fileName);
        return backupFile;
    }

    private void cleanupOldBackups(String fileName) {
        List<File> backupFiles = findBackupFiles(fileName);
        backupFiles.sort(Comparator.comparingLong(File::lastModified).reversed());

        for (int i = MAX_BACKUPS_PER_FILE; i < backupFiles.size(); i++) {
            File oldBackup = backupFiles.get(i);
            try {
                Files.deleteIfExists(oldBackup.toPath());
                if (isDebugMode()) {
                    plugin.getLogger().info("Deleted old backup: " + oldBackup.getName());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not delete old backup " + oldBackup.getName() + ": " + e.getMessage());
            }
        }
    }

    private List<File> findBackupFiles(String fileName) {
        List<File> backupFiles = new ArrayList<>();
        String backupPrefix = fileName + ".backup.";

        collectBackupFiles(plugin.getDataFolder(), backupPrefix, backupFiles);
        collectBackupFiles(new File(plugin.getDataFolder(), BACKUP_FOLDER_NAME), backupPrefix, backupFiles);

        return backupFiles;
    }

    private void collectBackupFiles(File directory, String backupPrefix, List<File> backupFiles) {
        File[] files = directory.listFiles((dir, name) -> name.startsWith(backupPrefix));
        if (files != null) {
            backupFiles.addAll(Arrays.asList(files));
        }
    }

    private boolean isDebugMode() {
        return plugin.getConfigManager() != null && plugin.getConfigManager().isDebugMode();
    }

    /**
     * Updates config file while preserving comments and structure
     */
    private void updateConfigWithComments(File userConfigFile) throws IOException {
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
                if (defaultStream == null) {
                    throw new IOException("Default config resource not found");
                }
                Files.copy(defaultStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Load the clean default structure
            FileConfiguration cleanConfig = YamlConfiguration.loadConfiguration(tempFile);

            // Override default values with user's custom values
            transferUserValues(userConfig, cleanConfig);

            // Save the clean, organized config
            cleanConfig.save(userConfigFile);

        } finally {
            // Clean up temp file
            deleteTempFile(tempFile);
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

                    if (isDebugMode()) {
                        plugin.getLogger().info("Preserved user setting: " + key + " = " + userValue);
                    }
                }
            }
        }
    }

    /**
     * Updates messages file while preserving comments and structure
     */
    private void updateMessagesWithComments(File userMessagesFile) throws IOException {
        // Same clean approach for messages
        FileConfiguration userMessages = YamlConfiguration.loadConfiguration(userMessagesFile);

        String language = userMessagesFile.getName().replace("messages_", "").replace(".yml", "");

        // Create a temporary file with the default structure
        File tempFile = File.createTempFile("vote2sleep-messages-update", ".yml");

        try {
            // Copy default messages to temp file
            try (InputStream defaultStream = plugin.getResource("messages_" + language + ".yml")) {
                if (defaultStream == null) {
                    throw new IOException("Default messages resource not found: messages_" + language + ".yml");
                }
                Files.copy(defaultStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Load the clean default structure
            FileConfiguration cleanMessages = YamlConfiguration.loadConfiguration(tempFile);

            // Override default values with user's custom values
            transferUserValues(userMessages, cleanMessages);

            // Save the clean, organized messages
            cleanMessages.save(userMessagesFile);

        } finally {
            // Clean up temp file
            deleteTempFile(tempFile);
        }
    }

    private void deleteTempFile(File tempFile) {
        try {
            Files.deleteIfExists(tempFile.toPath());
        } catch (IOException e) {
            if (isDebugMode()) {
                plugin.getLogger().warning("Could not delete temporary file " + tempFile.getName() + ": " + e.getMessage());
            }
        }
    }
}