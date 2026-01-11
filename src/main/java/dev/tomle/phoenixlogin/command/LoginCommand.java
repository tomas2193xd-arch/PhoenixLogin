package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.MessageManager;
import dev.tomle.phoenixlogin.model.PlayerData;
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
            sender.sendMessage(
                    plugin.getMessageManager().colorize(plugin.getMessageManager().getMessage("commands.player-only")));
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

        // 🛡 BLOQUEO: Si tiene captcha pendiente, DEBE completarlo primero
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
            return true;
        }

        String password = args[0];

        plugin.getDatabaseManager().verifyPasswordAsync(player.getName(), password)
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
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

        // Registrar en historial
        plugin.getLoginHistoryManager().logLoginAttempt(player.getName(), ip, true, "password");

        plugin.getAuthSecurityManager().recordSuccessfulAttempt(player);

        // Limpiar inventario de captcha si existe
        // Limpiar inventario de captcha si existe (YA NO NECESARIO, LO HACE EL JOIN)
        // player.getInventory().clear();

        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);

        // RESTAURAR ubicación del jugador (desde VoidAuthWorld al mundo real)
        // plugin.getLogger().info(">>> BEFORE restoreLocation for " + player.getName()
        // + " ... " + player.getLocation().getBlockZ() + ")");

        plugin.getLocationManager().restoreLocation(player);

        // 🛡 LIMPIEZA: Borrar items de Auth/Captcha ANTES de restaurar
        plugin.getCaptchaManager().clearCaptchaItems(player);

        // ✅ Restaurar inventario (items reales) una vez en el mundo correcto
        plugin.getInventoryManager().restoreInventory(player);

        // plugin.getLogger().info(">>> AFTER restoreLocation for " + player.getName() +
        // " ... " + player.getLocation().getBlockZ() + ")");

        String joinMsg = msg.getMessage("join.message",
                MessageManager.createPlaceholders("player", player.getName()));
        plugin.getServer().broadcastMessage(joinMsg);

        // 🎵 DETENER MÚSICA
        plugin.getMusicManager().stopMusic(player);

        plugin.getEffectsManager().showLoginSuccessTitle(player);
        plugin.getEffectsManager().playLoginSound(player);
        plugin.getEffectsManager().playLoginParticles(player);
        plugin.getEffectsManager().removeBossBar(player);

        msg.sendMessage(player, "auth.login-success");

        plugin.getLogger().info(player.getName() + " has logged in successfully.");
    }

    private void handleFailedLogin(Player player) {
        MessageManager msg = plugin.getMessageManager();

        plugin.getAuthSecurityManager().recordFailedAttempt(player);

        int remaining = plugin.getAuthSecurityManager().getRemainingAttempts(player);

        plugin.getEffectsManager().playErrorSound(player);
        plugin.getEffectsManager().playErrorParticles(player);

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
