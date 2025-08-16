package com.github.k1ritoz.vote2Sleep.platform;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public class FoliaAdapter implements PlatformAdapter {

    private final Vote2Sleep plugin = Vote2Sleep.getInstance();

    @Override
    public String getPlatformName() {
        return "Folia";
    }

    @Override
    public boolean isVersionSupported() {
        return getMinecraftVersion() >= 1210; // 1.21.0
    }

    @Override
    public BukkitTask runTaskLater(Runnable task, long delay) {
        try {
            Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Object scheduledTask = scheduler.getClass()
                    .getMethod("runDelayed", org.bukkit.plugin.Plugin.class,
                            java.util.function.Consumer.class, long.class)
                    .invoke(scheduler, plugin, (java.util.function.Consumer<Object>) (t) -> {
                        try {
                            task.run();
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error in Folia global task: " + e.getMessage());
                        }
                    }, delay);
            return new FoliaTaskWrapper(scheduledTask);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule Folia global task, falling back to Bukkit scheduler: " + e.getMessage());
            return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    @Override
    public BukkitTask runTaskTimer(Runnable task, long delay, long period) {
        try {
            Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Object scheduledTask = scheduler.getClass()
                    .getMethod("runAtFixedRate", org.bukkit.plugin.Plugin.class,
                            java.util.function.Consumer.class, long.class, long.class)
                    .invoke(scheduler, plugin, (java.util.function.Consumer<Object>) (t) -> {
                        try {
                            task.run();
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error in Folia global timer task: " + e.getMessage());
                        }
                    }, delay, period);
            return new FoliaTaskWrapper(scheduledTask);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule Folia global timer task, falling back to Bukkit scheduler: " + e.getMessage());
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }

    @Override
    public BukkitTask runTaskAsync(Runnable task) {
        try {
            Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            Object scheduledTask = scheduler.getClass()
                    .getMethod("runNow", org.bukkit.plugin.Plugin.class,
                            java.util.function.Consumer.class)
                    .invoke(scheduler, plugin, (java.util.function.Consumer<Object>) (t) -> {
                        try {
                            task.run();
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error in Folia async task: " + e.getMessage());
                        }
                    });
            return new FoliaTaskWrapper(scheduledTask);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule Folia async task, falling back to Bukkit scheduler: " + e.getMessage());
            return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    @Override
    public BukkitTask runTaskLaterForWorld(World world, Consumer<World> task, long delay) {
        // IMPORTANT: For operations like setTime(), use GLOBAL scheduler in Folia
        // World operations like time and weather must be executed on global thread
        return runTaskLater(() -> task.accept(world), delay);
    }

    @Override
    public BukkitTask runTaskTimerForWorld(World world, Consumer<World> task, long delay, long period) {
        // IMPORTANT: For operations like setTime(), use GLOBAL scheduler in Folia
        return runTaskTimer(() -> task.accept(world), delay, period);
    }

    /**
     * For operations that really need the region thread (like spawning entities at specific locations)
     */
    public BukkitTask runTaskLaterForWorldRegion(World world, Consumer<World> task, long delay) {
        try {
            Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);

            // Use spawn location for scheduling
            int chunkX = world.getSpawnLocation().getBlockX() >> 4;
            int chunkZ = world.getSpawnLocation().getBlockZ() >> 4;

            Object scheduledTask = scheduler.getClass()
                    .getMethod("runDelayed", org.bukkit.plugin.Plugin.class,
                            org.bukkit.World.class, int.class, int.class,
                            java.util.function.Consumer.class, long.class)
                    .invoke(scheduler, plugin, world, chunkX, chunkZ,
                            (java.util.function.Consumer<Object>) (t) -> {
                                try {
                                    // Execute on the region thread
                                    task.accept(world);
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Error in Folia world region task: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }, delay);
            return new FoliaTaskWrapper(scheduledTask);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule Folia world region task, falling back to global scheduler: " + e.getMessage());
            e.printStackTrace();
            // Fallback to global scheduler
            return runTaskLater(() -> task.accept(world), delay);
        }
    }

    @Override
    public BukkitTask runTaskLaterForPlayer(Player player, Consumer<Player> task, long delay) {
        try {
            // Get entity scheduler from player
            Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
            Object scheduledTask = scheduler.getClass()
                    .getMethod("runDelayed", org.bukkit.plugin.Plugin.class,
                            java.util.function.Consumer.class, Runnable.class, long.class)
                    .invoke(scheduler, plugin,
                            (java.util.function.Consumer<Object>) (t) -> {
                                try {
                                    if (player.isOnline()) { // Safety check
                                        task.accept(player);
                                    }
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Error in Folia player task: " + e.getMessage());
                                }
                            }, null, delay);
            return new FoliaTaskWrapper(scheduledTask);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule Folia player task for " + player.getName() + ", falling back to global scheduler: " + e.getMessage());
            // Fallback to global scheduler
            return runTaskLater(() -> {
                if (player.isOnline()) {
                    task.accept(player);
                }
            }, delay);
        }
    }

    @Override
    public double[] getTPS() {
        return new double[]{20.0, 20.0, 20.0}; // Folia doesn't have global TPS
    }

    @Override
    public boolean isAsyncSafe() {
        return false; // Folia requires specific thread handling
    }

    /**
     * Specific method for effects that need the region thread
     * (like spawning entities at specific coordinates)
     */
    public BukkitTask runRegionSpecificEffects(World world, Consumer<World> effectsTask, long delay) {
        return runTaskLaterForWorldRegion(world, effectsTask, delay);
    }

    private int getMinecraftVersion() {
        String version = Bukkit.getBukkitVersion();
        String[] parts = version.split("-")[0].split("\\.");
        return Integer.parseInt(parts[1]) * 100 + (parts.length > 2 ? Integer.parseInt(parts[2]) : 0);
    }

    /**
     * Enhanced wrapper class for better error handling and compatibility
     */
    private static class FoliaTaskWrapper implements BukkitTask {
        private final Object scheduledTask;
        private volatile boolean cancelled = false;

        public FoliaTaskWrapper(Object scheduledTask) {
            this.scheduledTask = scheduledTask;
        }

        @Override
        public int getTaskId() {
            return scheduledTask.hashCode();
        }

        @Override
        public org.bukkit.plugin.Plugin getOwner() {
            try {
                Method method = findMethod(scheduledTask.getClass(), "getOwningPlugin");
                if (method != null) {
                    method.setAccessible(true);
                    return (org.bukkit.plugin.Plugin) method.invoke(scheduledTask);
                }
            } catch (Exception e) {
                // Ignore and return plugin instance
            }
            return Vote2Sleep.getInstance();
        }

        @Override
        public boolean isSync() {
            return true;
        }

        @Override
        public boolean isCancelled() {
            if (cancelled) return true;

            try {
                Method method = findMethod(scheduledTask.getClass(), "isCancelled");
                if (method != null) {
                    method.setAccessible(true);
                    return (Boolean) method.invoke(scheduledTask);
                }
            } catch (Exception e) {
                // If we can't check, use our internal state
            }
            return cancelled;
        }

        @Override
        public void cancel() {
            cancelled = true;

            try {
                Method method = findMethod(scheduledTask.getClass(), "cancel");
                if (method != null) {
                    method.setAccessible(true);
                    method.invoke(scheduledTask);
                }
            } catch (Exception e) {
                // Log but don't throw - we've marked it as cancelled
                Vote2Sleep.getInstance().getLogger().warning("Could not cancel Folia task: " + e.getMessage());
            }
        }

        private Method findMethod(Class<?> clazz, String methodName) {
            try {
                return clazz.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                try {
                    return clazz.getDeclaredMethod(methodName);
                } catch (NoSuchMethodException ex) {
                    Class<?> superClass = clazz.getSuperclass();
                    if (superClass != null && superClass != Object.class) {
                        return findMethod(superClass, methodName);
                    }

                    for (Class<?> iface : clazz.getInterfaces()) {
                        try {
                            return findMethod(iface, methodName);
                        } catch (Exception ignored) {
                            // Continue
                        }
                    }
                }
            }
            return null;
        }
    }
}