package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.model.PlayerData;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final PhoenixLogin plugin;
    private final Map<UUID, PlayerData> activeSessions;
    private final String tablePrefix = "phoenixlogin_";

    public SessionManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
    }

    public PlayerData getPlayerData(UUID uuid) {
        return activeSessions.get(uuid);
    }

    public PlayerData getPlayerData(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public void createSession(Player player, PlayerData data) {
        activeSessions.put(player.getUniqueId(), data);
    }

    public void removeSession(UUID uuid) {
        activeSessions.remove(uuid);
    }

    public void removeSession(Player player) {
        activeSessions.remove(player.getUniqueId());
    }

    public boolean isAuthenticated(Player player) {
        PlayerData data = getPlayerData(player);
        return data != null && data.isAuthenticated();
    }

    public boolean isRegistered(Player player) {
        PlayerData data = getPlayerData(player);
        return data != null && data.isRegistered();
    }

    public void setAuthenticated(Player player, boolean authenticated) {
        PlayerData data = getPlayerData(player);
        if (data != null) {
            data.setAuthenticated(authenticated);

            if (authenticated && plugin.getConfigManager().isSessionsEnabled()) {
                saveSessionToDatabase(player, data);
            }
        }
    }

    public boolean checkExistingSession(Player player, PlayerData data) {
        if (!plugin.getConfigManager().isSessionsEnabled()) {
            return false;
        }

        if (!plugin.getConfigManager().isRememberIP()) {
            return false;
        }

        String currentIP = player.getAddress().getAddress().getHostAddress();

        try (Connection conn = plugin.getDatabaseManager().getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM " + tablePrefix + "sessions WHERE player_name = ?")) {

            ps.setString(1, player.getName());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String savedIP = rs.getString("ip_address");
                long expiry = rs.getLong("session_expiry");

                // Verificar si la IP coincide y la sesión no ha expirado
                if (savedIP.equals(currentIP) && System.currentTimeMillis() < expiry) {
                    data.setSessionExpiry(expiry);
                    return true;
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking session for: " + player.getName());
            e.printStackTrace();
        }

        return false;
    }

    private void saveSessionToDatabase(Player player, PlayerData data) {
        String ip = player.getAddress().getAddress().getHostAddress();
        long expiry = System.currentTimeMillis() + (plugin.getConfigManager().getSessionDuration() * 1000L);
        data.setSessionExpiry(expiry);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // Primero intentamos actualizar
            PreparedStatement updatePs = conn.prepareStatement(
                    "UPDATE " + tablePrefix + "sessions SET ip_address = ?, session_expiry = ? WHERE player_name = ?");
            updatePs.setString(1, ip);
            updatePs.setLong(2, expiry);
            updatePs.setString(3, player.getName());

            int updated = updatePs.executeUpdate();

            // Si no se actualizó ninguna fila, insertamos una nueva
            if (updated == 0) {
                PreparedStatement insertPs = conn.prepareStatement(
                        "INSERT INTO " + tablePrefix
                                + "sessions (player_name, ip_address, session_expiry) VALUES (?, ?, ?)");
                insertPs.setString(1, player.getName());
                insertPs.setString(2, ip);
                insertPs.setLong(3, expiry);
                insertPs.executeUpdate();
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving session for: " + player.getName());
            e.printStackTrace();
        }
    }

    public void clearAllSessions() {
        activeSessions.clear();
    }

    public int getActiveSessionsCount() {
        return activeSessions.size();
    }

    public int getAuthenticatedCount() {
        return (int) activeSessions.values().stream()
                .filter(PlayerData::isAuthenticated)
                .count();
    }
}
