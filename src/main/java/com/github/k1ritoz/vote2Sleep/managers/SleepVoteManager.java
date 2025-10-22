package com.github.k1ritoz.vote2Sleep.managers;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import com.github.k1ritoz.vote2Sleep.api.events.*;
import com.github.k1ritoz.vote2Sleep.data.SleepVote;
import com.github.k1ritoz.vote2Sleep.data.WorldData;
import com.github.k1ritoz.vote2Sleep.platform.FoliaAdapter;
import com.github.k1ritoz.vote2Sleep.platform.PurpurAdapter;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SleepVoteManager {

    private final Vote2Sleep plugin;
    private final Map<UUID, WorldData> worldDataMap;
    private final Map<UUID, BossBar> worldBossBars;
    private final Map<UUID, BukkitTask> skipTasks;
    private final Map<UUID, BukkitTask> voteTimeoutTasks;
    private final Map<UUID, BukkitTask> nightCheckTasks;

    public SleepVoteManager(Vote2Sleep plugin) {
        this.plugin = plugin;
        this.worldDataMap = new ConcurrentHashMap<>();
        this.worldBossBars = new ConcurrentHashMap<>();
        this.skipTasks = new ConcurrentHashMap<>();
        this.voteTimeoutTasks = new ConcurrentHashMap<>();
        this.nightCheckTasks = new ConcurrentHashMap<>();
    }

    /**
     * Starts sleep vote from command (/sleep) - allows voting even if bed interaction is disabled
     */
    public void startSleepVote(Player player) {
        startSleepVoteInternal(player, true);
    }

    /**
     * Starts sleep vote from bed interaction - respects bed-interaction config
     */
    public void startSleepVoteFromBed(Player player) {
        // Check if bed interaction is enabled
        if (!plugin.getConfigManager().isBedInteractionEnabled()) {
            plugin.getMessageManager().sendMessage(player, "bed-interaction-disabled");
            return;
        }

        startSleepVoteInternal(player, false);
    }

    /**
     * Internal method for processing sleep votes
     */
    private void startSleepVoteInternal(Player player, boolean skipBedCheck) {
        World world = player.getWorld();

        // Check if world is enabled
        if (!plugin.getConfigManager().isWorldEnabled(world)) {
            plugin.getMessageManager().sendMessage(player, "world-not-enabled");
            return;
        }

        // Check if dawn animation is active
        if (plugin.getDawnAnimationManager().isAnimating(world)) {
            plugin.getMessageManager().sendMessage(player, "dawn-animation-active");
            return;
        }

        // Check if player is exempt
        if (isPlayerExempt(player)) {
            plugin.getMessageManager().sendMessage(player, "player-exempt");
            return;
        }

        // Check if sleeping is possible
        if (!canSleep(world)) {
            plugin.getMessageManager().sendMessage(player, "cannot-sleep-now");
            return;
        }

        WorldData worldData = getOrCreateWorldData(world);

        // Check if player already voted
        if (worldData.hasPlayerVoted(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "already-voted");
            return;
        }

        // Create sleep vote
        SleepVote vote = new SleepVote(
                player.getUniqueId(),
                player.getName(),
                LocalDateTime.now(),
                player.getLocation()
        );

        // Fire pre-vote event
        PreSleepVoteEvent preEvent = new PreSleepVoteEvent(player, world, vote);
        Bukkit.getPluginManager().callEvent(preEvent);

        if (preEvent.isCancelled()) {
            return;
        }

        // Add vote
        worldData.addVote(vote);

        // Update displays
        updateBossBar(world);
        sendVoteMessage(world);
        playVoteEffects(world, player);

        // Check if vote requirements are met
        if (areRequirementsMet(world)) {
            scheduleNightSkip(world);
        } else {
            scheduleVoteTimeout(world);
            scheduleNightCheckTask(world);
        }
    }

    public void removeSleepVote(Player player) {
        World world = player.getWorld();
        WorldData worldData = worldDataMap.get(world.getUID());

        if (worldData == null || !worldData.hasPlayerVoted(player.getUniqueId())) {
            return;
        }

        // Don't allow vote removal during dawn animation
        if (plugin.getDawnAnimationManager().isAnimating(world)) {
            plugin.getMessageManager().sendMessage(player, "dawn-animation-active");
            return;
        }

        SleepVote vote = worldData.getVote(player.getUniqueId());
        worldData.removeVote(player.getUniqueId());

        // Fire event
        SleepVoteRemovedEvent event = new SleepVoteRemovedEvent(player, world, vote);
        Bukkit.getPluginManager().callEvent(event);

        // Update displays
        updateBossBar(world);

        // Cancel skip if no longer enough votes
        if (!areRequirementsMet(world)) {
            cancelSkipTask(world);
        }

        plugin.getMessageManager().sendMessage(player, "vote-removed");
    }

    /**
     * Schedules a task to detect when night ends naturally (preventing boss bar stuck bug)
     */
    private void scheduleNightCheckTask(World world) {
        cancelNightCheckTask(world);

        // Check every second if night has ended
        BukkitTask task = plugin.getPlatformAdapter().runTaskTimerForWorld(world, (w) -> {
            // If world is no longer enabled, cancel the task
            if (!plugin.getConfigManager().isWorldEnabled(w)) {
                cancelNightCheckTask(w);
                return;
            }

            WorldData worldData = worldDataMap.get(w.getUID());
            if (worldData == null || worldData.isEmpty()) {
                cancelNightCheckTask(w);
                return;
            }

            // Check if night has ended naturally
            if (!canSleep(w)) {
                // Night/storm has ended naturally, clear all votes and boss bar
                clearVotes(w);
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Natural night/storm end detected in world " + w.getName() + ", clearing votes");
                }
            }
        }, 20L, 20L); // Check every second

        nightCheckTasks.put(world.getUID(), task);
    }

    private void scheduleNightSkip(World world) {
        // Cancel existing tasks
        cancelSkipTask(world);
        cancelVoteTimeoutTask(world);
        cancelNightCheckTask(world);

        int delay = plugin.getConfigManager().getSkipDelaySeconds() * 20;

        // Use global scheduler for the countdown, but world operations will use region scheduler
        BukkitTask task = plugin.getPlatformAdapter().runTaskLater(() -> {
            // Final check before executing skip
            if (areRequirementsMet(world)) {
                executeNightSkip(world);
            }
        }, delay);

        skipTasks.put(world.getUID(), task);

        // Send countdown message using global scheduler
        plugin.getPlatformAdapter().runTaskLater(() -> {
            boolean night = isNight(world);

            String skipKey = night ? "skip-countdown" : "storm-skip-countdown";

            plugin.getMessageManager().sendWorldMessage(world, skipKey,
                    Map.of("seconds", String.valueOf(plugin.getConfigManager().getSkipDelaySeconds())));
        }, 1L);
    }

    private void scheduleVoteTimeout(World world) {
        cancelVoteTimeoutTask(world);

        int timeout = plugin.getConfigManager().getVoteTimeoutSeconds() * 20;

        // Use global scheduler for world operations
        BukkitTask task = plugin.getPlatformAdapter().runTaskLater(() -> {
            // Check if votes still exist before clearing
            WorldData worldData = worldDataMap.get(world.getUID());
            if (worldData != null && !worldData.isEmpty()) {
                clearVotes(world);
                plugin.getMessageManager().sendWorldMessage(world, "vote-timeout");
            }
        }, timeout);

        voteTimeoutTasks.put(world.getUID(), task);
    }

    /**
     * Executes night skip with optional dawn animation
     */
    private void executeNightSkip(World world) {
        WorldData worldData = worldDataMap.get(world.getUID());
        if (worldData == null) return;

        List<SleepVote> votes = new ArrayList<>(worldData.getVotes().values());

        // Fire pre-skip event
        PreNightSkipEvent preEvent = new PreNightSkipEvent(world, votes);
        Bukkit.getPluginManager().callEvent(preEvent);

        if (preEvent.isCancelled()) {
            return;
        }

        // Check if we should start dawn animation (only for night skip)
        boolean isNightTime = isNight(world);
        boolean shouldAnimate = plugin.getConfigManager().isDawnAnimationEnabled() &&
                isNightTime &&
                plugin.getConfigManager().isNightSkipAllowed();

        if (shouldAnimate) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Starting dawn animation for night skip in world: " + world.getName());
            }

            // Start dawn animation
            plugin.getDawnAnimationManager().startDawnAnimation(world);

            // Delay the actual skip actions to let animation handle time progression
            plugin.getPlatformAdapter().runTaskLater(() -> {
                performSkipActions(world, votes, false); // false = don't set time, animation handles it

                // Fire post-skip event after animation delay
                NightSkipEvent skipEvent = new NightSkipEvent(world, votes);
                Bukkit.getPluginManager().callEvent(skipEvent);
            }, plugin.getConfigManager().getDawnAnimationDuration() * 20L);

        } else {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Performing immediate skip actions (no animation) for world: " + world.getName());
            }

            // Immediately perform skip actions if animation is disabled or it's storm skip
            performSkipActions(world, votes, true); // true = set time normally

            // Fire post-skip event immediately
            NightSkipEvent skipEvent = new NightSkipEvent(world, votes);
            Bukkit.getPluginManager().callEvent(skipEvent);
        }

        // Clear votes and update displays
        clearVotes(world);

        // Save statistics if enabled
        if (plugin.getConfigManager().isDatabaseEnabled()) {
            plugin.getDatabaseManager().saveSkipEvent(world, votes);
        }
    }

    /**
     * Performs the actual skip actions (world operations, player effects, messages)
     */
    private void performSkipActions(World world, List<SleepVote> votes, boolean shouldSetTime) {
        boolean wasNight = isNight(world);
        // WORLD OPERATIONS - For Folia, setTime() and weather must use GLOBAL scheduler
        plugin.getPlatformAdapter().runTaskLaterForWorld(world, (w) -> {
            try {
                // Only set time if not handled by animation
                if (shouldSetTime) {
                    // Use Purpur optimizations if available
                    if (plugin.getPlatformAdapter() instanceof PurpurAdapter) {
                        PurpurAdapter purpurAdapter = (PurpurAdapter) plugin.getPlatformAdapter();

                        // Skip time with Purpur optimization
                        if (isNight(w) && plugin.getConfigManager().isNightSkipAllowed()) {
                            purpurAdapter.setWorldTimeOptimized(w, 1000);
                        }

                        // Clear weather with Purpur optimization
                        if (plugin.getConfigManager().shouldClearWeather()) {
                            purpurAdapter.clearWeatherOptimized(w);
                        }
                    } else {
                        // Standard operations - in Folia these operations run on global thread
                        if (isNight(w) && plugin.getConfigManager().isNightSkipAllowed()) {
                            w.setTime(1000);
                        }

                        if (plugin.getConfigManager().shouldClearWeather()) {
                            w.setStorm(false);
                            w.setThundering(false);
                        }
                    }
                } else {
                    // Animation handled time, but we still need to clear weather if needed
                    if (plugin.getConfigManager().shouldClearWeather()) {
                        if (plugin.getPlatformAdapter() instanceof PurpurAdapter) {
                            ((PurpurAdapter) plugin.getPlatformAdapter()).clearWeatherOptimized(w);
                        } else {
                            w.setStorm(false);
                            w.setThundering(false);
                        }
                    }
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Error performing world skip actions: " + e.getMessage());
                e.printStackTrace();
            }
        }, 1L);

        // PLAYER OPERATIONS - each player needs its own task for Folia
        for (SleepVote vote : votes) {
            Player player = Bukkit.getPlayer(vote.getPlayerUUID());
            if (player != null && player.isOnline()) {
                plugin.getPlatformAdapter().runTaskLaterForPlayer(player, (p) -> {
                    try {
                        // Reset phantom statistics
                        if (plugin.getConfigManager().shouldResetStatistics()) {
                            try {
                                p.setStatistic(Statistic.TIME_SINCE_REST, 0);
                            } catch (Exception e) {
                                // Silent fail for older versions
                            }
                        }

                        // Heal and feed players
                        if (plugin.getConfigManager().shouldHealPlayers()) {
                            p.setHealth(p.getMaxHealth());
                        }
                        if (plugin.getConfigManager().shouldFeedPlayers()) {
                            p.setFoodLevel(20);
                            p.setSaturation(20.0f);
                        }

                        // Use Purpur optimization if available
                        if (plugin.getPlatformAdapter() instanceof PurpurAdapter) {
                            ((PurpurAdapter) plugin.getPlatformAdapter()).optimizePlayerOperation(p, () -> {
                                // Additional Purpur optimizations here if needed
                            });
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error performing player actions for " + p.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }, 2L); // Delay slightly to ensure world operations complete first
            }
        }

        // MESSAGES - use global scheduler (only if animation is disabled or it's storm skip)
        if (shouldSetTime) {
            plugin.getPlatformAdapter().runTaskLater(() -> {
                try {
                    if (wasNight) {
                        plugin.getMessageManager().sendWorldMessage(world, "night-skipped");
                    } else {
                        plugin.getMessageManager().sendWorldMessage(world, "storm-skipped");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error sending skip messages: " + e.getMessage());
                    e.printStackTrace();
                }
            }, 3L);
        }

        // EFFECTS - for effects that need specific coordinates, use region (only if animation is disabled)
        if (shouldSetTime) {
            if (plugin.getPlatformAdapter() instanceof FoliaAdapter) {
                FoliaAdapter foliaAdapter = (FoliaAdapter) plugin.getPlatformAdapter();
                foliaAdapter.runRegionSpecificEffects(world, (w) -> {
                    try {
                        plugin.getEffectsManager().playSkipEffects(w, votes, wasNight);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error playing skip effects: " + e.getMessage());
                        e.printStackTrace();
                    }
                }, 4L);
            } else {
                // For other platforms, use normal scheduler
                plugin.getPlatformAdapter().runTaskLater(() -> {
                    try {
                        plugin.getEffectsManager().playSkipEffects(world, votes, wasNight);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error playing skip effects: " + e.getMessage());
                        e.printStackTrace();
                    }
                }, 4L);
            }
        }
    }

    // Utility methods
    private WorldData getOrCreateWorldData(World world) {
        return worldDataMap.computeIfAbsent(world.getUID(), k -> new WorldData(world));
    }

    private boolean isPlayerExempt(Player player) {
        // Check game mode exemptions
        String gameMode = player.getGameMode().name();
        if (plugin.getConfigManager().getExemptGameModes().contains(gameMode)) {
            return true;
        }

        // Check permission exemptions
        for (String permission : plugin.getConfigManager().getExemptPermissions()) {
            if (player.hasPermission(permission)) {
                return true;
            }
        }

        return false;
    }

    private boolean canSleep(World world) {
        boolean isNight = isNight(world);
        boolean isStormy = world.hasStorm() || world.isThundering();

        return (isNight && plugin.getConfigManager().isNightSkipAllowed()) ||
                (isStormy && plugin.getConfigManager().isStormSkipAllowed());
    }

    private boolean isNight(World world) {
        long time = world.getTime();
        return time >= 12542 && time <= 23459;
    }

    private boolean areRequirementsMet(World world) {
        WorldData worldData = worldDataMap.get(world.getUID());
        if (worldData == null) return false;

        int currentVotes = worldData.getVotes().size();
        int requiredVotes = getRequiredVotes(world);

        return currentVotes >= requiredVotes;
    }

    private int getEligiblePlayerCount(World world) {
        return (int) world.getPlayers().stream()
                .filter(p -> !isPlayerExempt(p))
                .count();
    }

    private void updateBossBar(World world) {
        if (!plugin.getConfigManager().isBossBarEnabled()) {
            // If BossBar was disabled during reload, remove the existing one
            BossBar existingBar = worldBossBars.remove(world.getUID());
            if (existingBar != null) {
                try {
                    existingBar.removeAll();
                } catch (Exception e) {
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().warning("Error removing disabled BossBar: " + e.getMessage());
                    }
                }
            }
            return;
        }

        BossBar bossBar = worldBossBars.get(world.getUID());

        // Determine if we need to recreate the BossBar
        boolean needsRecreation = false;

        if (bossBar == null) {
            needsRecreation = true;
        } else {
            // Check if configuration has changed
            try {
                BarColor currentColor = bossBar.getColor();
                BarStyle currentStyle = bossBar.getStyle();

                BarColor configColor = BarColor.valueOf(plugin.getConfigManager().getBossBarColor());
                BarStyle configStyle = BarStyle.valueOf(plugin.getConfigManager().getBossBarStyle());

                // If color or style differ, mark for recreation
                if (currentColor != configColor || currentStyle != configStyle) {
                    needsRecreation = true;
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("BossBar settings changed after reload, recreating for world " + world.getName());
                    }
                }

                // Validate existing BossBar by accessing its properties
                bossBar.getTitle();
                bossBar.getProgress();
                bossBar.isVisible();

            } catch (IllegalArgumentException e) {
                // Invalid configuration values
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("Invalid BossBar configuration detected, using defaults: " + e.getMessage());
                }
                needsRecreation = true;
            } catch (Exception e) {
                // Corrupted BossBar instance
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("BossBar corrupted for world " + world.getName() + ", recreating: " + e.getMessage());
                }
                needsRecreation = true;
            }
        }

        if (needsRecreation) {
            // Safely remove old BossBar if it exists
            if (bossBar != null) {
                try {
                    bossBar.removeAll();
                } catch (Exception ignored) {}
                worldBossBars.remove(world.getUID());
            }

            try {
                // Create new BossBar with validated config
                BarColor barColor;
                BarStyle barStyle;

                try {
                    barColor = BarColor.valueOf(plugin.getConfigManager().getBossBarColor());
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid boss bar color, using default BLUE");
                    barColor = BarColor.BLUE;
                }

                try {
                    barStyle = BarStyle.valueOf(plugin.getConfigManager().getBossBarStyle());
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid boss bar style, using default SOLID");
                    barStyle = BarStyle.SOLID;
                }

                bossBar = Bukkit.createBossBar("", barColor, barStyle);
                worldBossBars.put(world.getUID(), bossBar);

                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Created new BossBar for world " + world.getName());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create BossBar for world " + world.getName() + ": " + e.getMessage());
                return;
            }
        }

        WorldData worldData = worldDataMap.get(world.getUID());

        if (worldData == null || worldData.getVotes().isEmpty()) {
            // Hide BossBar if no votes
            try {
                bossBar.setVisible(false);
                bossBar.removeAll();
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("Error hiding BossBar: " + e.getMessage());
                }
            }
        } else {
            int currentVotes = worldData.getVotes().size();
            int requiredVotes = getRequiredVotes(world);

            try {
                String title = plugin.getMessageManager().getBossBarTitle(currentVotes, requiredVotes);
                bossBar.setTitle(title);
                bossBar.setProgress(Math.min((double) currentVotes / requiredVotes, 1.0));
                bossBar.setVisible(true);

                // Synchronize players in BossBar with world players
                Set<Player> bossBarPlayers = new HashSet<>(bossBar.getPlayers());
                Set<Player> worldPlayers = new HashSet<>(world.getPlayers());

                // Add missing players
                for (Player player : worldPlayers) {
                    if (!bossBarPlayers.contains(player)) {
                        try {
                            bossBar.addPlayer(player);
                            if (plugin.getConfigManager().isDebugMode()) {
                                plugin.getLogger().info("Added missing player " + player.getName() + " to BossBar");
                            }
                        } catch (Exception e) {
                            if (plugin.getConfigManager().isDebugMode()) {
                                plugin.getLogger().warning("Failed to add player " + player.getName() + " to BossBar: " + e.getMessage());
                            }
                        }
                    }
                }

                // Remove players no longer in the world
                for (Player player : bossBarPlayers) {
                    if (!worldPlayers.contains(player)) {
                        try {
                            bossBar.removePlayer(player);
                            if (plugin.getConfigManager().isDebugMode()) {
                                plugin.getLogger().info("Removed player " + player.getName() + " from BossBar (not in world)");
                            }
                        } catch (Exception e) {
                            if (plugin.getConfigManager().isDebugMode()) {
                                plugin.getLogger().warning("Failed to remove player " + player.getName() + " from BossBar: " + e.getMessage());
                            }
                        }
                    }
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Error updating BossBar for world " + world.getName() + ": " + e.getMessage());

                // On repeated errors, remove corrupted BossBar
                worldBossBars.remove(world.getUID());
                try {
                    bossBar.removeAll();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Specific method to synchronize BossBars after config reload
     */
    public void synchronizeBossBarSettings() {
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Synchronizing BossBar settings after configuration reload...");
        }

        // Update all existing BossBars
        for (UUID worldUUID : new HashSet<>(worldBossBars.keySet())) {
            World world = Bukkit.getWorld(worldUUID);
            if (world != null) {
                updateBossBar(world);
            } else {
                // World no longer exists, remove BossBar
                BossBar bossBar = worldBossBars.remove(worldUUID);
                if (bossBar != null) {
                    try {
                        bossBar.removeAll();
                    } catch (Exception ignored) {}
                }
            }
        }

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("BossBar synchronization completed");
        }
    }

    private void sendVoteMessage(World world) {
        WorldData worldData = worldDataMap.get(world.getUID());
        if (worldData == null) return;

        int currentVotes = worldData.getVotes().size();
        int requiredVotes = getRequiredVotes(world);

        plugin.getMessageManager().sendWorldMessage(world, "vote-cast", Map.of(
                "current", String.valueOf(currentVotes),
                "required", String.valueOf(requiredVotes),
                "remaining", String.valueOf(requiredVotes - currentVotes)
        ));
    }

    private void playVoteEffects(World world, Player voter) {
        // Use the effects manager
        plugin.getEffectsManager().playVoteEffects(world, voter);

        // Send action bar to all players in the world showing who voted
        if (plugin.getConfigManager().isActionBarEnabled()) {
            WorldData worldData = worldDataMap.get(world.getUID());
            if (worldData == null) return;

            int currentVotes = worldData.getVotes().size();
            int requiredVotes = getRequiredVotes(world);

            for (Player player : world.getPlayers()) {
                plugin.getMessageManager().sendActionBar(player, "vote-cast-actionbar",
                        Map.of("player", voter.getName(),
                                "current", String.valueOf(currentVotes),
                                "required", String.valueOf(requiredVotes),
                                "remaining", String.valueOf(requiredVotes - currentVotes)));
            }
        }
    }

    public void clearVotes(World world) {
        worldDataMap.remove(world.getUID());
        updateBossBar(world);
        cancelSkipTask(world);
        cancelVoteTimeoutTask(world);
        cancelNightCheckTask(world);
    }

    private void cancelSkipTask(World world) {
        BukkitTask task = skipTasks.remove(world.getUID());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    private void cancelVoteTimeoutTask(World world) {
        BukkitTask task = voteTimeoutTasks.remove(world.getUID());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    private void cancelNightCheckTask(World world) {
        BukkitTask task = nightCheckTasks.remove(world.getUID());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    // Public getters for API
    public WorldData getWorldData(World world) {
        return worldDataMap.get(world.getUID());
    }

    public boolean hasPlayerVoted(Player player) {
        WorldData data = worldDataMap.get(player.getWorld().getUID());
        return data != null && data.hasPlayerVoted(player.getUniqueId());
    }

    public int getCurrentVotes(World world) {
        WorldData data = worldDataMap.get(world.getUID());
        return data != null ? data.getVotes().size() : 0;
    }

    public int getRequiredVotes(World world) {
        int onlinePlayers = getEligiblePlayerCount(world);
        double percentage = plugin.getConfigManager().getVotePercentage(world);
        int required = (int) Math.ceil(onlinePlayers * percentage);

        int minimum = plugin.getConfigManager().getMinimumPlayers();
        int maximum = plugin.getConfigManager().getMaximumPlayers();

        required = Math.max(required, minimum);
        if (maximum > 0) {
            required = Math.min(required, maximum);
        }

        return Math.min(required, onlinePlayers);
    }

    public void forceSkip(World world, Player initiator) {
        if (!plugin.getConfigManager().isWorldEnabled(world)) {
            plugin.getMessageManager().sendMessage(initiator, "world-not-enabled");
            return;
        }

        // Don't allow force skip during dawn animation
        if (plugin.getDawnAnimationManager().isAnimating(world)) {
            plugin.getMessageManager().sendMessage(initiator, "dawn-animation-active");
            return;
        }

        // Create a fake vote list with just the initiator
        List<SleepVote> fakeVotes = List.of(new SleepVote(
                initiator.getUniqueId(),
                initiator.getName(),
                LocalDateTime.now(),
                initiator.getLocation()
        ));

        // Perform skip actions
        performSkipActions(world, fakeVotes, true);

        // Send force skip message
        plugin.getMessageManager().sendWorldMessage(world, "force-skip-by-admin",
                Map.of("admin", initiator.getName()));
    }

    /**
     * Event handlers for player connections and world changes
     */
    public void handlePlayerJoin(Player player) {
        World world = player.getWorld();

        plugin.getPlatformAdapter().runTaskLater(() -> {
            updateBossBar(world);
            addPlayerToBossBar(world, player);

            if (plugin.getDawnAnimationManager() != null) {
                plugin.getDawnAnimationManager().handlePlayerJoin(player);
            }
        }, 20L);
    }

    /**
     * Handles player quitting from the server
     */
    public void handlePlayerQuit(Player player) {
        // Remove their vote if they had one
        if (hasPlayerVoted(player)) {
            removeSleepVote(player);
        }

        // Handle dawn animation player quit
        if (plugin.getDawnAnimationManager() != null) {
            plugin.getDawnAnimationManager().handlePlayerQuit(player);
        }
    }

    /**
     * Handles player changing worlds
     */
    public void handlePlayerChangeWorld(Player player, World fromWorld, World toWorld) {
        // Remove vote from old world
        WorldData fromData = worldDataMap.get(fromWorld.getUID());
        if (fromData != null && fromData.hasPlayerVoted(player.getUniqueId())) {
            SleepVote vote = fromData.getVote(player.getUniqueId());
            fromData.removeVote(player.getUniqueId());

            // Fire event
            SleepVoteRemovedEvent event = new SleepVoteRemovedEvent(player, fromWorld, vote);
            Bukkit.getPluginManager().callEvent(event);

            updateBossBar(fromWorld);
        }

        // Add to new world's boss bar
        addPlayerToBossBar(toWorld, player);
    }

    private void addPlayerToBossBar(World world, Player player) {
        BossBar bossBar = worldBossBars.get(world.getUID());
        if (bossBar != null && !bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }
    }

    public void shutdown() {
        // Cancel all tasks
        skipTasks.values().forEach(task -> {
            if (!task.isCancelled()) task.cancel();
        });
        voteTimeoutTasks.values().forEach(task -> {
            if (!task.isCancelled()) task.cancel();
        });
        nightCheckTasks.values().forEach(task -> {
            if (!task.isCancelled()) task.cancel();
        });

        // Remove all boss bars
        worldBossBars.values().forEach(BossBar::removeAll);

        // Clear data
        worldDataMap.clear();
        worldBossBars.clear();
        skipTasks.clear();
        voteTimeoutTasks.clear();
        nightCheckTasks.clear();
    }
}