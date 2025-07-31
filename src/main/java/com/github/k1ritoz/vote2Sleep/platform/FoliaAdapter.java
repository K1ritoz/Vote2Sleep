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
        // Use region scheduler for world-specific operations
        try {
            Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
            Object scheduledTask = scheduler.getClass()
                    .getMethod("runDelayed", org.bukkit.plugin.Plugin.class,
                            org.bukkit.World.class, int.class, int.class,
                            java.util.function.Consumer.class, long.class)
                    .invoke(scheduler, plugin, world, 0, 0, // chunk coordinates (0,0 is safe for world operations)
                            (java.util.function.Consumer<Object>) (t) -> {
                                try {
                                    task.accept(world);
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Error in Folia world task: " + e.getMessage());
                                }
                            }, delay);
            return new FoliaTaskWrapper(scheduledTask);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule Folia world task, falling back to global scheduler: " + e.getMessage());
            // Fallback to global scheduler
            return runTaskLater(() -> task.accept(world), delay);
        }
    }

    @Override
    public BukkitTask runTaskTimerForWorld(World world, Consumer<World> task, long delay, long period) {
        // Use region scheduler for world-specific operations
        try {
            Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
            Object scheduledTask = scheduler.getClass()
                    .getMethod("runAtFixedRate", org.bukkit.plugin.Plugin.class,
                            org.bukkit.World.class, int.class, int.class,
                            java.util.function.Consumer.class, long.class, long.class)
                    .invoke(scheduler, plugin, world, 0, 0, // chunk coordinates
                            (java.util.function.Consumer<Object>) (t) -> {
                                try {
                                    task.accept(world);
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Error in Folia world timer task: " + e.getMessage());
                                }
                            }, delay, period);
            return new FoliaTaskWrapper(scheduledTask);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule Folia world timer task, falling back to global scheduler: " + e.getMessage());
            // Fallback to global scheduler
            return runTaskTimer(() -> task.accept(world), delay, period);
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
        return false; // Folia has strict threading
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