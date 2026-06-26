package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manages login history entries.
 */
public class LoginHistoryManager {

    private final PhoenixLogin plugin;
    private final String tablePrefix = "phoenixlogin_";

    public LoginHistoryManager(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        createHistoryTable();
    }

    private void createHistoryTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "login_history (" +
                "id INTEGER PRIMARY KEY "
                + (plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("MYSQL") ? "AUTO_INCREMENT"
                        : "AUTOINCREMENT")
                + ", " +
                "player_name VARCHAR(16) NOT NULL, " +
                "ip_address VARCHAR(45) NOT NULL, " +
                "timestamp BIGINT NOT NULL, " +
                "success BOOLEAN NOT NULL, " +
                "login_method VARCHAR(20) DEFAULT 'password'" +
                ")";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating login history table: " + e.getMessage());
        }
    }

    public CompletableFuture<Void> logLoginAttempt(String playerName, String ipAddress, boolean success,
            String method) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO " + tablePrefix + "login_history " +
                    "(player_name, ip_address, timestamp, success, login_method) VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerName);
                stmt.setString(2, ipAddress);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.setBoolean(4, success);
                stmt.setString(5, method);
                stmt.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().severe("Error logging login attempt: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<List<LoginEntry>> getLoginHistory(String playerName, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<LoginEntry> history = new ArrayList<>();
            String sql = "SELECT * FROM " + tablePrefix + "login_history " +
                    "WHERE player_name = ? ORDER BY timestamp DESC LIMIT ?";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerName);
                stmt.setInt(2, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        history.add(new LoginEntry(
                                rs.getString("player_name"),
                                rs.getString("ip_address"),
                                rs.getLong("timestamp"),
                                rs.getBoolean("success"),
                                rs.getString("login_method")));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error fetching login history: " + e.getMessage());
            }

            return history;
        });
    }

    public CompletableFuture<List<String>> getRecentIPs(String playerName, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> ips = new ArrayList<>();
            String sql = "SELECT DISTINCT ip_address FROM " + tablePrefix + "login_history " +
                    "WHERE player_name = ? AND success = true ORDER BY timestamp DESC LIMIT ?";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerName);
                stmt.setInt(2, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ips.add(rs.getString("ip_address"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error fetching recent IPs: " + e.getMessage());
            }

            return ips;
        });
    }

    public CompletableFuture<Integer> countFailedAttempts(String ipAddress, long sinceTimestamp) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM " + tablePrefix + "login_history " +
                    "WHERE ip_address = ? AND success = false AND timestamp >= ?";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, ipAddress);
                stmt.setLong(2, sinceTimestamp);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error counting failed attempts: " + e.getMessage());
            }

            return 0;
        });
    }

    public CompletableFuture<Void> cleanupOldEntries(int daysToKeep) {
        return CompletableFuture.runAsync(() -> {
            long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
            String sql = "DELETE FROM " + tablePrefix + "login_history WHERE timestamp < ?";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, cutoffTime);
                stmt.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().severe("Error cleaning up old history: " + e.getMessage());
            }
        });
    }

    /**
     * Represents a single login history entry.
     */
    public static class LoginEntry {
        private final String playerName;
        private final String ipAddress;
        private final long timestamp;
        private final boolean success;
        private final String method;

        public LoginEntry(String playerName, String ipAddress, long timestamp, boolean success, String method) {
            this.playerName = playerName;
            this.ipAddress = ipAddress;
            this.timestamp = timestamp;
            this.success = success;
            this.method = method;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMethod() {
            return method;
        }

        private static final DateTimeFormatter DATE_FORMAT =
                DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

        public String getFormattedDate() {
            return DATE_FORMAT.format(Instant.ofEpochMilli(timestamp));
        }

        public String getStatusColor() {
            return success ? "§a" : "§c";
        }

        public String getStatusSymbol() {
            return success ? "✓" : "✗";
        }
    }
}
