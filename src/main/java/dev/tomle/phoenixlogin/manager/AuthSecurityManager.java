package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthSecurityManager {

    private final PhoenixLogin plugin;
    private final Map<UUID, FailedAttempts> failedAttempts;
    private final Map<UUID, Long> lockedAccounts;

    public AuthSecurityManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        this.failedAttempts = new ConcurrentHashMap<>();
        this.lockedAccounts = new ConcurrentHashMap<>();
    }

    public boolean isAccountLocked(Player player) {
        Long lockExpiry = lockedAccounts.get(player.getUniqueId());

        if (lockExpiry == null) {
            return false;
        }

        if (System.currentTimeMillis() >= lockExpiry) {
            lockedAccounts.remove(player.getUniqueId());
            return false;
        }

        return true;
    }

    public long getLockoutRemainingTime(Player player) {
        Long lockExpiry = lockedAccounts.get(player.getUniqueId());
        if (lockExpiry == null) {
            return 0;
        }

        long remaining = (lockExpiry - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public void recordFailedAttempt(Player player) {
        FailedAttempts attempts = failedAttempts.computeIfAbsent(
                player.getUniqueId(),
                k -> new FailedAttempts());

        attempts.increment();

        int maxAttempts = plugin.getConfigManager().getMaxLoginAttempts();

        if (attempts.getCount() >= maxAttempts) {
            lockAccount(player);
        }

        // Log del intento fallido
        String ip = player.getAddress().getAddress().getHostAddress();
        plugin.getDatabaseManager().logLoginAttemptAsync(player.getName(), ip, false);
    }

    public void recordSuccessfulAttempt(Player player) {
        // Limpiar intentos fallidos
        failedAttempts.remove(player.getUniqueId());
        lockedAccounts.remove(player.getUniqueId());

        // Log del intento exitoso
        String ip = player.getAddress().getAddress().getHostAddress();
        plugin.getDatabaseManager().logLoginAttemptAsync(player.getName(), ip, true);
    }

    public int getRemainingAttempts(Player player) {
        FailedAttempts attempts = failedAttempts.get(player.getUniqueId());
        if (attempts == null) {
            return plugin.getConfigManager().getMaxLoginAttempts();
        }

        int max = plugin.getConfigManager().getMaxLoginAttempts();
        return Math.max(0, max - attempts.getCount());
    }

    private void lockAccount(Player player) {
        int lockoutDuration = plugin.getConfigManager().getLockoutDuration();
        long lockExpiry = System.currentTimeMillis() + (lockoutDuration * 1000L);
        lockedAccounts.put(player.getUniqueId(), lockExpiry);

        plugin.getLogger().warning("Account locked for " + player.getName() + " due to too many failed attempts.");
    }

    public void resetAttempts(Player player) {
        failedAttempts.remove(player.getUniqueId());
        lockedAccounts.remove(player.getUniqueId());
    }

    public boolean validatePassword(String password) {
        int minLength = plugin.getConfigManager().getMinPasswordLength();
        int maxLength = plugin.getConfigManager().getMaxPasswordLength();

        if (password.length() < minLength || password.length() > maxLength) {
            return false;
        }

        if (plugin.getConfigManager().isPasswordRequireUppercase()) {
            if (!password.matches(".*[A-Z].*")) {
                return false;
            }
        }

        if (plugin.getConfigManager().isPasswordRequireNumbers()) {
            if (!password.matches(".*[0-9].*")) {
                return false;
            }
        }

        if (plugin.getConfigManager().isPasswordRequireSpecial()) {
            if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
                return false;
            }
        }

        return true;
    }

    public void cleanup(UUID uuid) {
        failedAttempts.remove(uuid);
        lockedAccounts.remove(uuid);
    }

    // Inner class para rastrear intentos fallidos
    private static class FailedAttempts {
        private int count;
        private long lastAttempt;

        public FailedAttempts() {
            this.count = 0;
            this.lastAttempt = System.currentTimeMillis();
        }

        public void increment() {
            this.count++;
            this.lastAttempt = System.currentTimeMillis();
        }

        public int getCount() {
            return count;
        }
    }
}
