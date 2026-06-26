package dev.tomle.phoenixlogin.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.entity.Player;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages premium (Mojang) authentication.
 *
 * Flow:
 * 1. Anti-bot check (before this)
 * 2. Player joins → check if username is a premium Mojang account
 * 3. If yes → compare player UUID with Mojang UUID
 * - UUID matches → player owns the account → auto-login
 * - UUID doesn't match → cracked player using premium name → password required
 * 4. If name is not premium → normal cracked flow (register/login)
 *
 * Additionally, players can type /premium after logging in to mark
 * themselves for auto-login on future connections.
 */
public class PremiumManager {

    private final PhoenixLogin plugin;

    // Cache Mojang API responses to avoid rate limiting (name -> MojangProfile)
    private final Map<String, MojangProfile> mojangCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheExpiry = new ConcurrentHashMap<>();

    private static final long CACHE_TTL = 600_000; // 10 minutes
    private static final String MOJANG_API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String TABLE_PREFIX = "phoenixlogin_";

    public PremiumManager(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the premium system — create DB column if needed.
     */
    public void initialize() {
        if (!isEnabled())
            return;

        addPremiumColumn();
        plugin.getLogger().info("Premium auto-login system active.");
    }

    public boolean isEnabled() {
        return plugin.getConfigManager().isPremiumEnabled();
    }

    public boolean isAutoLoginEnabled() {
        return plugin.getConfigManager().isPremiumAutoLogin();
    }

    // =========================================================================
    // MOJANG API
    // =========================================================================

    /**
     * Checks if a username belongs to a premium Mojang account.
     * Returns the MojangProfile (UUID + name) or null if not premium.
     * This is async and caches results.
     */
    public CompletableFuture<MojangProfile> lookupMojangProfile(String username) {
        // Check cache first
        String key = username.toLowerCase();
        MojangProfile cached = mojangCache.get(key);
        Long expiry = cacheExpiry.get(key);
        if (cached != null && expiry != null && System.currentTimeMillis() < expiry) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(MOJANG_API + username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "PhoenixLogin/" +
                        plugin.getDescription().getVersion());

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    InputStreamReader reader = new InputStreamReader(conn.getInputStream(),
                            StandardCharsets.UTF_8);
                    JsonElement element = JsonParser.parseReader(reader);
                    reader.close();

                    if (element.isJsonObject()) {
                        JsonObject json = element.getAsJsonObject();
                        String id = json.get("id").getAsString();
                        String name = json.get("name").getAsString();

                        UUID mojangUUID = fromTrimmed(id);
                        MojangProfile profile = new MojangProfile(name, mojangUUID);

                        // Cache it
                        mojangCache.put(key, profile);
                        cacheExpiry.put(key, System.currentTimeMillis() + CACHE_TTL);

                        return profile;
                    }
                } else if (responseCode == 204 || responseCode == 404) {
                    // Not a premium account — cache null result
                    mojangCache.put(key, MojangProfile.NOT_PREMIUM);
                    cacheExpiry.put(key, System.currentTimeMillis() + CACHE_TTL);
                    return MojangProfile.NOT_PREMIUM;
                }

                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Mojang API lookup failed for " + username + ": " + e.getMessage());
            }

