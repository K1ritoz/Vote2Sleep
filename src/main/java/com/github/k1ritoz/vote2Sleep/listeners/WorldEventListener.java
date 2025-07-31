package com.github.k1ritoz.vote2Sleep.listeners;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldEventListener implements Listener {

    private final Vote2Sleep plugin;

    public WorldEventListener(Vote2Sleep plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        plugin.getLogger().info("World loaded: " + world.getName());

        // Initialize world data if needed
        if (plugin.getConfigManager().isWorldEnabled(world)) {
            plugin.getLogger().info("Vote2Sleep is enabled for world: " + world.getName());
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();

        // Clear any pending votes and tasks for this world
        plugin.getVoteManager().clearVotes(world);

        plugin.getLogger().info("World unloaded: " + world.getName());
    }
}