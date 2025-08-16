package com.github.k1ritoz.vote2Sleep.platform;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

public class BukkitAdapter implements PlatformAdapter {

    private final Vote2Sleep plugin = Vote2Sleep.getInstance();

    @Override
    public String getPlatformName() {
        String serverName = Bukkit.getName();
        if (serverName.toLowerCase().contains("spigot")) {
            return "Spigot";
        } else if (serverName.toLowerCase().contains("craftbukkit")) {
            return "CraftBukkit";
        }
        return "Bukkit";
    }

    @Override
    public boolean isVersionSupported() {
        return getMinecraftVersion() >= 1210; // 1.21.0
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
            // Try to get TPS if available (newer Spigot/Paper versions)
            return new double[]{
                    (double) Bukkit.getServer().getClass().getMethod("getTPS").invoke(Bukkit.getServer(), new Object[0])
            };
        } catch (Exception e) {
            // Fallback to default TPS values
            return new double[]{20.0, 20.0, 20.0};
        }
    }

    @Override
    public boolean isAsyncSafe() {
        return true; // Bukkit/Spigot/Paper/Purpur is generally safe for async tasks
    }

    private int getMinecraftVersion() {
        String version = Bukkit.getBukkitVersion();
        String[] parts = version.split("-")[0].split("\\.");
        return Integer.parseInt(parts[1]) * 100 + (parts.length > 2 ? Integer.parseInt(parts[2]) : 0);
    }
}