            return null; // API error — don't cache, try again next time
        });
    }

    /**
     * Checks if a player's UUID matches the Mojang UUID for their name.
     * This proves they authenticated through Mojang (online-mode or BungeeCord).
     */
    public boolean isVerifiedPremium(Player player, MojangProfile profile) {
        if (profile == null || !profile.isPremium())
            return false;
        return player.getUniqueId().equals(profile.getUuid());
    }

    /**
     * Generates the offline UUID for a player name.
     * Used to check if a player's UUID is "cracked" (offline-mode).
     */
    public static UUID getOfflineUUID(String playerName) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Checks if a player is connecting with an offline/cracked UUID.
     */
    public boolean hasOfflineUUID(Player player) {
        UUID offlineUUID = getOfflineUUID(player.getName());
        return player.getUniqueId().equals(offlineUUID);
    }

    // =========================================================================
    // DATABASE — Premium status storage
    // =========================================================================

    /**
     * Adds the 'is_premium' column to the players table if it doesn't exist.
     */
    private void addPremiumColumn() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // Check if column exists by attempting to query it
            try {
                conn.createStatement().executeQuery(
                        "SELECT is_premium FROM " + TABLE_PREFIX + "players LIMIT 1").close();
            } catch (SQLException e) {
                // Column doesn't exist — add it
                String dbType = plugin.getConfigManager().getDatabaseType();
                String sql;
                if (dbType.equals("MYSQL")) {
                    sql = "ALTER TABLE " + TABLE_PREFIX + "players ADD COLUMN is_premium BOOLEAN DEFAULT FALSE";
                } else {
                    sql = "ALTER TABLE " + TABLE_PREFIX + "players ADD COLUMN is_premium INTEGER DEFAULT 0";
                }
                conn.createStatement().executeUpdate(sql);
                plugin.getLogger().info("Added premium column to players table.");
            }

            // Also add mojang_uuid column
            try {
                conn.createStatement().executeQuery(
                        "SELECT mojang_uuid FROM " + TABLE_PREFIX + "players LIMIT 1").close();
            } catch (SQLException e) {
                conn.createStatement().executeUpdate(
                        "ALTER TABLE " + TABLE_PREFIX + "players ADD COLUMN mojang_uuid VARCHAR(36) DEFAULT NULL");
                plugin.getLogger().info("Added mojang_uuid column to players table.");
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize premium columns.");
            e.printStackTrace();
        }
    }

    /**
     * Marks a player as premium in the database.
     */
    public CompletableFuture<Boolean> setPremiumStatusAsync(String playerName, boolean premium, UUID mojangUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE " + TABLE_PREFIX +
                                    "players SET is_premium = ?, mojang_uuid = ? WHERE player_name = ?")) {

                ps.setBoolean(1, premium);
                ps.setString(2, mojangUUID != null ? mojangUUID.toString() : null);
                ps.setString(3, playerName);

                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to set premium status for " + playerName);
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Checks if a player is marked as premium in the database.
     */
    public CompletableFuture<PremiumStatus> getPremiumStatusAsync(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT is_premium, mojang_uuid FROM " + TABLE_PREFIX +
                                    "players WHERE player_name = ?")) {

                ps.setString(1, playerName);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    boolean isPremium = rs.getBoolean("is_premium");
                    String uuidStr = rs.getString("mojang_uuid");
                    UUID mojangUUID = uuidStr != null ? UUID.fromString(uuidStr) : null;
                    return new PremiumStatus(isPremium, mojangUUID);
                }
            } catch (SQLException e) {
                // Column might not exist yet for new installs
                plugin.getLogger().fine("Premium status check failed for " + playerName);
            }
            return new PremiumStatus(false, null);
        });
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    /**
     * Converts a trimmed UUID string (no dashes) to a Java UUID.
     */
    private static UUID fromTrimmed(String trimmed) {
        if (trimmed.length() != 32)
            throw new IllegalArgumentException("Invalid UUID: " + trimmed);
        String formatted = trimmed.substring(0, 8) + "-" +
                trimmed.substring(8, 12) + "-" +
                trimmed.substring(12, 16) + "-" +
                trimmed.substring(16, 20) + "-" +
                trimmed.substring(20, 32);
        return UUID.fromString(formatted);
    }

    public void clearCache() {
        mojangCache.clear();
        cacheExpiry.clear();
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    /**
     * Represents a Mojang account profile.
     */
    public static class MojangProfile {
        public static final MojangProfile NOT_PREMIUM = new MojangProfile(null, null);

        private final String name;
        private final UUID uuid;

        public MojangProfile(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public UUID getUuid() {
            return uuid;
        }

        public boolean isPremium() {
            return name != null && uuid != null;
        }
    }

    /**
     * Represents a player's premium status from the database.
     */
    public static class PremiumStatus {
        private final boolean premium;
        private final UUID mojangUUID;

        public PremiumStatus(boolean premium, UUID mojangUUID) {
            this.premium = premium;
            this.mojangUUID = mojangUUID;
        }

        public boolean isPremium() {
            return premium;
        }

        public UUID getMojangUUID() {
            return mojangUUID;
        }
    }
}
