package com.github.k1ritoz.vote2Sleep.listeners;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;

public class WeatherChangeListener implements Listener {

    private final Vote2Sleep plugin;

    public WeatherChangeListener(Vote2Sleep plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        World world = event.getWorld();

        if (!plugin.getConfigManager().isWorldEnabled(world)) {
            return;
        }

        // If weather clears and there are votes, check if we should cancel them
        if (!event.toWeatherState()) {
            handleWeatherClear(world);
        }
    }

    @EventHandler
    public void onThunderChange(ThunderChangeEvent event) {
        World world = event.getWorld();

        if (!plugin.getConfigManager().isWorldEnabled(world)) {
            return;
        }

        // If thunder stops and there are votes, check if we should cancel them
        if (!event.toThunderState()) {
            handleWeatherClear(world);
        }
    }

    private void handleWeatherClear(World world) {
        // If storm skip is not allowed during night, clear votes when weather clears
        boolean isNight = world.getTime() >= 12542 && world.getTime() <= 23459;

        if (!isNight && !plugin.getConfigManager().isStormSkipAllowed()) {
            plugin.getVoteManager().clearVotes(world);
            plugin.getMessageManager().sendWorldMessage(world, "weather-cleared-votes-cancelled");
        }
    }
}