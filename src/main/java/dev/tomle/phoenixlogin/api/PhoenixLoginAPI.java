package dev.tomle.phoenixlogin.api;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.model.PlayerData;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Main API class for PhoenixLogin.
 * 
 * @author TomLe
 * @version 1.2.0
 * @since 1.2.0
 */
public class PhoenixLoginAPI {

    private static PhoenixLoginAPI instance;
    private final PhoenixLogin plugin;

    private PhoenixLoginAPI(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    public static void initialize(PhoenixLogin plugin) {
        if (instance != null) {
            throw new IllegalStateException("PhoenixLoginAPI is already initialized");
        }
        instance = new PhoenixLoginAPI(plugin);
    }

    public static PhoenixLoginAPI getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PhoenixLoginAPI is not initialized. Is PhoenixLogin loaded?");
        }
        return instance;
    }

    public boolean isAuthenticated(Player player) {
        if (player == null) {
            throw new NullPointerException("Player cannot be null");
        }
        return plugin.getSessionManager().isAuthenticated(player);
    }

    public CompletableFuture<Boolean> isRegisteredAsync(String playerName) {
        if (playerName == null) {
            throw new NullPointerException("Player name cannot be null");
        }
        return plugin.getDatabaseManager().loadPlayerDataAsync(playerName)
                .thenApply(data -> data != null && data.isRegistered());
    }

    public CompletableFuture<Optional<PlayerData>> getPlayerDataAsync(String playerName) {
        if (playerName == null) {
            throw new NullPointerException("Player name cannot be null");
        }
        return plugin.getDatabaseManager().loadPlayerDataAsync(playerName)
                .thenApply(Optional::ofNullable);
    }

    public void forceAuthenticate(Player player) {
        if (player == null) {
            throw new NullPointerException("Player cannot be null");
        }
        plugin.getSessionManager().setAuthenticated(player, true);
    }

    public void forceLogout(Player player) {
        if (player == null) {
            throw new NullPointerException("Player cannot be null");
        }
        plugin.getSessionManager().setAuthenticated(player, false);
    }

    public boolean areSessionsEnabled() {
        return plugin.getConfigManager().isSessionsEnabled();
    }

    public int getSessionDuration() {
        return plugin.getConfigManager().getSessionDuration();
    }

    public int getMaxLoginAttempts() {
        return plugin.getConfigManager().getMaxLoginAttempts();
    }

    public int getLockoutDuration() {
        return plugin.getConfigManager().getLockoutDuration();
    }

    public PhoenixLogin getPlugin() {
        return plugin;
    }

    public String getAPIVersion() {
        return "1.2.0";
    }
}
