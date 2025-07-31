package com.github.k1ritoz.vote2Sleep.data;

import org.bukkit.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds voting data for a specific world
 */
public class WorldData {

    private final World world;
    private final Map<UUID, SleepVote> votes;
    private long lastSkipTime;

    public WorldData(World world) {
        this.world = world;
        this.votes = new ConcurrentHashMap<>();
        this.lastSkipTime = 0;
    }

    public void addVote(SleepVote vote) {
        votes.put(vote.getPlayerUUID(), vote);
    }

    public void removeVote(UUID playerUUID) {
        votes.remove(playerUUID);
    }

    public boolean hasPlayerVoted(UUID playerUUID) {
        return votes.containsKey(playerUUID);
    }

    public SleepVote getVote(UUID playerUUID) {
        return votes.get(playerUUID);
    }

    public Map<UUID, SleepVote> getVotes() {
        return new ConcurrentHashMap<>(votes);
    }

    public void clearVotes() {
        votes.clear();
    }

    // Getters/Setters
    public World getWorld() {
        return world;
    }

    public long getLastSkipTime() {
        return lastSkipTime;
    }

    public void setLastSkipTime(long lastSkipTime) {
        this.lastSkipTime = lastSkipTime;
    }

    public int getVoteCount() {
        return votes.size();
    }

    public boolean isEmpty() {
        return votes.isEmpty();
    }
}