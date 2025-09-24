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

        // Particle effect around voter (only if particle effects are enabled)
        if (plugin.getConfigManager().areParticleEffectsEnabled() && voter.isOnline()) {
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

    public void playSkipEffects(World world, List<SleepVote> votes, boolean wasNight) {
        // Sound effect
        if (plugin.getConfigManager().areSoundsEnabled()) {
            playSound(world, plugin.getConfigManager().getSkipSound());
        }

        // Title effect (only if title are enabled)
        if (plugin.getConfigManager().areTitlesEnabled()) {
            int fadeIn = plugin.getConfigManager().getTitleFadeIn();
            int stay = plugin.getConfigManager().getTitleStay();
            int fadeOut = plugin.getConfigManager().getTitleFadeOut();

            String title = wasNight ? plugin.getMessageManager().getMessage("skip-title")
                    : plugin.getMessageManager().getMessage("storm-skip-title");
            String subtitle = wasNight ? plugin.getMessageManager().getMessage("skip-subtitle")
                    : plugin.getMessageManager().getMessage("storm-skip-subtitle");

            for (Player player : world.getPlayers()) {
                player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            }
        }


        // Particle effects at voting locations (only if particle effects are enabled)
        if (plugin.getConfigManager().areParticleEffectsEnabled()) {
            for (SleepVote vote : votes) {
                Location loc = vote.getLocation();
                if (loc.getWorld().equals(world)) {
                    world.spawnParticle(Particle.END_ROD, loc.add(0, 2, 0), 20, 1, 1, 1, 0.1);
                }
            }
        }

        // Lightning effect (visual only)
        if (plugin.getConfigManager().isLightningOnSkipEnabled()) {
            double lightningChance = plugin.getConfigManager().getLightningChance();

            for (Player player : world.getPlayers()) {
                if (Math.random() < lightningChance) {
                    Location loc = player.getLocation();
                    world.strikeLightningEffect(loc);

                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("Lightning effect triggered for player " + player.getName() + " at " + loc);
                    }
                }
            }
        } else {
            // Optional: Send message explaining why lightning is disabled
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Lightning effects are disabled to prevent 'Surge Protector' achievement");
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