package com.github.k1ritoz.vote2Sleep;

import com.github.k1ritoz.vote2Sleep.api.Vote2SleepAPI;
import com.github.k1ritoz.vote2Sleep.commands.SleepCommand;
import com.github.k1ritoz.vote2Sleep.config.ConfigurationManager;
import com.github.k1ritoz.vote2Sleep.listeners.*;
import com.github.k1ritoz.vote2Sleep.managers.*;
import com.github.k1ritoz.vote2Sleep.platform.PlatformAdapter;
import com.github.k1ritoz.vote2Sleep.platform.PlatformDetector;
import com.github.k1ritoz.vote2Sleep.utils.UpdateChecker;

import org.bukkit.plugin.java.JavaPlugin;

public final class Vote2Sleep extends JavaPlugin {

    private static Vote2Sleep instance;
    private PlatformAdapter platformAdapter;
    private ConfigurationManager configManager;
    private MessageManager messageManager;
    private SleepVoteManager voteManager;
    private EffectsManager effectsManager;
    private DatabaseManager databaseManager;
    private HooksManager hooksManager;
    private DawnAnimationManager dawnAnimationManager;
    private Vote2SleepAPI api;
    private UpdateChecker updateChecker;

    @Override
    public void onLoad() {
        instance = this;

        // Detect platform early
        this.platformAdapter = PlatformDetector.createAdapter();
        getLogger().info("Detected platform: " + platformAdapter.getPlatformName());
    }

