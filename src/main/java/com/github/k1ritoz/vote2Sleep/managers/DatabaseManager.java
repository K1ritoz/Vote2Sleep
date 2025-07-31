package com.github.k1ritoz.vote2Sleep.managers;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import com.github.k1ritoz.vote2Sleep.data.SleepVote;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class DatabaseManager {

    private final Vote2Sleep plugin;
    private Connection connection;
    private boolean enabled;

    public DatabaseManager(Vote2Sleep plugin) {
        this.plugin = plugin;

        // Safe check for database enabled status
        try {
            this.enabled = plugin.getConfigManager() != null && plugin.getConfigManager().isDatabaseEnabled();
        } catch (Exception e) {
            plugin.getLogger().warning("Could not check database configuration, disabling database: " + e.getMessage());
            this.enabled = false;
        }

        if (enabled) {
            initializeDatabase();
        } else {
            plugin.getLogger().info("Database is disabled in configuration");
        }
    }

    private void initializeDatabase() {
        String dbType;
        try {
            dbType = plugin.getConfigManager().getDatabaseType();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to get database type from config: " + e.getMessage());
            this.enabled = false;
            return;
        }

        try {
            if ("SQLITE".equalsIgnoreCase(dbType)) {
                setupSQLite();
            } else if ("MYSQL".equalsIgnoreCase(dbType)) {
                setupMySQL();
            } else {
                plugin.getLogger().warning("Unknown database type: " + dbType + ", falling back to SQLite");
                setupSQLite();
            }

            createTables();
            plugin.getLogger().info("Database connection established (" + dbType + ")");

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            this.enabled = false;
        }
    }

    private void setupSQLite() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String url = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/vote2sleep.db";
        connection = DriverManager.getConnection(url);
    }

    private void setupMySQL() throws SQLException {
        // MySQL configuration would go here
        // For now, fallback to SQLite
        plugin.getLogger().info("MySQL support not yet implemented, falling back to SQLite");
        setupSQLite();
    }

    private void createTables() throws SQLException {
        String createSkipEventsTable = """
            CREATE TABLE IF NOT EXISTS skip_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                world_name TEXT NOT NULL,
                world_uuid TEXT NOT NULL,
                skip_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                voter_count INTEGER NOT NULL,
                skip_type TEXT NOT NULL
            )
        """;

        String createVotesTable = """
            CREATE TABLE IF NOT EXISTS votes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                skip_event_id INTEGER,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                vote_time TIMESTAMP NOT NULL,
                location_x REAL,
                location_y REAL,
                location_z REAL,
                FOREIGN KEY (skip_event_id) REFERENCES skip_events (id)
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSkipEventsTable);
            stmt.execute(createVotesTable);
        }
    }

    public void saveSkipEvent(World world, List<SleepVote> votes) {
        if (!enabled || connection == null) {
            return;
        }

        try {
            // Insert skip event
            String insertSkipEvent = """
                INSERT INTO skip_events (world_name, world_uuid, voter_count, skip_type)
                VALUES (?, ?, ?, ?)
            """;

            int skipEventId;
            try (PreparedStatement stmt = connection.prepareStatement(insertSkipEvent, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, world.getName());
                stmt.setString(2, world.getUID().toString());
                stmt.setInt(3, votes.size());
                stmt.setString(4, determineSkipType(world));

                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        skipEventId = keys.getInt(1);
                    } else {
                        plugin.getLogger().warning("Failed to get generated key for skip event");
                        return;
                    }
                }
            }

            // Insert individual votes
            String insertVote = """
                INSERT INTO votes (skip_event_id, player_uuid, player_name, vote_time, location_x, location_y, location_z)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

            try (PreparedStatement stmt = connection.prepareStatement(insertVote)) {
                for (SleepVote vote : votes) {
                    stmt.setInt(1, skipEventId);
                    stmt.setString(2, vote.getPlayerUUID().toString());
                    stmt.setString(3, vote.getPlayerName());
                    stmt.setTimestamp(4, Timestamp.valueOf(vote.getTimestamp()));
                    stmt.setDouble(5, vote.getLocation().getX());
                    stmt.setDouble(6, vote.getLocation().getY());
                    stmt.setDouble(7, vote.getLocation().getZ());

                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            plugin.getLogger().info("Saved skip event with " + votes.size() + " votes to database");

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save skip event to database: " + e.getMessage());
        }
    }

    private String determineSkipType(World world) {
        boolean isNight = world.getTime() >= 12542 && world.getTime() <= 23459;
        boolean isStormy = world.hasStorm() || world.isThundering();

        if (isNight && isStormy) return "NIGHT_STORM";
        if (isNight) return "NIGHT";
        if (isStormy) return "STORM";
        return "UNKNOWN";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed");
            } catch (SQLException e) {
                plugin.getLogger().warning("Error closing database connection: " + e.getMessage());
            }
        }
    }
}