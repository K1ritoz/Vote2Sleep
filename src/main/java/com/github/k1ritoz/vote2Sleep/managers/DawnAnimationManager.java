package com.github.k1ritoz.vote2Sleep.managers;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dawn animation effects when sleep voting is successful
 */
public class DawnAnimationManager {

    private final Vote2Sleep plugin;
    private final Map<UUID, BukkitTask> activeAnimations;
    private final Map<UUID, BossBar> animationBossBars;

    // Thread-safe operations for Folia
    private final Object animationLock = new Object();

    public DawnAnimationManager(Vote2Sleep plugin) {
        this.plugin = plugin;
        this.activeAnimations = new ConcurrentHashMap<>();
        this.animationBossBars = new ConcurrentHashMap<>();
    }

    /**
     * Starts the dawn animation for a world (only for night skip)
     * Thread-safe for Folia
     */
    public void startDawnAnimation(World world) {
        if (!plugin.getConfigManager().isDawnAnimationEnabled()) {
            return;
        }

        // Thread-safe check and start
        synchronized (animationLock) {
            if (activeAnimations.containsKey(world.getUID())) {
                return;
            }

            // Only animate for night skip, not storm skip
            if (!isNight(world)) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Dawn animation skipped - not night time");
                }
                return;
            }

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Starting dawn animation for world: " + world.getName());
            }

            // Clear existing vote boss bar first
            plugin.getPlatformAdapter().runTaskLater(() -> {
                try {
                    plugin.getVoteManager().clearVotes(world);
                } catch (Exception e) {
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().warning("Error clearing votes before animation: " + e.getMessage());
                    }
                }
            }, 1L);

            startTimeAcceleration(world);
        }
    }

    /**
     * Starts efficient time acceleration animation
     */
    private void startTimeAcceleration(World world) {
        int duration = Math.max(1, Math.min(plugin.getConfigManager().getDawnAnimationDuration(), 30));
        int totalSteps = Math.max(8, Math.min(plugin.getConfigManager().getDawnAnimationSteps(), 256));

        // Calculate time progression
        long startTime = world.getTime();
        long endTime = 1000L; // Day time
        long totalTimeToAdvance = calculateTimeDistance(startTime, endTime);

        // CRITICAL: Calculate ticks per step based on DURATION, not steps
        long ticksPerStep = Math.max((duration * 20L) / totalSteps, 1L);
        long timePerStep = Math.max(totalTimeToAdvance / totalSteps, 10);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Dawn animation: " + totalTimeToAdvance + " time units over " +
                    totalSteps + " steps in " + duration + " seconds (" + timePerStep + " time per step, " +
                    ticksPerStep + " ticks per step)");
        }

        // Create visual effects after clearing votes
        plugin.getPlatformAdapter().runTaskLater(() -> {
            createAnimationVisuals(world);
        }, 3L);

        final int[] currentStep = {0};
        final long finalTimePerStep = timePerStep;
        final int finalTotalSteps = totalSteps;

        // Use world-specific timer task
        BukkitTask animationTask = plugin.getPlatformAdapter().runTaskTimerForWorld(world, (w) -> {
            if (currentStep[0] >= finalTotalSteps) {
                finalizeDawnAnimation(w);
                return;
            }

            performTimeStep(w, startTime, finalTimePerStep, currentStep[0], finalTotalSteps);
            currentStep[0]++;

        }, 5L, ticksPerStep); // Start after visual effects are created

        activeAnimations.put(world.getUID(), animationTask);
    }

    /**
     * Performs a single time acceleration step
     */
    private void performTimeStep(World world, long startTime, long timePerStep,
                                 long currentStep, int totalSteps) {

        // Ensure animation is still active (thread-safe check)
        if (!activeAnimations.containsKey(world.getUID())) {
            return;
        }

        try {
            // Calculate and set new time directly in world task
            long newTime = (startTime + (timePerStep * (currentStep + 1))) % 24000;
            world.setTime(newTime);

            // Update progress immediately
            double progress = (double) (currentStep + 1) / totalSteps;
            updateAnimationProgress(world, progress);

        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().warning("Error during dawn animation time step: " + e.getMessage());
            }
            stopDawnAnimation(world);
        }
    }

    /**
     * Creates initial visual effects
     */
    private void createAnimationVisuals(World world) {
        // Create boss bar
        if (plugin.getConfigManager().isDawnBossBarEnabled()) {
            createAnimationBossBar(world);
        }

        // Send initial title
        if (plugin.getConfigManager().isDawnTitleEnabled()) {
            sendAnimationTitle(world, "dawn-animation-start");
        }

        // Play initial sound
        if (plugin.getConfigManager().areSoundsEnabled()) {
            playDawnSound(world, plugin.getConfigManager().getDawnSound());
        }
    }

    /**
     * Updates animation progress efficiently
     */
    private void updateAnimationProgress(World world, double progress) {
        // Update boss bar
        BossBar bossBar = animationBossBars.get(world.getUID());
        if (bossBar != null) {
            bossBar.setProgress(Math.min(progress, 1.0));

            String title = plugin.getMessageManager().getMessage("dawn-animation-progress",
                    Map.of("progress", String.valueOf((int)(progress * 100))));
            bossBar.setTitle(title);
        }

        // Spawn particles occasionally and only near players
        int totalSteps = plugin.getConfigManager().getDawnAnimationSteps();
        double particleChance = Math.max(0.1, Math.min(0.6, 10.0 / totalSteps));

        if (plugin.getConfigManager().isDawnParticlesEnabled() && Math.random() < particleChance) {
            spawnDawnParticles(world);
        }
    }

    /**
     * Spawns dawn-themed particles efficiently
     */
    private void spawnDawnParticles(World world) {
        String particleType = plugin.getConfigManager().getDawnParticleType();

        // Only spawn particles around players
        for (Player player : world.getPlayers()) {
            if (Math.random() < 0.6) { // 60% chance per player
                Location loc = player.getLocation().add(0, 2, 0);

                try {
                    Particle particle = Particle.valueOf(particleType.toUpperCase());
                    world.spawnParticle(particle, loc, 3, 1.5, 0.8, 1.5, 0.05);
                } catch (IllegalArgumentException e) {
                    world.spawnParticle(Particle.ENCHANT, loc, 3, 1.5, 0.8, 1.5, 0.05);
                }
            }
        }
    }

    /**
     * Creates animation boss bar
     */
    private void createAnimationBossBar(World world) {
        String title = plugin.getMessageManager().getMessage("dawn-animation-title");
        BossBar bossBar = Bukkit.createBossBar(
                title,
                BarColor.valueOf(plugin.getConfigManager().getDawnBossBarColor()),
                BarStyle.valueOf(plugin.getConfigManager().getDawnBossBarStyle())
        );

        for (Player player : world.getPlayers()) {
            bossBar.addPlayer(player);
        }

        bossBar.setVisible(true);
        animationBossBars.put(world.getUID(), bossBar);
    }

    /**
     * Sends animation title
     */
    private void sendAnimationTitle(World world, String messageKey) {
        if (!plugin.getConfigManager().areTitlesEnabled()) {
            return;
        }

        String title = plugin.getMessageManager().getMessage(messageKey + "-title");
        String subtitle = plugin.getMessageManager().getMessage(messageKey + "-subtitle");

        for (Player player : world.getPlayers()) {
            player.sendTitle(title, subtitle, 10, 40, 10);
        }
    }

    /**
     * Plays dawn animation sound
     */
    private void playDawnSound(World world, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName);
            for (Player player : world.getPlayers()) {
                player.playSound(player.getLocation(), sound, 0.6f, 1.1f);
            }
        } catch (IllegalArgumentException e) {
            // Fallback sound
            for (Player player : world.getPlayers()) {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.1f);
            }
        }
    }

    /**
     * Finalizes the dawn animation - Fixed for Folia
     */
    private void finalizeDawnAnimation(World world) {
        // Thread-safe cleanup
        synchronized (animationLock) {
            stopDawnAnimation(world);
        }

        // Final world operations
        try {
            // Ensure final time is day
            world.setTime(1000L);

            // Clear weather if configured
            if (plugin.getConfigManager().shouldClearWeather()) {
                world.setStorm(false);
                world.setThundering(false);
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().warning("Error finalizing dawn animation world operations: " + e.getMessage());
            }
        }

        // Schedule completion effects on global thread
        plugin.getPlatformAdapter().runTaskLater(() -> {
            try {
                // Send completion title
                if (plugin.getConfigManager().isDawnTitleEnabled()) {
                    sendAnimationTitle(world, "dawn-animation-complete");
                }

                // Play final sound
                if (plugin.getConfigManager().areSoundsEnabled()) {
                    playDawnSound(world, plugin.getConfigManager().getDawnFinalSound());
                }

                // Send completion message
                plugin.getMessageManager().sendWorldMessage(world, "dawn-animation-complete");

                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Dawn animation completed for world: " + world.getName());
                }
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("Error finalizing dawn animation effects: " + e.getMessage());
                }
            }
        }, 5L);
    }

    /**
     * Calculates the shortest time distance between two times
     */
    private long calculateTimeDistance(long from, long to) {
        long directDistance = (to - from + 24000) % 24000;
        return Math.min(directDistance, 24000 - directDistance);
    }

    /**
     * Checks if it's night time
     */
    private boolean isNight(World world) {
        long time = world.getTime();
        return time >= 12542 && time <= 23459;
    }

    /**
     * Stops the dawn animation - Thread-safe for Folia
     */
    public void stopDawnAnimation(World world) {
        synchronized (animationLock) {
            BukkitTask task = activeAnimations.remove(world.getUID());
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }

            BossBar bossBar = animationBossBars.remove(world.getUID());
            if (bossBar != null) {
                bossBar.removeAll();
            }
        }
    }

    /**
     * Checks if a world is currently animating
     */
    public boolean isAnimating(World world) {
        synchronized (animationLock) {
            return activeAnimations.containsKey(world.getUID());
        }
    }

    /**
     * Cleans up all animations
     */
    public void cleanup() {
        synchronized (animationLock) {
            for (BukkitTask task : activeAnimations.values()) {
                if (!task.isCancelled()) {
                    task.cancel();
                }
            }

            for (BossBar bossBar : animationBossBars.values()) {
                bossBar.removeAll();
            }

            activeAnimations.clear();
            animationBossBars.clear();
        }
    }

    /**
     * Handles player joining during animation
     */
    public void handlePlayerJoin(Player player) {
        World world = player.getWorld();

        BossBar bossBar = animationBossBars.get(world.getUID());
        if (bossBar != null) {
            plugin.getPlatformAdapter().runTaskLater(() -> {
                if (bossBar != null && player.isOnline()) {
                    bossBar.addPlayer(player);
                }
            }, 1L);
        }
    }

    /**
     * Handles player leaving during animation
     */
    public void handlePlayerQuit(Player player) {
        // Boss bars automatically remove players when they disconnect
    }
}