package com.github.k1ritoz.vote2Sleep.platform;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

public interface PlatformAdapter {

    String getPlatformName();

    boolean isVersionSupported();

    // Scheduling methods for cross-platform compatibility
    BukkitTask runTaskLater(Runnable task, long delay);
    BukkitTask runTaskTimer(Runnable task, long delay, long period);
    BukkitTask runTaskAsync(Runnable task);

    // World-specific scheduling (important for Folia)
    BukkitTask runTaskLaterForWorld(World world, Consumer<World> task, long delay);
    BukkitTask runTaskTimerForWorld(World world, Consumer<World> task, long delay, long period);

    // Player-specific scheduling (important for Folia)
    BukkitTask runTaskLaterForPlayer(Player player, Consumer<Player> task, long delay);

    // Get server TPS (if available)
    double[] getTPS();

    // Check if async operations are safe
    boolean isAsyncSafe();
}