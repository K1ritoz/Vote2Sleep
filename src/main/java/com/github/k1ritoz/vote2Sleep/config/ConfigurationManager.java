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
    private AutoConfigUpdater autoUpdater;

    public ConfigurationManager(Vote2Sleep plugin) {
        this.plugin = plugin;
        this.autoUpdater = new AutoConfigUpdater(plugin);
    }

    public void loadConfig() {
        loadConfigurations();

        // Automatically update config if needed (preserves comments and structure)
        autoUpdater.updateConfigIfNeeded();

        // Reload after update
        plugin.reloadConfig();
        this.config = plugin.getConfig();
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

        // Set default if not exist (this won't override existing values)
        setDefaults();
    }

    private void setDefaults() {
        // Main config defaults - only set if they don't exist
        if (!config.contains("settings.language")) config.set("settings.language", "en");
        if (!config.contains("settings.vote-percentage")) config.set("settings.vote-percentage", 0.5);
        if (!config.contains("settings.minimum-players")) config.set("settings.minimum-players", 1);
        if (!config.contains("settings.maximum-players")) config.set("settings.maximum-players", -1);
        if (!config.contains("settings.skip-delay-seconds")) config.set("settings.skip-delay-seconds", 3);
        if (!config.contains("settings.vote-timeout-seconds")) config.set("settings.vote-timeout-seconds", 60);
        if (!config.contains("settings.allow-storm-skip")) config.set("settings.allow-storm-skip", true);
        if (!config.contains("settings.allow-night-skip")) config.set("settings.allow-night-skip", true);
        if (!config.contains("settings.reset-statistics")) config.set("settings.reset-statistics", true);
        if (!config.contains("settings.heal-players")) config.set("settings.heal-players", false);
        if (!config.contains("settings.feed-players")) config.set("settings.feed-players", false);
        if (!config.contains("settings.clear-weather")) config.set("settings.clear-weather", true);

        // Display settings
        if (!config.contains("display.boss-bar.enabled")) config.set("display.boss-bar.enabled", true);
        if (!config.contains("display.boss-bar.color")) config.set("display.boss-bar.color", "BLUE");
        if (!config.contains("display.boss-bar.style")) config.set("display.boss-bar.style", "SOLID");
        if (!config.contains("display.titles.enabled")) config.set("display.titles.enabled", true);
        if (!config.contains("display.titles.fade-in")) config.set("display.titles.fade-in", 10);
        if (!config.contains("display.titles.stay")) config.set("display.titles.stay", 70);
        if (!config.contains("display.titles.fade-out")) config.set("display.titles.fade-out", 20);
        if (!config.contains("display.sounds.enabled")) config.set("display.sounds.enabled", true);
        if (!config.contains("display.sounds.vote-sound")) config.set("display.sounds.vote-sound", "BLOCK_NOTE_BLOCK_CHIME");
        if (!config.contains("display.sounds.skip-sound")) config.set("display.sounds.skip-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        if (!config.contains("display.actionbar.enabled")) config.set("display.actionbar.enabled", true);

        // Effects settings
        if (!config.contains("effects.lightning-on-skip")) config.set("effects.lightning-on-skip", false);
        if (!config.contains("effects.lightning-chance")) config.set("effects.lightning-chance", 0.3);
        if (!config.contains("effects.particle-effects")) config.set("effects.particle-effects", true);

        // Dawn Animation settings
        config.addDefault("animation.dawn.enabled", false);
        config.addDefault("animation.dawn.duration-seconds", 3);
        config.addDefault("animation.dawn.animation-steps", 32);
        config.addDefault("animation.dawn.title.enabled", true);
        config.addDefault("animation.dawn.boss-bar.enabled", true);
        config.addDefault("animation.dawn.boss-bar.color", "YELLOW");
        config.addDefault("animation.dawn.boss-bar.style", "SOLID");
        config.addDefault("animation.dawn.particles.enabled", true);
        config.addDefault("animation.dawn.particles.type", "ENCHANT");
        config.addDefault("animation.dawn.sounds.animation-sound", "BLOCK_AMETHYST_BLOCK_CHIME");
        config.addDefault("animation.dawn.sounds.final-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");

        // Advanced settings
        if (!config.contains("advanced.database.enabled")) config.set("advanced.database.enabled", false);
        if (!config.contains("advanced.database.type")) config.set("advanced.database.type", "SQLITE");
        if (!config.contains("advanced.update-checker")) config.set("advanced.update-checker", true);
        if (!config.contains("advanced.debug-mode")) config.set("advanced.debug-mode", false);
        if (!config.contains("advanced.metrics")) config.set("advanced.metrics", true);
        if (!config.contains("advanced.exempt-gamemodes")) config.set("advanced.exempt-gamemodes", Arrays.asList("SPECTATOR", "CREATIVE"));
        if (!config.contains("advanced.exempt-permissions")) config.set("advanced.exempt-permissions", Arrays.asList("vote2sleep.exempt"));

        // Save only if there were changes
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
    public int getTitleFadeIn() { return config.getInt("display.titles.fade-in"); }
    public int getTitleStay() { return config.getInt("display.titles.stay"); }
    public int getTitleFadeOut() { return config.getInt("display.titles.fade-out"); }
    public boolean areSoundsEnabled() { return config.getBoolean("display.sounds.enabled"); }
    public String getVoteSound() { return config.getString("display.sounds.vote-sound"); }
    public String getSkipSound() { return config.getString("display.sounds.skip-sound"); }
    public boolean isActionBarEnabled() { return config.getBoolean("display.actionbar.enabled"); }

    // Effects settings
    public boolean isLightningOnSkipEnabled() { return config.getBoolean("effects.lightning-on-skip"); }
    public double getLightningChance() { return config.getDouble("effects.lightning-chance"); }
    public boolean areParticleEffectsEnabled() { return config.getBoolean("effects.particle-effects"); }

    // Dawn Animation settings
    public boolean isDawnAnimationEnabled() { return config.getBoolean("animation.dawn.enabled"); }
    public int getDawnAnimationDuration() { return config.getInt("animation.dawn.duration-seconds"); }
    public int getDawnAnimationSteps() { return config.getInt("animation.dawn.animation-steps"); }
    public boolean isDawnTitleEnabled() { return config.getBoolean("animation.dawn.title.enabled"); }
    public boolean isDawnBossBarEnabled() { return config.getBoolean("animation.dawn.boss-bar.enabled"); }
    public String getDawnBossBarColor() { return config.getString("animation.dawn.boss-bar.color"); }
    public String getDawnBossBarStyle() { return config.getString("animation.dawn.boss-bar.style"); }
    public boolean isDawnParticlesEnabled() { return config.getBoolean("animation.dawn.particles.enabled"); }
    public String getDawnParticleType() { return config.getString("animation.dawn.particles.type"); }
    public String getDawnSound() { return config.getString("animation.dawn.sounds.animation-sound"); }
    public String getDawnFinalSound() { return config.getString("animation.dawn.sounds.final-sound"); }

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