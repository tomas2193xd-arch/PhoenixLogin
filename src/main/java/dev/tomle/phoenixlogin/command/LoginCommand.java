package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.api.event.LoginFailedEvent;
import dev.tomle.phoenixlogin.api.event.PlayerLoginEvent;
import dev.tomle.phoenixlogin.api.event.PreLoginEvent;
import dev.tomle.phoenixlogin.manager.MessageManager;
import dev.tomle.phoenixlogin.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class LoginCommand implements CommandExecutor {

    private final PhoenixLogin plugin;

    public LoginCommand(PhoenixLogin plugin) {
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

        if (plugin.getSessionManager().isAuthenticated(player)) {
            msg.sendMessage(player, "auth.already-logged");
            return true;
        }

        PlayerData data = plugin.getSessionManager().getPlayerData(player);
        if (data == null || !data.isRegistered()) {
            msg.sendMessage(player, "auth.not-registered");
            return true;
        }

        // Block if captcha is pending
        if (plugin.getCaptchaManager().hasPendingCaptcha(player)) {
            msg.sendMessage(player, "captcha.required");
            plugin.getEffectsManager().playErrorSound(player);
            return true;
        }

        if (args.length != 1) {
            msg.sendMessage(player, "auth.login-usage");
            return true;
        }

        if (plugin.getAuthSecurityManager().isAccountLocked(player)) {
            long remaining = plugin.getAuthSecurityManager().getLockoutRemainingTime(player);
            Map<String, String> placeholders = MessageManager.createPlaceholders(
                    "duration", String.valueOf(remaining));
            msg.sendMessage(player, "auth.account-locked", placeholders);

            // Fire LoginFailedEvent for locked accounts
            int maxAttempts = plugin.getConfigManager().getMaxLoginAttempts();
            Bukkit.getPluginManager().callEvent(
                    new LoginFailedEvent(player, LoginFailedEvent.FailReason.LOCKED_OUT, maxAttempts));
            return true;
        }

        String password = args[0];

        // Fire PreLoginEvent (cancellable)
        PreLoginEvent preLoginEvent = new PreLoginEvent(player, password);
        Bukkit.getPluginManager().callEvent(preLoginEvent);

        if (preLoginEvent.isCancelled()) {
            if (preLoginEvent.getCancelMessage() != null) {
                player.sendMessage(preLoginEvent.getCancelMessage());
            }
            return true;
        }

        plugin.getDatabaseManager().verifyPasswordAsync(player.getName(), password)
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline())
                            return;
                        if (success) {
                            handleSuccessfulLogin(player);
                        } else {
                            handleFailedLogin(player);
                        }
                    });
                });

        return true;
    }

    private void handleSuccessfulLogin(Player player) {
        MessageManager msg = plugin.getMessageManager();

        plugin.getSessionManager().setAuthenticated(player, true);

        String ip = player.getAddress().getAddress().getHostAddress();
        plugin.getDatabaseManager().updateLoginAsync(player.getName(), ip);

        plugin.getLoginHistoryManager().logLoginAttempt(player.getName(), ip, true, "password");
        plugin.getAuthSecurityManager().recordSuccessfulAttempt(player);

        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);

        plugin.getLocationManager().restoreLocation(player);
        plugin.getCaptchaManager().clearCaptchaItems(player);
        plugin.getInventoryManager().restoreInventory(player);

        // Join message (respects config toggle)
        if (plugin.getConfigManager().isJoinMessageEnabled()) {
            String joinMsg = msg.getMessage("join.message",
                    MessageManager.createPlaceholders("player", player.getName()));
            plugin.getServer().broadcastMessage(joinMsg);
        }

        plugin.getMusicManager().stopMusic(player);
        plugin.getEffectsManager().showLoginSuccessTitle(player);
        plugin.getEffectsManager().playLoginSound(player);
        plugin.getEffectsManager().playLoginParticles(player);
        plugin.getEffectsManager().removeBossBar(player);

        msg.sendMessage(player, "auth.login-success");

        if (!plugin.getConfigManager().isCleanConsole()) {
            plugin.getLogger().info(player.getName() + " logged in.");
        }

        // Fire PlayerLoginEvent
        Bukkit.getPluginManager().callEvent(new PlayerLoginEvent(player, false));
    }

    private void handleFailedLogin(Player player) {
        MessageManager msg = plugin.getMessageManager();

        plugin.getAuthSecurityManager().recordFailedAttempt(player);
        int remaining = plugin.getAuthSecurityManager().getRemainingAttempts(player);

        plugin.getEffectsManager().playErrorSound(player);
        plugin.getEffectsManager().playErrorParticles(player);

        // Fire LoginFailedEvent
        int totalAttempts = plugin.getConfigManager().getMaxLoginAttempts() - remaining;
        Bukkit.getPluginManager().callEvent(
                new LoginFailedEvent(player, LoginFailedEvent.FailReason.WRONG_PASSWORD, totalAttempts));

        if (remaining > 0) {
            Map<String, String> placeholders = MessageManager.createPlaceholders(
                    "attempts", String.valueOf(remaining));
            msg.sendMessage(player, "auth.wrong-password", placeholders);
        } else {
            long lockout = plugin.getAuthSecurityManager().getLockoutRemainingTime(player);
            Map<String, String> placeholders = MessageManager.createPlaceholders(
                    "duration", String.valueOf(lockout));
            msg.sendMessage(player, "auth.account-locked", placeholders);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    String kickMessage = msg.getMessage("kick.too-many-attempts", placeholders);
                    player.kickPlayer(kickMessage);
                }
            }, 40L);
        }
    }
}
