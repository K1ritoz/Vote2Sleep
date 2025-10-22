package com.github.k1ritoz.vote2Sleep.listeners;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;

public class PlayerBedListener implements Listener {

    private final Vote2Sleep plugin;

    public PlayerBedListener(Vote2Sleep plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();

        // Check if bed interaction is enabled in config
        if (!plugin.getConfigManager().isBedInteractionEnabled()) {
            plugin.getMessageManager().sendMessage(player, "bed-interaction-disabled");
            return;
        }

        // For Folia compatibility - schedule task based on detected platform
        try {
            if (plugin.getPlatformAdapter().getPlatformName().equals("Folia")) {
                // Use player-specific scheduler for Folia
                plugin.getPlatformAdapter().runTaskLaterForPlayer(player, (p) -> {
                    if (p.isOnline() && p.isSleeping()) {
                        // Additional safety checks
                        plugin.getVoteManager().startSleepVoteFromBed(p);
                    }
                }, 2L); // Slightly longer delay for Folia
            } else {
                // Use standard scheduler for other platforms
                plugin.getPlatformAdapter().runTaskLaterForPlayer(player, (p) -> {
                    if (p.isOnline()) {
                        // Safety check
                        plugin.getVoteManager().startSleepVoteFromBed(p);
                    }
                }, 1L);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error scheduling sleep vote for player " + player.getName() + ": " + e.getMessage());

            // Fallback: try to start vote immediately
            try {
                plugin.getVoteManager().startSleepVoteFromBed(player);
            } catch (Exception fallbackError) {
                plugin.getLogger().severe("Failed to start sleep vote even with fallback: " + fallbackError.getMessage());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();

        try {
            plugin.getVoteManager().removeSleepVote(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing sleep vote for player " + player.getName() + ": " + e.getMessage());
        }
    }
}