package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.api.event.PlayerRegisterEvent;
import dev.tomle.phoenixlogin.api.event.PreRegisterEvent;
import dev.tomle.phoenixlogin.manager.MessageManager;
import dev.tomle.phoenixlogin.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class RegisterCommand implements CommandExecutor {

    private final PhoenixLogin plugin;

    public RegisterCommand(PhoenixLogin plugin) {
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

        PlayerData data = plugin.getSessionManager().getPlayerData(player);
        if (data != null && data.isRegistered()) {
            msg.sendMessage(player, "auth.already-registered");
            return true;
        }

        // Block if captcha is pending
        if (plugin.getCaptchaManager().hasPendingCaptcha(player)) {
            msg.sendMessage(player, "captcha.required");
            plugin.getEffectsManager().playErrorSound(player);
            return true;
        }

        if (args.length != 2) {
            msg.sendMessage(player, "auth.register-usage");
            return true;
        }

        String password = args[0];
        String confirm = args[1];

        if (!password.equals(confirm)) {
            msg.sendMessage(player, "auth.password-mismatch");
            plugin.getEffectsManager().playErrorSound(player);
            return true;
        }

        if (!plugin.getAuthSecurityManager().validatePassword(password)) {
            int minLength = plugin.getConfigManager().getMinPasswordLength();
            int maxLength = plugin.getConfigManager().getMaxPasswordLength();

            if (password.length() < minLength) {
                Map<String, String> placeholders = MessageManager.createPlaceholders(
                        "min", String.valueOf(minLength));
                msg.sendMessage(player, "auth.password-too-short", placeholders);
            } else if (password.length() > maxLength) {
                Map<String, String> placeholders = MessageManager.createPlaceholders(
                        "max", String.valueOf(maxLength));
                msg.sendMessage(player, "auth.password-too-long", placeholders);
            } else {
                msg.sendMessage(player, "auth.password-requirements");
            }

            plugin.getEffectsManager().playErrorSound(player);
            return true;
        }

        // Fire PreRegisterEvent (cancellable)
        PreRegisterEvent preRegisterEvent = new PreRegisterEvent(player, password);
        Bukkit.getPluginManager().callEvent(preRegisterEvent);

        if (preRegisterEvent.isCancelled()) {
            if (preRegisterEvent.getCancelMessage() != null) {
                player.sendMessage(preRegisterEvent.getCancelMessage());
            }
            return true;
        }

        // Check IP account limit
        String ip = player.getAddress().getAddress().getHostAddress();
        int maxAccounts = plugin.getConfigManager().getMaxAccountsPerIP();
        if (maxAccounts > 0) {
            plugin.getDatabaseManager().countAccountsByIPAsync(ip).thenAccept(count -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline())
                        return;
                    if (count >= maxAccounts) {
                        msg.sendMessage(player, "auth.too-many-accounts");
                        plugin.getEffectsManager().playErrorSound(player);
                        return;
                    }
                    performRegistration(player, password, ip);
                });
            });
        } else {
            performRegistration(player, password, ip);
        }

        return true;
    }

    private void performRegistration(Player player, String password, String ip) {
        plugin.getDatabaseManager().registerPlayerAsync(player.getName(), password, ip)
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline())
                            return;
                        if (success) {
                            handleSuccessfulRegistration(player);
                        } else {
                            handleFailedRegistration(player);
                        }
                    });
                });
    }

    private void handleSuccessfulRegistration(Player player) {
        MessageManager msg = plugin.getMessageManager();

        plugin.getDatabaseManager().loadPlayerDataAsync(player.getName())
                .thenAccept(data -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline())
                            return;

                        PlayerData sessionData = plugin.getSessionManager().getPlayerData(player);
                        if (sessionData != null) {
                            sessionData.setPasswordHash(data.getPasswordHash());
                            sessionData.setRegistrationDate(data.getRegistrationDate());
                        }

                        plugin.getSessionManager().setAuthenticated(player, true);
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
                        plugin.getEffectsManager().showRegisterSuccessTitle(player);
                        plugin.getEffectsManager().playRegisterSound(player);
                        plugin.getEffectsManager().playLoginParticles(player);
                        plugin.getEffectsManager().removeBossBar(player);

                        msg.sendMessage(player, "auth.register-success");

                        String ip = player.getAddress().getAddress().getHostAddress();
                        plugin.getLoginHistoryManager().logLoginAttempt(player.getName(), ip, true, "register");

                        if (!plugin.getConfigManager().isCleanConsole()) {
                            plugin.getLogger().info(player.getName() + " registered.");
                        }

                        // Fire PlayerRegisterEvent
                        Bukkit.getPluginManager().callEvent(new PlayerRegisterEvent(player));
                    });
                });
    }

    private void handleFailedRegistration(Player player) {
        plugin.getEffectsManager().playErrorSound(player);
        plugin.getMessageManager().sendMessage(player, "auth.register-failed");
    }
}
