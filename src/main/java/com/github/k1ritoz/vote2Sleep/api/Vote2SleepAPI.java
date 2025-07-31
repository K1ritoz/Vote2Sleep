package com.github.k1ritoz.vote2Sleep.api;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import com.github.k1ritoz.vote2Sleep.data.SleepVote;
import com.github.k1ritoz.vote2Sleep.data.WorldData;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class Vote2SleepAPI {

    private final Vote2Sleep plugin;

    public Vote2SleepAPI(Vote2Sleep plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if a player has voted for sleep in their current world
     */
    public boolean hasPlayerVoted(Player player) {
        return plugin.getVoteManager().hasPlayerVoted(player);
    }

    /**
     * Get the current number of votes in a world
     */
    public int getCurrentVotes(World world) {
        return plugin.getVoteManager().getCurrentVotes(world);
    }

    /**
     * Get the required number of votes for a world
     */
    public int getRequiredVotes(World world) {
        return plugin.getVoteManager().getRequiredVotes(world);
    }

    /**
     * Check if Vote2Sleep is enabled in a world
     */
    public boolean isWorldEnabled(World world) {
        return plugin.getConfigManager().isWorldEnabled(world);
    }

    /**
     * Force start a sleep vote for a player
     */
    public void startSleepVote(Player player) {
        plugin.getVoteManager().startSleepVote(player);
    }

    /**
     * Remove a player's sleep vote
     */
    public void removeSleepVote(Player player) {
        plugin.getVoteManager().removeSleepVote(player);
    }

    /**
     * Get all current votes for a world
     */
    public Map<String, SleepVote> getWorldVotes(World world) {
        WorldData data = plugin.getVoteManager().getWorldData(world);
        if (data == null) return Map.of();

        return data.getVotes().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getValue().getPlayerName(),
                        Map.Entry::getValue
                ));
    }

    /**
     * Force skip night/storm in a world
     */
    public void forceSkip(World world, Player initiator) {
        plugin.getVoteManager().forceSkip(world, initiator);
    }

    /**
     * Clear all votes in a world
     */
    public void clearVotes(World world) {
        plugin.getVoteManager().clearVotes(world);
    }

    /**
     * Check if sleeping is currently possible in a world
     */
    public boolean canSleep(World world) {
        boolean isNight = world.getTime() >= 12542 && world.getTime() <= 23459;
        boolean isStormy = world.hasStorm() || world.isThundering();

        return (isNight && plugin.getConfigManager().isNightSkipAllowed()) ||
                (isStormy && plugin.getConfigManager().isStormSkipAllowed());
    }

    /**
     * Get the sleep vote progress as a percentage (0.0 to 1.0)
     */
    public double getVoteProgress(World world) {
        int current = getCurrentVotes(world);
        int required = getRequiredVotes(world);

        return required > 0 ? (double) current / required : 0.0;
    }

    /**
     * Get eligible player count for a world (players who can vote)
     */
    public int getEligiblePlayerCount(World world) {
        return (int) world.getPlayers().stream()
                .filter(p -> p.getGameMode() != org.bukkit.GameMode.SPECTATOR)
                .filter(p -> p.getGameMode() != org.bukkit.GameMode.CREATIVE)
                .filter(p -> !p.hasPermission("vote2sleep.exempt"))
                .count();
    }

    /**
     * Check if a player is exempt from voting
     */
    public boolean isPlayerExempt(Player player) {
        // Check game mode exemptions
        String gameMode = player.getGameMode().name();
        if (plugin.getConfigManager().getExemptGameModes().contains(gameMode)) {
            return true;
        }

        // Check permission exemptions
        for (String permission : plugin.getConfigManager().getExemptPermissions()) {
            if (player.hasPermission(permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get world data for a specific world
     */
    public WorldData getWorldData(World world) {
        return plugin.getVoteManager().getWorldData(world);
    }
}