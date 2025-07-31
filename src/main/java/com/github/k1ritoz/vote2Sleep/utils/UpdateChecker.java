package com.github.k1ritoz.vote2Sleep.utils;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private final Vote2Sleep plugin;
    private final String githubApiUrl;

    public UpdateChecker(Vote2Sleep plugin, String githubApiUrl) {
        this.plugin = plugin;
        this.githubApiUrl = githubApiUrl;
    }

    public void checkForUpdates() {
        plugin.getPlatformAdapter().runTaskAsync(() -> {
            try {
                String currentVersion = plugin.getDescription().getVersion();
                String latestVersion = getLatestVersion();

                if (latestVersion == null) {
                    plugin.getLogger().warning("Could not check for updates");
                    return;
                }

                if (!isNewerVersion(currentVersion, latestVersion)) {
                    plugin.getLogger().info("You are running the latest version!");
                    return;
                }

                plugin.getLogger().info("New version available: " + latestVersion);
                plugin.getLogger().info("You are running: " + currentVersion);
                plugin.getLogger().info("Download: https://github.com/k1ritoz/Vote2Sleep/releases/latest");

                // Notify online admins
                plugin.getPlatformAdapter().runTaskLater(() -> {
                    Bukkit.getOnlinePlayers().stream()
                            .filter(p -> p.hasPermission("vote2sleep.admin"))
                            .forEach(p -> {
                                p.sendMessage("§8[§bVote2Sleep§8] §eNew version available: §a" + latestVersion);
                                p.sendMessage("§8[§bVote2Sleep§8] §7Download at GitHub Releases");
                            });
                }, 20L);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
                if (plugin.getConfigManager().isDebugMode()) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String getLatestVersion() throws Exception {
        URL url = new URL(githubApiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        connection.setRequestProperty("User-Agent", "Vote2Sleep-UpdateChecker");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

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

    private boolean isNewerVersion(String current, String latest) {
        try {
            // Remove any non-version suffixes (like -SNAPSHOT)
            current = current.split("-")[0];
            latest = latest.split("-")[0];

            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");

            int maxLength = Math.max(currentParts.length, latestParts.length);

            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }

            return false; // Versions are equal
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Could not parse version numbers for comparison");
            return false;
        }
    }
}