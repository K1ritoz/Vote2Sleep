package com.github.k1ritoz.vote2Sleep.listeners;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PlayerConnectionListener implements Listener {

    private final Vote2Sleep plugin;

    public PlayerConnectionListener(Vote2Sleep plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getVoteManager().handlePlayerJoin(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getVoteManager().handlePlayerQuit(player);
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World fromWorld = event.getFrom();
        World toWorld = player.getWorld();

        plugin.getVoteManager().handlePlayerChangeWorld(player, fromWorld, toWorld);
    }
}