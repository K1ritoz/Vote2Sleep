package com.github.k1ritoz.vote2Sleep.managers;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import com.github.k1ritoz.vote2Sleep.data.WorldData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Vote2SleepPlaceholders extends PlaceholderExpansion {

    private final Vote2Sleep plugin;

    public Vote2SleepPlaceholders(Vote2Sleep plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "vote2sleep";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Keep expansion registered even if PlaceholderAPI reloads
    }

    @Override
    public boolean canRegister() {
        return plugin != null; // Only register if plugin is available
    }

    @Override
    @Nullable
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        // Handle requests without player (for global placeholders)
        if (player == null) {
            return handleGlobalPlaceholder(params);
        }

        World world = player.getWorld();

        // Check if plugin is still enabled
        if (!plugin.isEnabled()) {
            return null;
        }

        // Check if vote manager is available
        if (plugin.getVoteManager() == null) {
            return null;
        }

        try {
            switch (params.toLowerCase()) {
                case "current_votes":
                    return String.valueOf(plugin.getVoteManager().getCurrentVotes(world));

                case "required_votes":
                    return String.valueOf(plugin.getVoteManager().getRequiredVotes(world));

                case "remaining_votes":
                    int current = plugin.getVoteManager().getCurrentVotes(world);
                    int required = plugin.getVoteManager().getRequiredVotes(world);
                    return String.valueOf(Math.max(0, required - current));

                case "has_voted":
                    return String.valueOf(plugin.getVoteManager().hasPlayerVoted(player));

                case "has_voted_yes_no":
                    return plugin.getVoteManager().hasPlayerVoted(player) ? "Yes" : "No";

                case "world_enabled":
                    return String.valueOf(plugin.getConfigManager().isWorldEnabled(world));

                case "world_enabled_yes_no":
                    return plugin.getConfigManager().isWorldEnabled(world) ? "Yes" : "No";

                case "can_sleep":
                    return String.valueOf(canSleep(world));

                case "can_sleep_yes_no":
                    return canSleep(world) ? "Yes" : "No";

                case "is_night":
                    return String.valueOf(isNight(world));

                case "is_night_yes_no":
                    return isNight(world) ? "Yes" : "No";

                case "is_stormy":
                    return String.valueOf(world.hasStorm() || world.isThundering());

                case "is_stormy_yes_no":
                    return (world.hasStorm() || world.isThundering()) ? "Yes" : "No";

                case "progress_percentage":
                    int currentVotes = plugin.getVoteManager().getCurrentVotes(world);
                    int requiredVotes = plugin.getVoteManager().getRequiredVotes(world);
                    return String.valueOf(requiredVotes > 0 ? Math.round((currentVotes * 100.0) / requiredVotes) : 0);

                case "progress_decimal":
                    int currentVotesDecimal = plugin.getVoteManager().getCurrentVotes(world);
                    int requiredVotesDecimal = plugin.getVoteManager().getRequiredVotes(world);
                    return String.format("%.2f", requiredVotesDecimal > 0 ? (currentVotesDecimal / (double) requiredVotesDecimal) : 0.0);

                case "world_name":
                    return world.getName();

                case "vote_percentage_setting":
                    double percentage = plugin.getConfigManager().getVotePercentage(world);
                    return String.valueOf(Math.round(percentage * 100));

                case "eligible_players":
                    return String.valueOf(getEligiblePlayerCount(world));

                case "total_players":
                    return String.valueOf(world.getPlayers().size());

                case "time_formatted":
                    return formatTime(world.getTime());

                // World time placeholders
                case "world_time":
                    return String.valueOf(world.getTime());

                case "world_time_12h":
                    return formatTime12Hour(world.getTime());

                case "world_time_24h":
                    return formatTime24Hour(world.getTime());

                default:
                    return null; // Placeholder not found
            }
        } catch (Exception e) {
            // Log error in debug mode only to avoid spam
            if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().warning("Error processing placeholder '" + params + "': " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Handle global placeholders that don't require a player
     */
    @Nullable
    private String handleGlobalPlaceholder(String params) {
        try {
            switch (params.toLowerCase()) {
                case "plugin_version":
                    return plugin.getDescription().getVersion();

                case "platform":
                    return plugin.getPlatformAdapter() != null ? plugin.getPlatformAdapter().getPlatformName() : "Unknown";

                case "enabled_worlds_count":
                    return String.valueOf(plugin.getConfigManager().getEnabledWorlds().size());

                case "database_enabled":
                    return String.valueOf(plugin.getConfigManager().isDatabaseEnabled());

                case "database_enabled_yes_no":
                    return plugin.getConfigManager().isDatabaseEnabled() ? "Yes" : "No";

                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    // Utility methods
    private boolean canSleep(World world) {
        boolean isNight = isNight(world);
        boolean isStormy = world.hasStorm() || world.isThundering();

        return (isNight && plugin.getConfigManager().isNightSkipAllowed()) ||
                (isStormy && plugin.getConfigManager().isStormSkipAllowed());
    }

    private boolean isNight(World world) {
        long time = world.getTime();
        return time >= 12542 && time <= 23459;
    }

    private int getEligiblePlayerCount(World world) {
        return (int) world.getPlayers().stream()
                .filter(p -> !isPlayerExempt(p))
                .count();
    }

    private boolean isPlayerExempt(Player player) {
        try {
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
        } catch (Exception e) {
            // If error checking exemptions, assume not exempt
        }

        return false;
    }

    private String formatTime(long ticks) {
        // Convert Minecraft ticks to hours and minutes
        long totalMinutes = (ticks + 6000) / 1000 * 60 / 60; // Offset by 6000 to start at 6:00 AM
        totalMinutes = totalMinutes % (24 * 60); // Keep within 24 hours

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        return String.format("%02d:%02d", hours, minutes);
    }

    private String formatTime12Hour(long ticks) {
        long totalMinutes = (ticks + 6000) / 1000 * 60 / 60;
        totalMinutes = totalMinutes % (24 * 60);

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        String amPm = hours < 12 ? "AM" : "PM";
        hours = hours == 0 ? 12 : (hours > 12 ? hours - 12 : hours);

        return String.format("%d:%02d %s", hours, minutes, amPm);
    }

    private String formatTime24Hour(long ticks) {
        long totalMinutes = (ticks + 6000) / 1000 * 60 / 60;
        totalMinutes = totalMinutes % (24 * 60);

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        return String.format("%02d:%02d", hours, minutes);
    }
}