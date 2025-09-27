package com.github.k1ritoz.vote2Sleep.utils;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private final Vote2Sleep plugin;
    private final String githubApiUrl;

    // Task for periodic update checks
    private BukkitTask updateCheckTask;

    // Cache latest version to avoid spamming console
    private String cachedLatestVersion = null;
    private boolean hasNotifiedUpdate = false;

    // Check interval: 24 hours = 24 * 60 * 60 * 20 ticks = 1,728,000 ticks
    private static final long CHECK_INTERVAL = 24 * 60 * 60 * 20L;

    public UpdateChecker(Vote2Sleep plugin, String githubApiUrl) {
        this.plugin = plugin;
        this.githubApiUrl = githubApiUrl;
    }

    /**
     * Starts periodic update checking every 24 hours
     */
    public void startPeriodicChecking() {
        // Cancel existing task if running
        stopPeriodicChecking();

        // Run initial check immediately, then every 24 hours
        updateCheckTask = plugin.getPlatformAdapter().runTaskTimer(() -> {
            // Run the actual check in async to avoid blocking main thread
            plugin.getPlatformAdapter().runTaskAsync(() -> {
                checkForUpdatesInternal(false); // false = periodic check (silent if no updates)
            });
        }, 20L, CHECK_INTERVAL); // Start after 1 second, then every 24 hours

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Started periodic update checking (every 24 hours)");
        }
    }

    /**
     * Stops periodic update checking
     */
    public void stopPeriodicChecking() {
        if (updateCheckTask != null && !updateCheckTask.isCancelled()) {
            updateCheckTask.cancel();
            updateCheckTask = null;
        }
    }

    /**
     * Performs initial update check on server startup (always shows result)
     */
    public void checkForUpdates() {
        plugin.getPlatformAdapter().runTaskAsync(() -> {
            checkForUpdatesInternal(true); // true = startup check (always show result)
        });
    }

    /**
     * Forces an immediate update check (always shows result)
     */
    public void forceUpdateCheck() {
        plugin.getPlatformAdapter().runTaskAsync(() -> {
            // Reset notification flag to allow re-notification
            hasNotifiedUpdate = false;
            cachedLatestVersion = null;
            checkForUpdatesInternal(true); // true = forced check (always show result)
        });
    }

    /**
     * Internal method that handles the actual update checking logic
     * @param alwaysLog whether to log results even if no update is available
     */
    private void checkForUpdatesInternal(boolean alwaysLog) {
        try {
            String currentVersion = plugin.getDescription().getVersion();
            String latestVersion = getLatestVersion();

            if (latestVersion == null) {
                if (alwaysLog) {
                    plugin.getLogger().warning("Could not check for updates - GitHub API unavailable");
                }
                return;
            }

            // Cache the latest version
            cachedLatestVersion = latestVersion;

            if (!isNewerVersion(currentVersion, latestVersion)) {
                // No update available
                if (alwaysLog) {
                    plugin.getLogger().info("You are running the latest version (" + currentVersion + ")");
                }
                // Reset notification flag when up to date
                hasNotifiedUpdate = false;
                return;
            }

            // Update is available
            boolean shouldNotify = !hasNotifiedUpdate || alwaysLog;

            if (shouldNotify) {
                // Log to console only
                plugin.getLogger().info("═══════════════════════════════════");
                plugin.getLogger().info("    UPDATE AVAILABLE!");
                plugin.getLogger().info("    Current: " + currentVersion);
                plugin.getLogger().info("    Latest:  " + latestVersion);
                plugin.getLogger().info("    Download: https://github.com/k1ritoz/Vote2Sleep/releases/latest");
                plugin.getLogger().info("═══════════════════════════════════");

                // Mark as notified to prevent spam in periodic checks
                hasNotifiedUpdate = true;
            }

        } catch (Exception e) {
            if (alwaysLog) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().warning("Update check error details:");
                e.printStackTrace();
            }
        }
    }

    /**
     * Fetches the latest version from GitHub API
     * @return latest version string or null if failed
     */
    private String getLatestVersion() throws Exception {
        URL url = new URL(githubApiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        connection.setRequestProperty("User-Agent", "Vote2Sleep-UpdateChecker");
        connection.setConnectTimeout(8000); // Increased timeout
        connection.setReadTimeout(8000);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("GitHub API returned response code: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        // Parse JSON response to get tag_name
        String jsonResponse = response.toString();
        Pattern pattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(jsonResponse);

        if (matcher.find()) {
            String tagName = matcher.group(1);
            // Remove 'v' prefix if present (e.g., "v1.0.0" -> "1.0.0")
            return tagName.startsWith("v") ? tagName.substring(1) : tagName;
        }

        return null;
    }

    /**
     * Compares two version strings to determine if latest is newer than current
     * @param current current version string
     * @param latest latest version string
     * @return true if latest version is newer than current
     */
    private boolean isNewerVersion(String current, String latest) {
        try {
            // Remove any non-version suffixes (like -SNAPSHOT, -BETA, etc.)
            current = current.split("-")[0];
            latest = latest.split("-")[0];

            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");

            int maxLength = Math.max(currentParts.length, latestParts.length);

            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;

                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }

            return false; // Versions are equal
        } catch (NumberFormatException e) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().warning("Could not parse version numbers for comparison: current=" + current + ", latest=" + latest);
            }
            return false;
        }
    }

    /**
     * Parses a version part string to integer, handling edge cases
     * @param versionPart version part string
     * @return parsed integer
     */
    private int parseVersionPart(String versionPart) {
        // Remove any non-numeric characters (like 'a', 'b', 'rc', etc.)
        String numericPart = versionPart.replaceAll("[^0-9]", "");
        return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
    }

    /**
     * Gets the currently cached latest version (if any)
     * @return cached latest version or null
     */
    public String getCachedLatestVersion() {
        return cachedLatestVersion;
    }

    /**
     * Checks if an update notification has been sent
     * @return true if notification was already sent
     */
    public boolean hasNotifiedUpdate() {
        return hasNotifiedUpdate;
    }
}