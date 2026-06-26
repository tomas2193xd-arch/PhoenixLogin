package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Anti-bot protection using kick-and-rejoin verification.
 *
 * Flow:
 * 1. Player connects with an unverified IP -> kick with "reconnect to verify"
 * 2. Player reconnects within the verification window -> IP verified, allowed
 * in
 * 3. Verified IPs are stored in the database with expiration (7-14 days)
 *
 * Triggers:
 * - New IP (never connected before)
 * - IP change (different from last verified IP)
 * - Verification expired
 *
 * Additional layers:
 * - Per-IP rate limiting (prevents rapid reconnection spam)
 * - Global attack detection (surge of connections = attack mode)
 * - Auto-blacklist for repeat offenders
 * - Username validation
 */
public class AntiBotManager {

    private final PhoenixLogin plugin;

    // Pending verifications: IP -> kick timestamp
    private final Map<String, Long> pendingVerifications = new ConcurrentHashMap<>();

    // Per-IP connection tracking for rate limiting
    private final Map<String, java.util.Deque<Long>> connectionLog = new ConcurrentHashMap<>();

    // Global join tracking — ConcurrentLinkedDeque instead of
    // CopyOnWriteArrayList
    private final java.util.Deque<Long> globalJoinTimestamps = new java.util.concurrent.ConcurrentLinkedDeque<>();

    // Temporary blacklist
    private final Map<String, Long> blacklistedIPs = new ConcurrentHashMap<>();

    // Violation counter per IP
    private final Map<String, AtomicInteger> violationCount = new ConcurrentHashMap<>();

    // Attack mode
    private final AtomicBoolean attackMode = new AtomicBoolean(false);
    private volatile long attackModeExpiry = 0;
    private volatile long lastAttackNotification = 0;

    // Stats
    private int totalBlocked = 0;
    private int totalVerified = 0;
    private int totalAttacksDetected = 0;

    // Verification window: how long a player has to reconnect (ms)
    private static final long VERIFICATION_WINDOW = 60000; // 60 seconds

    public AntiBotManager(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the anti-bot system: create DB table and start cleanup task.
     */
    public void initialize() {
        if (!isEnabled())
            return;

        createTable();

        // Cleanup task every 30 seconds
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::cleanup, 600L, 600L);

        plugin.getLogger().info("AntiBot protection active.");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("antibot.enabled", true);
    }

