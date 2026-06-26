package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.api.event.PlayerLoginEvent;
import dev.tomle.phoenixlogin.manager.MessageManager;
import dev.tomle.phoenixlogin.manager.PremiumManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /premium command — allows premium players to auto-login.
 *
 * When a premium player joins, they can type /premium instead of /login.
 * The plugin verifies their UUID against Mojang's API.
 * If the UUID matches → they own the account → auto-login.
 * If the UUID doesn't match → reject (cracked player using a premium name).
 *
 * Players already authenticated can use /premium to toggle premium auto-login
 * for future sessions.
 */
public class PremiumCommand implements CommandExecutor {

    private final PhoenixLogin plugin;

    public PremiumCommand(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
            return true;
        }

        Player player = (Player) sender;
        MessageManager msg = plugin.getMessageManager();
        PremiumManager premiumManager = plugin.getPremiumManager();

        if (!premiumManager.isEnabled()) {
            msg.sendMessage(player, "premium.disabled");
            return true;
        }

        // If player is already authenticated → toggle premium status for future logins
        if (plugin.getSessionManager().isAuthenticated(player)) {
            handlePremiumToggle(player, msg, premiumManager);
            return true;
        }

        // Player is NOT authenticated → try premium auto-login
        handlePremiumLogin(player, msg, premiumManager);
        return true;
    }

    /**
     * Handles premium auto-login for unauthenticated players.
     */
    private void handlePremiumLogin(Player player, MessageManager msg, PremiumManager premiumManager) {
        msg.sendMessage(player, "premium.verifying");

        premiumManager.lookupMojangProfile(player.getName()).thenAccept(profile -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline())
                    return;

                if (profile == null) {
                    // API error
                    msg.sendMessage(player, "premium.api-error");
                    plugin.getEffectsManager().playErrorSound(player);
                    return;
                }

                if (!profile.isPremium()) {
                    // Not a premium account
                    msg.sendMessage(player, "premium.not-premium");
                    plugin.getEffectsManager().playErrorSound(player);
                    return;
                }

                // Check if player's UUID matches the Mojang UUID
                if (premiumManager.isVerifiedPremium(player, profile)) {
                    // UUID matches → they own this Mojang account!
                    performPremiumLogin(player, msg, premiumManager, profile);
                } else {
                    // UUID doesn't match → cracked player or offline-mode server
                    if (premiumManager.hasOfflineUUID(player)) {
                        // Player has an offline UUID → server is in offline-mode
                        // This is expected for most cracked servers
                        msg.sendMessage(player, "premium.offline-mode");
                        msg.sendMessage(player, "premium.use-login");
                        plugin.getEffectsManager().playErrorSound(player);
                    } else {
                        // UUID exists but doesn't match → suspicious
                        msg.sendMessage(player, "premium.uuid-mismatch");
                        plugin.getEffectsManager().playErrorSound(player);
                    }
                }
            });
        });
    }

    /**
     * Performs the actual premium login — authenticates the player.
     */
    private void performPremiumLogin(Player player, MessageManager msg,
            PremiumManager premiumManager, PremiumManager.MojangProfile profile) {
        // Mark as premium in DB
        premiumManager.setPremiumStatusAsync(player.getName(), true, profile.getUuid());

        // Authenticate
        plugin.getSessionManager().setAuthenticated(player, true);

        String ip = player.getAddress().getAddress().getHostAddress();
        plugin.getDatabaseManager().updateLoginAsync(player.getName(), ip);
        plugin.getLoginHistoryManager().logLoginAttempt(player.getName(), ip, true, "premium");
        plugin.getAuthSecurityManager().recordSuccessfulAttempt(player);

        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);

        plugin.getLocationManager().restoreLocation(player);
        plugin.getCaptchaManager().clearCaptchaItems(player);
        plugin.getInventoryManager().restoreInventory(player);

        // Broadcast join message (respects config toggle)
        if (plugin.getConfigManager().isJoinMessageEnabled()) {
            String joinMsg = msg.getMessage("join.message",
                    MessageManager.createPlaceholders("player", player.getName()));
            plugin.getServer().broadcastMessage(joinMsg);
        }

        // Effects
        plugin.getMusicManager().stopMusic(player);
        plugin.getEffectsManager().showLoginSuccessTitle(player);
        plugin.getEffectsManager().playLoginSound(player);
        plugin.getEffectsManager().playLoginParticles(player);
        plugin.getEffectsManager().removeBossBar(player);

        msg.sendMessage(player, "premium.login-success");

        if (!plugin.getConfigManager().isCleanConsole()) {
            plugin.getLogger().info(player.getName() + " logged in via premium (Mojang verified).");
        }

        // Fire API event
        Bukkit.getPluginManager().callEvent(new PlayerLoginEvent(player, false));
    }

    /**
     * Handles toggling premium status for already-authenticated players.
     */
    private void handlePremiumToggle(Player player, MessageManager msg, PremiumManager premiumManager) {
        premiumManager.getPremiumStatusAsync(player.getName()).thenAccept(status -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline())
                    return;

                if (status.isPremium()) {
                    // Disable premium
                    premiumManager.setPremiumStatusAsync(player.getName(), false, null);
                    msg.sendMessage(player, "premium.disabled-for-account");
                    plugin.getEffectsManager().playLoginSound(player);

                    if (!plugin.getConfigManager().isCleanConsole()) {
                        plugin.getLogger().info(player.getName() + " disabled premium auto-login.");
                    }
                } else {
                    // Enable premium — verify first
                    premiumManager.lookupMojangProfile(player.getName()).thenAccept(profile -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (!player.isOnline())
                                return;

                            if (profile == null || !profile.isPremium()) {
                                msg.sendMessage(player, "premium.not-premium");
                                plugin.getEffectsManager().playErrorSound(player);
                                return;
                            }

                            if (premiumManager.isVerifiedPremium(player, profile)) {
                                premiumManager.setPremiumStatusAsync(player.getName(), true, profile.getUuid());
                                msg.sendMessage(player, "premium.enabled-for-account");
                                plugin.getEffectsManager().playRegisterSound(player);

                                if (!plugin.getConfigManager().isCleanConsole()) {
                                    plugin.getLogger().info(player.getName() + " enabled premium auto-login.");
                                }
                            } else {
                                msg.sendMessage(player, "premium.cannot-verify");
                                plugin.getEffectsManager().playErrorSound(player);
                            }
                        });
                    });
                }
            });
        });
    }
}
