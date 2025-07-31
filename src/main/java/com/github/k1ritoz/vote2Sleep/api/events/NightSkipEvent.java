package com.github.k1ritoz.vote2Sleep.api.events;

import com.github.k1ritoz.vote2Sleep.data.SleepVote;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class NightSkipEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final World world;
    private final List<SleepVote> votes;

    public NightSkipEvent(World world, List<SleepVote> votes) {
        this.world = world;
        this.votes = votes;
    }

    public World getWorld() { return world; }
    public List<SleepVote> getVotes() { return votes; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}