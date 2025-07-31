package com.github.k1ritoz.vote2Sleep.data;

import org.bukkit.Location;

import java.time.LocalDateTime;
import java.util.UUID;

public class SleepVote {

    private final UUID playerUUID;
    private final String playerName;
    private final LocalDateTime timestamp;
    private final Location location;

    public SleepVote(UUID playerUUID, String playerName, LocalDateTime timestamp, Location location) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.timestamp = timestamp;
        this.location = location.clone();
    }

    // Getters
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Location getLocation() { return location.clone(); }
}