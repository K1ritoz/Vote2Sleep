package com.github.k1ritoz.vote2Sleep.config;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ConfigurationManager {

    private final Vote2Sleep plugin;
    private FileConfiguration config;
    private FileConfiguration worldConfig;
    private File worldsFile;

    public ConfigurationManager(Vote2Sleep plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        loadConfigurations();
    }

    public void loadConfigurations() {
        // Main config
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Worlds config
        this.worldsFile = new File(plugin.getDataFolder(), "worlds.yml");
        if (!worldsFile.exists()) {
            try {
                worldsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create worlds.yml file: " + e.getMessage());
            }
        }
        this.worldConfig = YamlConfiguration.loadConfiguration(worldsFile);

        // Set default if not exist
        setDefaults();
    }

    private void setDefaults() {
        // Main config defaults
        config.addDefault("settings.language", "en");
        config.addDefault("settings.vote-percentage", 0.5);
        config.addDefault("settings.minimum-players", 1);
        config.addDefault("settings.maximum-players", -1);
        config.addDefault("settings.skip-delay-seconds", 3);
        config.addDefault("settings.vote-timeout-seconds", 60);
        config.addDefault("settings.allow-storm-skip", true);
        config.addDefault("settings.allow-night-skip", true);
        config.addDefault("settings.reset-statistics", true);
        config.addDefault("settings.heal-players", false);
        config.addDefault("settings.feed-players", false);
        config.addDefault("settings.clear-weather", true);

        // Display settings
        config.addDefault("display.boss-bar.enabled", true);
        config.addDefault("display.boss-bar.color", "BLUE");
        config.addDefault("display.boss-bar.style", "SOLID");
        config.addDefault("display.titles.enabled", true);
        config.addDefault("display.titles.fade-in", 10);
        config.addDefault("display.titles.stay", 70);
        config.addDefault("display.titles.fade-out", 20);
        config.addDefault("display.sounds.enabled", true);
        config.addDefault("display.sounds.vote-sound", "BLOCK_NOTE_BLOCK_CHIME");
        config.addDefault("display.sounds.skip-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        config.addDefault("display.actionbar.enabled", true);

        // Advanced settings
        config.addDefault("advanced.database.enabled", false);
        config.addDefault("advanced.database.type", "SQLITE");
        config.addDefault("advanced.update-checker", true);
        config.addDefault("advanced.debug-mode", false);
        config.addDefault("advanced.metrics", true);
        config.addDefault("advanced.exempt-gamemodes", Arrays.asList("SPECTATOR", "CREATIVE"));
        config.addDefault("advanced.exempt-permissions", Arrays.asList("vote2sleep.exempt"));

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    // World Management
    public boolean isWorldEnabled(World world) {
        return worldConfig.getBoolean("worlds." + world.getName() + ".enabled", false);
    }

    public void enableWorld(World world) {
        worldConfig.set("worlds." + world.getName() + ".enabled", true);
        saveWorldsConfig();
    }

    public void disableWorld(World world) {
        worldConfig.set("worlds." + world.getName() + ".enabled", false);
        saveWorldsConfig();
    }

    public Set<String> getEnabledWorlds() {
        ConfigurationSection worldsSection = worldConfig.getConfigurationSection("worlds");
        if (worldsSection == null) return Set.of();

        return worldsSection.getKeys(false);
    }

    // World-specific settings
    public double getVotePercentage(World world) {
        return worldConfig.getDouble("worlds." + world.getName() + ".vote-percentage",
                config.getDouble("settings.vote-percentage"));
    }

    public void setVotePercentage(World world, double percentage) {
        worldConfig.set("worlds." + world.getName() + ".vote-percentage", percentage);
        saveWorldsConfig();
    }

    // General getters
    public String getLanguage() { return config.getString("settings.language"); }
    public double getVotePercentage() { return config.getDouble("settings.vote-percentage"); }
    public int getMinimumPlayers() { return config.getInt("settings.minimum-players"); }
    public int getMaximumPlayers() { return config.getInt("settings.maximum-players"); }
    public int getSkipDelaySeconds() { return config.getInt("settings.skip-delay-seconds"); }
    public int getVoteTimeoutSeconds() { return config.getInt("settings.vote-timeout-seconds"); }
    public boolean isStormSkipAllowed() { return config.getBoolean("settings.allow-storm-skip"); }
    public boolean isNightSkipAllowed() { return config.getBoolean("settings.allow-night-skip"); }
    public boolean shouldResetStatistics() { return config.getBoolean("settings.reset-statistics"); }
    public boolean shouldHealPlayers() { return config.getBoolean("settings.heal-players"); }
    public boolean shouldFeedPlayers() { return config.getBoolean("settings.feed-players"); }
    public boolean shouldClearWeather() { return config.getBoolean("settings.clear-weather"); }

    // Display settings
    public boolean isBossBarEnabled() { return config.getBoolean("display.boss-bar.enabled"); }
    public String getBossBarColor() { return config.getString("display.boss-bar.color"); }
    public String getBossBarStyle() { return config.getString("display.boss-bar.style"); }
    public boolean areTitlesEnabled() { return config.getBoolean("display.titles.enabled"); }
    public boolean areSoundsEnabled() { return config.getBoolean("display.sounds.enabled"); }
    public String getVoteSound() { return config.getString("display.sounds.vote-sound"); }
    public String getSkipSound() { return config.getString("display.sounds.skip-sound"); }
    public boolean isActionBarEnabled() { return config.getBoolean("display.actionbar.enabled"); }

    // Advanced settings
    public boolean isDatabaseEnabled() { return config.getBoolean("advanced.database.enabled"); }
    public String getDatabaseType() { return config.getString("advanced.database.type"); }
    public boolean isUpdateCheckEnabled() { return config.getBoolean("advanced.update-checker"); }
    public boolean isDebugMode() { return config.getBoolean("advanced.debug-mode"); }
    public boolean isMetricsEnabled() { return config.getBoolean("advanced.metrics"); }
    public List<String> getExemptGameModes() { return config.getStringList("advanced.exempt-gamemodes"); }
    public List<String> getExemptPermissions() { return config.getStringList("advanced.exempt-permissions"); }

    public void reload() {
        loadConfigurations();
    }

    private void saveWorldsConfig() {
        try {
            worldConfig.save(worldsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save worlds.yml file!");
        }
    }
}