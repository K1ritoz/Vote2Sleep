package com.github.k1ritoz.vote2Sleep.platform;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public class PurpurAdapter implements PlatformAdapter {

    private final Vote2Sleep plugin = Vote2Sleep.getInstance();

    @Override
    public String getPlatformName() { return "Purpur"; }

    @Override
    public boolean isVersionSupported() {
        return getMinecraftVersion() >= 1210;
    }

    @Override
    public BukkitTask runTaskLater(Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }

    @Override
    public BukkitTask runTaskTimer(Runnable task, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
    }

    @Override
    public BukkitTask runTaskAsync(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public BukkitTask runTaskLaterForWorld(World world, Consumer<World> task, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, () -> task.accept(world), delay);
    }

    @Override
    public BukkitTask runTaskTimerForWorld(World world, Consumer<World> task, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimer(plugin, () -> task.accept(world), delay, period);
    }

    @Override
    public BukkitTask runTaskLaterForPlayer(Player player, Consumer<Player> task, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, () -> task.accept(player), delay);
    }

    @Override
    public double[] getTPS() {
        try {
            // Try Purpur's enhanced TPS method first
            return getPurpurTPS();
        } catch (Exception e) {
            // Fallback to Paper's TPS
            try {
                return Bukkit.getTPS();
            } catch (Exception ex) {
                return new double[]{20.0, 20.0, 20.0};
            }
        }
    }

    @Override
    public boolean isAsyncSafe() {
        return true; // Bukkit/Spigot/Paper/Purpur is generally safe for async tasks
    }

    // Purpur-specific methods

    /**
     * Get Purpur's enhanced TPS information
     */
    private double[] getPurpurTPS() throws Exception {
        try {
            // Try to access Purpur's enhanced server class
            Class<?> purpurServerClass = Class.forName("org.purpurmc.purpur.PurpurServer");
            Method getTpsMethod = purpurServerClass.getMethod("getTPS");
            Object tpsResult = getTpsMethod.invoke(null);

            if (tpsResult instanceof double[]) {
                return (double[]) tpsResult;
            }
        } catch (ClassNotFoundException e) {
            // Not Purpur or method doesn't exist, try Paper method
            return Bukkit.getTPS();
        }

        return new double[]{20.0, 20.0, 20.0};
    }

    /**
     * Use Purpur's optimized player operations if available
     */
    public void optimizePlayerOperation(Player player, Runnable operation) {
        try {
            // Check if Purpur's player optimization is available
            Class<?> purpurPlayerClass = Class.forName("org.purpurmc.purpur.entity.PurpurPlayer");
            if (purpurPlayerClass.isInstance(player)) {
                // Use Purpur's optimized methods if available
                Method optimizeMethod = purpurPlayerClass.getMethod("runOptimizedOperation", Runnable.class);
                optimizeMethod.invoke(player, operation);
                return;
            }
        } catch (Exception e) {
            // Fallback to normal operation
        }

        // Standard operation
        operation.run();
    }

    /**
     * Check if Purpur's enhanced world operations are available
     */
    public boolean hasPurpurWorldOptimizations(World world) {
        try {
            Class<?> purpurWorldClass = Class.forName("org.purpurmc.purpur.world.PurpurWorld");
            return purpurWorldClass.isInstance(world);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Use Purpur's optimized world time setting if available
     */
    public void setWorldTimeOptimized(World world, long time) {
        try {
            if (hasPurpurWorldOptimizations(world)) {
                Class<?> purpurWorldClass = Class.forName("org.purpurmc.purpur.world.PurpurWorld");
                Method optimizedSetTime = purpurWorldClass.getMethod("setTimeOptimized", long.class);
                optimizedSetTime.invoke(world, time);
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to use Purpur optimized time setting: " + e.getMessage());
        }

        // Fallback to standard method
        world.setTime(time);
    }

    /**
     * Use Purpur's optimized weather clearing if available
     */
    public void clearWeatherOptimized(World world) {
        try {
            if (hasPurpurWorldOptimizations(world)) {
                Class<?> purpurWorldClass = Class.forName("org.purpurmc.purpur.world.PurpurWorld");
                Method optimizedClearWeather = purpurWorldClass.getMethod("clearWeatherOptimized");
                optimizedClearWeather.invoke(world);
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to use Purpur optimized weather clearing: " + e.getMessage());
        }

        // Fallback to standard methods
        world.setStorm(false);
        world.setThundering(false);
    }

    /**
     * Check if Purpur's async chunk loading optimization is available
     */
    public boolean hasAsyncChunkOptimizations() {
        try {
            Class.forName("org.purpurmc.purpur.chunk.AsyncChunkLoader");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private int getMinecraftVersion() {
        try {
            String version = Bukkit.getBukkitVersion();
            String[] parts = version.split("-")[0].split("\\.");
            return Integer.parseInt(parts[1]) * 100 + (parts.length > 2 ? Integer.parseInt(parts[2]) : 0);
        } catch (Exception e) {
            return 1210;
        }
    }
}