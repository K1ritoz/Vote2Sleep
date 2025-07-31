package com.github.k1ritoz.vote2Sleep.api.events;

import com.github.k1ritoz.vote2Sleep.data.SleepVote;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SleepVoteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final World world;
    private final SleepVote vote;
    private final int totalVotes;

    public SleepVoteEvent(Player player, World world, SleepVote vote, int totalVotes) {
        this.player = player;
        this.world = world;
        this.vote = vote;
        this.totalVotes = totalVotes;
    }

    public Player getPlayer() { return player; }
    public World getWorld() { return world; }
    public SleepVote getVote() { return vote; }
    public int getTotalVotes() { return totalVotes; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}