    // =========================================================================
    // DATABASE — Verified IPs
    // =========================================================================

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS phoenixlogin_verified_ips (" +
                "ip_address VARCHAR(45) NOT NULL," +
                "player_name VARCHAR(16) NOT NULL," +
                "verified_at BIGINT NOT NULL," +
                "expires_at BIGINT NOT NULL," +
                "PRIMARY KEY (ip_address, player_name)" +
                ")";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("AntiBot: Failed to create verified_ips table!");
            e.printStackTrace();
        }
    }

    private boolean isIPVerified(String ip, String playerName) {
        String sql = "SELECT expires_at FROM phoenixlogin_verified_ips " +
                "WHERE ip_address = ? AND player_name = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ip);
            ps.setString(2, playerName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                long expiresAt = rs.getLong("expires_at");
                if (System.currentTimeMillis() < expiresAt) {
                    return true; // Verified and not expired
                }
                // Expired — clean up
                removeVerifiedIP(ip, playerName);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("AntiBot: Error checking verified IP");
        }
        return false;
    }

    private void saveVerifiedIP(String ip, String playerName) {
        int expirationDays = plugin.getConfig().getInt("antibot.verification.expiration-days", 14);
        long now = System.currentTimeMillis();
        long expiresAt = now + (expirationDays * 86400000L);

        String sql;
        if (plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("MYSQL")) {
            sql = "INSERT INTO phoenixlogin_verified_ips (ip_address, player_name, verified_at, expires_at) " +
                    "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE verified_at = ?, expires_at = ?";
        } else {
            sql = "INSERT OR REPLACE INTO phoenixlogin_verified_ips (ip_address, player_name, verified_at, expires_at) "
                    +
                    "VALUES (?, ?, ?, ?)";
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ip);
            ps.setString(2, playerName);
            ps.setLong(3, now);
            ps.setLong(4, expiresAt);

            if (plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("MYSQL")) {
                ps.setLong(5, now);
                ps.setLong(6, expiresAt);
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("AntiBot: Error saving verified IP");
        }
    }

    private void removeVerifiedIP(String ip, String playerName) {
        String sql = "DELETE FROM phoenixlogin_verified_ips WHERE ip_address = ? AND player_name = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.setString(2, playerName);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    // =========================================================================
    // MAIN EVALUATION — Called from AsyncPlayerPreLoginEvent
    // =========================================================================

    /**
     * Evaluates an incoming connection.
     *
     * @return null if allowed, or a kick message if denied.
     */
    public String evaluateConnection(String playerName, InetAddress address) {
        if (!isEnabled())
            return null;

        String ip = address.getHostAddress();

        // Layer 1: Blacklist
        if (isBlacklisted(ip)) {
            totalBlocked++;
            return getKickMessage("antibot.blacklisted");
        }

        // Layer 2: Rate limit
        if (isRateLimited(ip)) {
            recordViolation(ip);
            totalBlocked++;
            int cooldown = getRateLimitWindow();
            return getKickMessage("antibot.rate-limited")
                    .replace("{time}", String.valueOf(cooldown));
        }

        // Layer 3: Name validation
        if (isNameValidationEnabled()) {
            String nameResult = validateName(playerName);
            if (nameResult != null) {
                recordViolation(ip);
                totalBlocked++;
                return nameResult;
            }
        }

        // Layer 4: Kick-and-rejoin verification
        if (isIPVerified(ip, playerName)) {
            // IP is verified and not expired — allow through
            recordConnection(ip);
            checkForAttack();
            return null;
        }

        // Check if this is a reconnection (player was kicked for verification)
        if (isPendingVerification(ip, playerName)) {
            // Player reconnected within window — VERIFIED!
            completePendingVerification(ip, playerName);
            saveVerifiedIP(ip, playerName);
            totalVerified++;
            recordConnection(ip);
            checkForAttack();
            plugin.getLogger().info("AntiBot: Verified " + playerName + " [" + maskIP(ip) + "]");
            return null;
        }

        // Not verified, not pending — kick for verification
        startVerification(ip, playerName);
        totalBlocked++;

        // Build kick message
        String kickMsg = getKickMessage("antibot.verification-kick");
        int expirationDays = plugin.getConfig().getInt("antibot.verification.expiration-days", 14);
        kickMsg = kickMsg.replace("{days}", String.valueOf(expirationDays));

        return kickMsg;
    }

    // =========================================================================
    // KICK-AND-REJOIN VERIFICATION
    // =========================================================================

    private String getVerificationKey(String ip, String playerName) {
        return ip + ":" + playerName.toLowerCase();
    }

    private void startVerification(String ip, String playerName) {
        String key = getVerificationKey(ip, playerName);
        pendingVerifications.put(key, System.currentTimeMillis());
    }

    private boolean isPendingVerification(String ip, String playerName) {
        String key = getVerificationKey(ip, playerName);
        Long kickTime = pendingVerifications.get(key);

        if (kickTime == null)
            return false;

        // Check if within the verification window
        long elapsed = System.currentTimeMillis() - kickTime;
        if (elapsed <= VERIFICATION_WINDOW) {
            return true;
        }

        // Expired
        pendingVerifications.remove(key);
        return false;
    }

    private void completePendingVerification(String ip, String playerName) {
        String key = getVerificationKey(ip, playerName);
        pendingVerifications.remove(key);
    }

    // =========================================================================
    // RATE LIMITING
    // =========================================================================

    private boolean isRateLimited(String ip) {
        int maxConnections = attackMode.get()
                ? getAttackModeMaxPerIP()
                : plugin.getConfig().getInt("antibot.rate-limit.max-connections-per-ip", 3);

        return getRecentConnectionCount(ip) >= maxConnections;
    }

    // Simple loop instead of stream for GC efficiency during attacks
    private int getRecentConnectionCount(String ip) {
        java.util.Deque<Long> timestamps = connectionLog.get(ip);
        if (timestamps == null)
            return 0;

        long windowStart = System.currentTimeMillis() - (getRateLimitWindow() * 1000L);
        int count = 0;
        for (Long t : timestamps) {
            if (t > windowStart)
                count++;
        }
        return count;
    }

    private int getRateLimitWindow() {
        return plugin.getConfig().getInt("antibot.rate-limit.time-window", 60);
    }

    private void recordConnection(String ip) {
        connectionLog.computeIfAbsent(ip, k -> new java.util.concurrent.ConcurrentLinkedDeque<>())
                .add(System.currentTimeMillis());
        globalJoinTimestamps.add(System.currentTimeMillis());
    }

    // =========================================================================
    // BLACKLIST
    // =========================================================================

    private boolean isBlacklisted(String ip) {
        Long expiry = blacklistedIPs.get(ip);
        if (expiry == null)
            return false;
        if (System.currentTimeMillis() >= expiry) {
            blacklistedIPs.remove(ip);
            return false;
        }
        return true;
    }

    private void blacklistIP(String ip) {
        int duration = plugin.getConfig().getInt("antibot.blacklist.duration", 3600);
        long expiry = System.currentTimeMillis() + (duration * 1000L);
        blacklistedIPs.put(ip, expiry);
        plugin.getLogger().warning("AntiBot: Blacklisted " + maskIP(ip) + " for " + duration + "s");
    }

    // =========================================================================
    // NAME VALIDATION
    // =========================================================================

    private boolean isNameValidationEnabled() {
        return plugin.getConfig().getBoolean("antibot.name-validation.enabled", true);
    }

    private String validateName(String name) {
        int minLen = plugin.getConfig().getInt("antibot.name-validation.min-length", 3);
        int maxLen = plugin.getConfig().getInt("antibot.name-validation.max-length", 16);

        if (name.length() < minLen || name.length() > maxLen) {
            return getKickMessage("antibot.invalid-name");
        }

        if (!name.matches("^[a-zA-Z0-9_]+$")) {
            return getKickMessage("antibot.invalid-name");
        }

        if (plugin.getConfig().getBoolean("antibot.name-validation.block-random-names", true)) {
            if (looksLikeBotName(name)) {
                return getKickMessage("antibot.invalid-name");
            }
        }

        return null;
    }

    /**
     * Heuristic: detects names that look auto-generated.
     */
    private boolean looksLikeBotName(String name) {
        if (name.matches(".*\\d{5,}$"))
            return true;
        if (name.matches("^(.)\\1{4,}$"))
            return true;

        // Simple char loop instead of stream for zero GC overhead
        if (name.length() >= 8) {
            String lower = name.toLowerCase();
            int vowels = 0;
            for (int i = 0; i < lower.length(); i++) {
                char c = lower.charAt(i);
                if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') {
                    vowels++;
                }
            }
            double vowelRatio = (double) vowels / name.length();
            if (vowelRatio < 0.1)
                return true;
        }

        return false;
    }

    // =========================================================================
    // ATTACK DETECTION
    // =========================================================================

    private void checkForAttack() {
        if (!plugin.getConfig().getBoolean("antibot.attack-detection.enabled", true))
            return;

        int maxJoins = plugin.getConfig().getInt("antibot.attack-detection.max-joins-per-minute", 15);
        long oneMinuteAgo = System.currentTimeMillis() - 60000;

        // Simple count instead of stream
        int recentJoins = 0;
        for (Long t : globalJoinTimestamps) {
            if (t > oneMinuteAgo)
                recentJoins++;
        }

        if (recentJoins >= maxJoins && !attackMode.get()) {
            activateAttackMode();
        }
    }

    private void activateAttackMode() {
        int duration = plugin.getConfig().getInt("antibot.attack-detection.attack-duration", 120);
        attackMode.set(true);
        attackModeExpiry = System.currentTimeMillis() + (duration * 1000L);
        totalAttacksDetected++;

        plugin.getLogger().warning("AntiBot: ATTACK DETECTED — Protection active for " + duration + "s");

        long now = System.currentTimeMillis();
        if (now - lastAttackNotification > 30000) {
            lastAttackNotification = now;
            notifyAdmins(true);
        }
    }

    private void deactivateAttackMode() {
        if (!attackMode.get())
            return;
        attackMode.set(false);
        plugin.getLogger().info("AntiBot: Attack mode ended.");
        notifyAdmins(false);
    }

    private void notifyAdmins(boolean started) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            MessageManager msg = plugin.getMessageManager();
            String key = started ? "antibot.attack-started" : "antibot.attack-ended";
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("phoenixlogin.admin")) {
                    msg.sendMessage(player, key);
                }
            }
        });
    }

    private int getAttackModeMaxPerIP() {
        return plugin.getConfig().getInt("antibot.attack-mode.max-connections-per-ip", 1);
    }

    // =========================================================================
    // VIOLATIONS
    // =========================================================================

    private void recordViolation(String ip) {
        AtomicInteger count = violationCount.computeIfAbsent(ip, k -> new AtomicInteger(0));
        int violations = count.incrementAndGet();

        int threshold = plugin.getConfig().getInt("antibot.blacklist.threshold", 5);
        if (violations >= threshold && plugin.getConfig().getBoolean("antibot.blacklist.enabled", true)) {
            blacklistIP(ip);
        }
    }

    // =========================================================================
    // CLEANUP
    // =========================================================================

    private void cleanup() {
        long now = System.currentTimeMillis();

        // Check attack mode expiry
        if (attackMode.get() && now >= attackModeExpiry) {
            deactivateAttackMode();
        }

        // Clean expired pending verifications
        pendingVerifications.entrySet().removeIf(e -> now - e.getValue() > VERIFICATION_WINDOW);

        // Clean old connection logs (keep last 5 minutes)
        long fiveMinAgo = now - 300000;
        connectionLog.forEach((ip, timestamps) -> {
            timestamps.removeIf(t -> t < fiveMinAgo);
            if (timestamps.isEmpty())
                connectionLog.remove(ip);
        });

        // Clean old global timestamps
        globalJoinTimestamps.removeIf(t -> t < fiveMinAgo);

        // Clean expired blacklist
        blacklistedIPs.entrySet().removeIf(e -> now >= e.getValue());

        // Reset violations if too many accumulated
        if (violationCount.size() > 200)
            violationCount.clear();
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    private String maskIP(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".*.*";
        }
        return ip.substring(0, Math.min(ip.length(), 8)) + "...";
    }

    private String getKickMessage(String path) {
        return plugin.getMessageManager().getMessage(path);
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    public boolean isAttackMode() {
        return attackMode.get();
    }

    public int getTotalBlocked() {
        return totalBlocked;
    }

    public int getTotalVerified() {
        return totalVerified;
    }

    public int getTotalAttacksDetected() {
        return totalAttacksDetected;
    }

    public int getBlacklistedCount() {
        return blacklistedIPs.size();
    }

    public void shutdown() {
        connectionLog.clear();
        globalJoinTimestamps.clear();
        blacklistedIPs.clear();
        violationCount.clear();
        pendingVerifications.clear();
    }
}
