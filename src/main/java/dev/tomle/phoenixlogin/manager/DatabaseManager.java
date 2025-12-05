package dev.tomle.phoenixlogin.manager;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.model.PlayerData;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final PhoenixLogin plugin;
    private HikariDataSource dataSource;
    private final String tablePrefix = "phoenixlogin_";

    public DatabaseManager(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        ConfigManager config = plugin.getConfigManager();

        try {
            if (config.getDatabaseType().equals("MYSQL")) {
                setupMySQL();
            } else {
                setupSQLite();
            }

            createTables();
            plugin.getLogger().info("Database initialized successfully!");

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database!");
            e.printStackTrace();
        }
    }

    private void setupMySQL() {
        ConfigManager config = plugin.getConfigManager();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getMySQLHost() + ":" + config.getMySQLPort() + "/"
                + config.getMySQLDatabase());
        hikariConfig.setUsername(config.getMySQLUsername());
        hikariConfig.setPassword(config.getMySQLPassword());
        hikariConfig.setMaximumPoolSize(config.getMySQLPoolSize());

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

        this.dataSource = new HikariDataSource(hikariConfig);

        plugin.getLogger().info("MySQL connection pool established!");
    }

    private void setupSQLite() {
        File dbFile = new File(plugin.getDataFolder(), "database.db");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hikariConfig.setMaximumPoolSize(1);

        this.dataSource = new HikariDataSource(hikariConfig);

        plugin.getLogger().info("SQLite database file created at: " + dbFile.getAbsolutePath());
    }

    private void createTables() throws SQLException {
        String playersTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "players (" +
                "player_name VARCHAR(16) PRIMARY KEY," +
                "password_hash VARCHAR(60) NOT NULL," +
                "last_ip VARCHAR(45)," +
                "last_login BIGINT," +
                "registration_date BIGINT" +
                ")";

        String sessionsTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "sessions (" +
                "player_name VARCHAR(16) PRIMARY KEY," +
                "ip_address VARCHAR(45)," +
                "session_expiry BIGINT," +
                "FOREIGN KEY (player_name) REFERENCES " + tablePrefix + "players(player_name) ON DELETE CASCADE" +
                ")";

        String attemptsTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "login_attempts (" +
                "id INTEGER PRIMARY KEY "
                + (plugin.getConfigManager().getDatabaseType().equals("MYSQL") ? "AUTO_INCREMENT" : "AUTOINCREMENT")
                + "," +
                "player_name VARCHAR(16)," +
                "ip_address VARCHAR(45)," +
                "success BOOLEAN," +
                "timestamp BIGINT" +
                ")";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(playersTable);
            stmt.executeUpdate(sessionsTable);
            stmt.executeUpdate(attemptsTable);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized!");
        }
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection closed!");
        }
    }

    // === ASYNC OPERATIONS ===

    public CompletableFuture<PlayerData> loadPlayerDataAsync(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT * FROM " + tablePrefix + "players WHERE player_name = ?")) {

                ps.setString(1, playerName);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    return new PlayerData(
                            rs.getString("player_name"),
                            rs.getString("password_hash"),
                            rs.getString("last_ip"),
                            rs.getLong("last_login"),
                            rs.getLong("registration_date"));
                }

                return new PlayerData(playerName);

            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading player data for: " + playerName);
                e.printStackTrace();
                return new PlayerData(playerName);
            }
        });
    }

    public CompletableFuture<Boolean> registerPlayerAsync(String playerName, String password, String ip) {
        return CompletableFuture.supplyAsync(() -> {
            String passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
            long now = System.currentTimeMillis();

            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO " + tablePrefix
                                    + "players (player_name, password_hash, last_ip, last_login, registration_date) VALUES (?, ?, ?, ?, ?)")) {

                ps.setString(1, playerName);
                ps.setString(2, passwordHash);
                ps.setString(3, ip);
                ps.setLong(4, now);
                ps.setLong(5, now);

                ps.executeUpdate();
                return true;

            } catch (SQLException e) {
                plugin.getLogger().severe("Error registering player: " + playerName);
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> verifyPasswordAsync(String playerName, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT password_hash FROM " + tablePrefix + "players WHERE player_name = ?")) {

                ps.setString(1, playerName);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), storedHash);
                    return result.verified;
                }

                return false;

            } catch (SQLException e) {
                plugin.getLogger().severe("Error verifying password for: " + playerName);
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Void> updateLoginAsync(String playerName, String ip) {
        return CompletableFuture.runAsync(() -> {
            long now = System.currentTimeMillis();

            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE " + tablePrefix
                                    + "players SET last_ip = ?, last_login = ? WHERE player_name = ?")) {

                ps.setString(1, ip);
                ps.setLong(2, now);
                ps.setString(3, playerName);

                ps.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().severe("Error updating login for: " + playerName);
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Boolean> changePasswordAsync(String playerName, String newPassword) {
        return CompletableFuture.supplyAsync(() -> {
            String newHash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());

            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE " + tablePrefix + "players SET password_hash = ? WHERE player_name = ?")) {

                ps.setString(1, newHash);
                ps.setString(2, playerName);

                ps.executeUpdate();
                return true;

            } catch (SQLException e) {
                plugin.getLogger().severe("Error changing password for: " + playerName);
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> unregisterPlayerAsync(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM " + tablePrefix + "players WHERE player_name = ?")) {

                ps.setString(1, playerName);
                ps.executeUpdate();
                return true;

            } catch (SQLException e) {
                plugin.getLogger().severe("Error unregistering player: " + playerName);
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Void> logLoginAttemptAsync(String playerName, String ip, boolean success) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO " + tablePrefix
                                    + "login_attempts (player_name, ip_address, success, timestamp) VALUES (?, ?, ?, ?)")) {

                ps.setString(1, playerName);
                ps.setString(2, ip);
                ps.setBoolean(3, success);
                ps.setLong(4, System.currentTimeMillis());

                ps.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().severe("Error logging attempt for: " + playerName);
                e.printStackTrace();
            }
        });
    }
}
