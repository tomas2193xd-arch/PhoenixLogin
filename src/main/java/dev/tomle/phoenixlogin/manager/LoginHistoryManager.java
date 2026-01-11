package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Gestiona el historial de logins de los jugadores
 */
public class LoginHistoryManager {

    private final PhoenixLogin plugin;
    private final String tablePrefix = "phoenixlogin_";

    public LoginHistoryManager(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicializa la tabla de historial
     */
    public void initialize() {
        createHistoryTable();
    }

    /**
     * Crea la tabla de historial si no existe
     */
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

    /**
     * Registra un intento de login en el historial
     */
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

    /**
     * Obtiene el historial de logins de un jugador
     */
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

    /**
     * Obtiene las últimas IPs usadas por un jugador
     */
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

    /**
     * Cuenta los intentos fallidos desde una IP en un período de tiempo
     */
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

    /**
     * Limpia entradas antiguas del historial (mantiene solo los últimos X días)
     */
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
     * Clase que representa una entrada del historial
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

        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            return sdf.format(new Date(timestamp));
        }

        public String getStatusColor() {
            return success ? "§a" : "§c";
        }

        public String getStatusSymbol() {
            return success ? "✓" : "✗";
        }
    }
}
