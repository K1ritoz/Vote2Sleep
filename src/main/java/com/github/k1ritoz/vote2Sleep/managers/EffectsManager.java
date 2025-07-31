package com.github.k1ritoz.vote2Sleep.managers;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import com.github.k1ritoz.vote2Sleep.data.SleepVote;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.List;

public class EffectsManager {

    private final Vote2Sleep plugin;

    public EffectsManager(Vote2Sleep plugin) {
        this.plugin = plugin;
    }

    public void playVoteEffects(World world, Player voter) {
        // Sound effect
        if (plugin.getConfigManager().areSoundsEnabled()) {
            playSound(world, plugin.getConfigManager().getVoteSound());
        }

        // Particle effect around voter
        if (voter.isOnline()) {
            Location loc = voter.getLocation();
            world.spawnParticle(Particle.HAPPY_VILLAGER, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
        }

        // Action bar for all players
        if (plugin.getConfigManager().isActionBarEnabled()) {
            for (Player player : world.getPlayers()) {
                plugin.getMessageManager().sendActionBar(player, "vote-cast-actionbar",
                        java.util.Map.of("player", voter.getName()));
            }
        }
    }

    public void playSkipEffects(World world, List<SleepVote> votes) {
        // Sound effect
        if (plugin.getConfigManager().areSoundsEnabled()) {
            playSound(world, plugin.getConfigManager().getSkipSound());
        }

        // Title effect
        if (plugin.getConfigManager().areTitlesEnabled()) {
            String title = plugin.getMessageManager().getMessage("skip-title");
            String subtitle = plugin.getMessageManager().getMessage("skip-subtitle");

            for (Player player : world.getPlayers()) {
                player.sendTitle(title, subtitle, 10, 70, 20);
            }
        }

        // Particle effects at voting locations
        for (SleepVote vote : votes) {
            Location loc = vote.getLocation();
            if (loc.getWorld().equals(world)) {
                world.spawnParticle(Particle.END_ROD, loc.add(0, 2, 0), 20, 1, 1, 1, 0.1);
            }
        }

        // Lightning effect (visual only)
        for (Player player : world.getPlayers()) {
            if (Math.random() < 0.3) { // 30% chance per player
                Location loc = player.getLocation();
                world.strikeLightningEffect(loc);
            }
        }
    }

    private void playSound(World world, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName);
            for (Player player : world.getPlayers()) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name: " + soundName);
            // Fallback to default sound
            for (Player player : world.getPlayers()) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }
    }

    public void cleanup() {
        // Nothing to cleanup for now
    }
}