    @Override
    public void onEnable() {
        // Version compatibility check
        if (!platformAdapter.isVersionSupported()) {
            getLogger().severe("Unsupported server version! Requires MC 1.21+");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            // Initialize core components
            initializeManagers();

            // Register commands and listeners
            registerCommands();
            registerListeners();

            // Initialize API
            this.api = new Vote2SleepAPI(this);

            // Initialize bStats metrics
            if (configManager.isMetricsEnabled()) {
                initializeMetrics();
            }

            // Check for updates
            if (configManager.isUpdateCheckEnabled()) {
                try {
                    // Initialize update checker
                    this.updateChecker = new UpdateChecker(this, "https://api.github.com/repos/k1ritoz/Vote2Sleep/releases/latest");

                    // Verify initialization was successful
                    if (this.updateChecker != null) {
                        // Perform initial check and start periodic checking
                        updateChecker.checkForUpdates();
                        updateChecker.startPeriodicChecking();

                        if (configManager.isDebugMode()) {
                            getLogger().info("Update checker initialized with 24h periodic checks");
                        }
                    } else {
                        getLogger().warning("Failed to initialize update checker - instance is null");
                    }
                } catch (Exception e) {
                    getLogger().warning("Failed to initialize update checker: " + e.getMessage());
                    if (configManager.isDebugMode()) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (configManager.isDebugMode()) {
                    getLogger().info("Update checker disabled in configuration");
                }
            }

            getLogger().info("Vote2Sleep v" + getDescription().getVersion() + " enabled successfully!");
            getLogger().info("Running on " + platformAdapter.getPlatformName());

        } catch (Exception e) {
            getLogger().severe("Failed to enable Vote2Sleep: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    // Initialize bStats metrics
    private void initializeMetrics() {
        try {
            // Plugin ID from bStats
            int pluginId = 26722;
            org.bstats.bukkit.Metrics metrics = new org.bstats.bukkit.Metrics(this, pluginId);
        } catch (Exception e) {
            // Don't fail plugin loading if metrics fail
        }
    }

    @Override
    public void onDisable() {
        try {
            // Shutdown managers in reverse order
            if (voteManager != null) {
                voteManager.shutdown();
            }

            if (dawnAnimationManager != null) {
                dawnAnimationManager.cleanup();
            }

            if (hooksManager != null) {
                hooksManager.cleanup();
            }

            if (databaseManager != null) {
                databaseManager.close();
            }

            if (effectsManager != null) {
                effectsManager.cleanup();
            }

            if (updateChecker != null) {
                updateChecker.stopPeriodicChecking();
            }

            getLogger().info("Vote2Sleep disabled successfully!");

        } catch (Exception e) {
            getLogger().severe("Error during plugin shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeManagers() {
        try {
            // Initialize configuration manager first and load config
            this.configManager = new ConfigurationManager(this);
            configManager.loadConfig();
            getLogger().info("Configuration loaded");

            // Initialize message manager and load messages
            this.messageManager = new MessageManager(this);
            messageManager.loadMessages();
            getLogger().info("Messages loaded");

            // Initialize database manager (now config is loaded)
            this.databaseManager = new DatabaseManager(this);
            getLogger().info("Database manager initialized");

            // Initialize hooks manager
            this.hooksManager = new HooksManager(this);
            getLogger().info("Hooks manager initialized");

            // Initialize effects manager
            this.effectsManager = new EffectsManager(this);
            getLogger().info("Effects manager initialized");

            // Initialize dawn animation manager
            this.dawnAnimationManager = new DawnAnimationManager(this);
            getLogger().info("Dawn animation manager initialized");

            // Initialize vote manager
            this.voteManager = new SleepVoteManager(this);
            getLogger().info("Vote manager initialized");

            // Initialize hooks after all managers are created
            hooksManager.initializeHooks();

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize managers", e);
        }
    }

    private void registerCommands() {
        try {
            // Register main command with all aliases
            if (getCommand("sleep") != null) {
                SleepCommand sleepCommand = new SleepCommand(this);
                getCommand("sleep").setExecutor(sleepCommand);
                getCommand("sleep").setTabCompleter(sleepCommand);
            }

            // Register alternative command names if they exist
            if (getCommand("v2s") != null) {
                SleepCommand sleepCommand = new SleepCommand(this);
                getCommand("v2s").setExecutor(sleepCommand);
                getCommand("v2s").setTabCompleter(sleepCommand);
            }

            if (getCommand("vote2sleep") != null) {
                SleepCommand sleepCommand = new SleepCommand(this);
                getCommand("vote2sleep").setExecutor(sleepCommand);
                getCommand("vote2sleep").setTabCompleter(sleepCommand);
            }

            getLogger().info("Commands registered successfully");

        } catch (Exception e) {
            throw new RuntimeException("Failed to register commands", e);
        }
    }

    private void registerListeners() {
        try {
            getServer().getPluginManager().registerEvents(new PlayerBedListener(this), this);
            getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
            getServer().getPluginManager().registerEvents(new WorldEventListener(this), this);
            getServer().getPluginManager().registerEvents(new WeatherChangeListener(this), this);

            getLogger().info("Event listeners registered successfully");

        } catch (Exception e) {
            throw new RuntimeException("Failed to register listeners", e);
        }
    }

    /**
     * Reload the plugin configuration and components
     */
    public void reloadPlugin() {
        try {
            // Reload configuration
            if (configManager != null) {
                configManager.reload();
            }

            // Reload messages
            if (messageManager != null) {
                messageManager.reload();
            }

            // Reinitialize hooks
            if (hooksManager != null) {
                hooksManager.reinitializeHooks();
            }

            getLogger().info("Plugin reloaded successfully");

        } catch (Exception e) {
            getLogger().severe("Error during plugin reload: " + e.getMessage());
            throw e;
        }
    }

    // Getters with null checks for safety
    public static Vote2Sleep getInstance() {
        return instance;
    }

    public PlatformAdapter getPlatformAdapter() {
        return platformAdapter;
    }

    public ConfigurationManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public SleepVoteManager getVoteManager() {
        return voteManager;
    }

    public EffectsManager getEffectsManager() {
        return effectsManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public HooksManager getHooksManager() {
        return hooksManager;
    }

    public DawnAnimationManager getDawnAnimationManager() {
        return dawnAnimationManager;
    }

    public Vote2SleepAPI getApi() {
        return api;
    }

    /**
     * Check if the plugin is fully initialized
     */
    public boolean isFullyInitialized() {
        return configManager != null &&
                messageManager != null &&
                voteManager != null &&
                effectsManager != null &&
                databaseManager != null &&
                hooksManager != null &&
                dawnAnimationManager != null;
    }
}