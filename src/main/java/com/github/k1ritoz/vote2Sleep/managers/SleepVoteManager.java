package com.github.k1ritoz.vote2Sleep.managers;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import com.github.k1ritoz.vote2Sleep.api.events.*;
import com.github.k1ritoz.vote2Sleep.data.SleepVote;
import com.github.k1ritoz.vote2Sleep.data.WorldData;
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

    public SleepVoteManager(Vote2Sleep plugin) {
        this.plugin = plugin;
        this.worldDataMap = new ConcurrentHashMap<>();
        this.worldBossBars = new ConcurrentHashMap<>();
        this.skipTasks = new ConcurrentHashMap<>();
        this.voteTimeoutTasks = new ConcurrentHashMap<>();
    }

    public void startSleepVote(Player player) {
        World world = player.getWorld();

        // Check if world is enabled
        if (!plugin.getConfigManager().isWorldEnabled(world)) {
            plugin.getMessageManager().sendMessage(player, "world-not-enabled");
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
        }
    }

    public void removeSleepVote(Player player) {
        World world = player.getWorld();
        WorldData worldData = worldDataMap.get(world.getUID());

        if (worldData == null || !worldData.hasPlayerVoted(player.getUniqueId())) {
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

    private void scheduleNightSkip(World world) {
        // Cancel existing tasks
        cancelSkipTask(world);
        cancelVoteTimeoutTask(world);

        int delay = plugin.getConfigManager().getSkipDelaySeconds() * 20;

        // IMPORTANT: Use global scheduler for world operations in Folia
        BukkitTask task = plugin.getPlatformAdapter().runTaskLater(() -> {
            // Final check
            if (areRequirementsMet(world)) {
                executeNightSkip(world);
            }
        }, delay);

        skipTasks.put(world.getUID(), task);

        // Send countdown message
        plugin.getMessageManager().sendWorldMessage(world, "skip-countdown",
                Map.of("seconds", String.valueOf(plugin.getConfigManager().getSkipDelaySeconds())));
    }

    private void scheduleVoteTimeout(World world) {
        cancelVoteTimeoutTask(world);

        int timeout = plugin.getConfigManager().getVoteTimeoutSeconds() * 20;

        // Use global scheduler for world operations
        BukkitTask task = plugin.getPlatformAdapter().runTaskLater(() -> {
            clearVotes(world);
            plugin.getMessageManager().sendWorldMessage(world, "vote-timeout");
        }, timeout);

        voteTimeoutTasks.put(world.getUID(), task);
    }

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

        // Perform skip actions (this needs to be on global thread for Folia)
        performSkipActions(world, votes);

        // Fire post-skip event
        NightSkipEvent skipEvent = new NightSkipEvent(world, votes);
        Bukkit.getPluginManager().callEvent(skipEvent);

        // Clear votes and update displays
        clearVotes(world);

        // Save statistics if enabled
        if (plugin.getConfigManager().isDatabaseEnabled()) {
            plugin.getDatabaseManager().saveSkipEvent(world, votes);
        }
    }

    private void performSkipActions(World world, List<SleepVote> votes) {
        // Execute world operations on global thread for Folia compatibility
        plugin.getPlatformAdapter().runTaskLater(() -> {
            try {
                // Skip time
                if (isNight(world) && plugin.getConfigManager().isNightSkipAllowed()) {
                    world.setTime(1000); // Morning
                }

                // Clear weather
                if (plugin.getConfigManager().shouldClearWeather()) {
                    world.setStorm(false);
                    world.setThundering(false);
                }

                // Reset phantom statistics and heal/feed players
                for (SleepVote vote : votes) {
                    Player player = Bukkit.getPlayer(vote.getPlayerUUID());
                    if (player != null && player.isOnline()) {
                        // Schedule player operations on player's thread for Folia
                        plugin.getPlatformAdapter().runTaskLaterForPlayer(player, (p) -> {
                            try {
                                // Reset phantom statistics
                                if (plugin.getConfigManager().shouldResetStatistics()) {
                                    try {
                                        p.setStatistic(Statistic.TIME_SINCE_REST, 0);
                                    } catch (Exception e) {
                                        // Statistic might not exist in older versions
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
                            } catch (Exception e) {
                                plugin.getLogger().warning("Error performing player actions for " + p.getName() + ": " + e.getMessage());
                            }
                        }, 1L);
                    }
                }

                // Send messages and effects (schedule on global thread)
                plugin.getPlatformAdapter().runTaskLater(() -> {
                    plugin.getMessageManager().sendWorldMessage(world, "night-skipped");
                    plugin.getEffectsManager().playSkipEffects(world, votes);
                }, 2L);

            } catch (Exception e) {
                plugin.getLogger().severe("Error performing skip actions: " + e.getMessage());
                e.printStackTrace();
            }
        }, 1L);
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
        if (!plugin.getConfigManager().isBossBarEnabled()) return;

        BossBar bossBar = worldBossBars.computeIfAbsent(world.getUID(), k -> {
            BossBar bar = Bukkit.createBossBar("",
                    BarColor.valueOf(plugin.getConfigManager().getBossBarColor()),
                    BarStyle.valueOf(plugin.getConfigManager().getBossBarStyle()));

            // Add all world players
            for (Player player : world.getPlayers()) {
                bar.addPlayer(player);
            }

            return bar;
        });

        WorldData worldData = worldDataMap.get(world.getUID());
        if (worldData == null || worldData.getVotes().isEmpty()) {
            bossBar.setVisible(false);
            return;
        }

        int currentVotes = worldData.getVotes().size();
        int requiredVotes = getRequiredVotes(world);

        String title = plugin.getMessageManager().getBossBarTitle(currentVotes, requiredVotes);
        bossBar.setTitle(title);
        bossBar.setProgress(Math.min((double) currentVotes / requiredVotes, 1.0));
        bossBar.setVisible(true);

        // Ensure all current world players are added to the boss bar
        for (Player player : world.getPlayers()) {
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
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

    public void shutdown() {
        // Cancel all tasks
        skipTasks.values().forEach(task -> {
            if (!task.isCancelled()) task.cancel();
        });
        voteTimeoutTasks.values().forEach(task -> {
            if (!task.isCancelled()) task.cancel();
        });

        // Remove all boss bars
        worldBossBars.values().forEach(BossBar::removeAll);

        // Clear data
        worldDataMap.clear();
        worldBossBars.clear();
        skipTasks.clear();
        voteTimeoutTasks.clear();
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

        // Create a fake vote list with just the initiator
        List<SleepVote> fakeVotes = List.of(new SleepVote(
                initiator.getUniqueId(),
                initiator.getName(),
                LocalDateTime.now(),
                initiator.getLocation()
        ));

        // Perform skip actions
        performSkipActions(world, fakeVotes);

        // Send force skip message
        plugin.getMessageManager().sendWorldMessage(world, "force-skip-by-admin",
                Map.of("admin", initiator.getName()));
    }

    // Event handlers for player connections and world changes
    public void handlePlayerJoin(Player player) {
        World world = player.getWorld();
        // Add player to existing boss bars in their world with delay
        plugin.getPlatformAdapter().runTaskLater(() -> {
            addPlayerToBossBar(world, player);
        }, 20L);
    }

    public void handlePlayerQuit(Player player) {
        // Remove their vote if they had one
        if (hasPlayerVoted(player)) {
            removeSleepVote(player);
        }
    }

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
}