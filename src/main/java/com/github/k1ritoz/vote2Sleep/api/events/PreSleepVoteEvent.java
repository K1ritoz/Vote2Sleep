package com.github.k1ritoz.vote2Sleep.api.events;

import com.github.k1ritoz.vote2Sleep.data.SleepVote;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PreSleepVoteEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final World world;
    private final SleepVote vote;

    public PreSleepVoteEvent(Player player, World world, SleepVote vote) {
        this.player = player;
        this.world = world;
        this.vote = vote;
    }

    public Player getPlayer() { return player; }
    public World getWorld() { return world; }
    public SleepVote getVote() { return vote; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}