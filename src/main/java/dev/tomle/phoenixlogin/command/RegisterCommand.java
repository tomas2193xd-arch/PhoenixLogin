package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.MessageManager;
import dev.tomle.phoenixlogin.model.PlayerData;
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
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        MessageManager msg = plugin.getMessageManager();

        PlayerData data = plugin.getSessionManager().getPlayerData(player);
        if (data != null && data.isRegistered()) {
            msg.sendMessage(player, "auth.already-registered");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(msg.colorize("&7Uso: &f/register <contraseña> <confirmar>"));
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
                player.sendMessage(msg.colorize("&cLa contraseña no cumple con los requisitos de seguridad."));
            }

            plugin.getEffectsManager().playErrorSound(player);
            return true;
        }

        String ip = player.getAddress().getAddress().getHostAddress();
        plugin.getDatabaseManager().registerPlayerAsync(player.getName(), password, ip)
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            handleSuccessfulRegistration(player);
                        } else {
                            handleFailedRegistration(player);
                        }
                    });
                });

        return true;
    }

    private void handleSuccessfulRegistration(Player player) {
        MessageManager msg = plugin.getMessageManager();

        plugin.getDatabaseManager().loadPlayerDataAsync(player.getName())
                .thenAccept(data -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        PlayerData sessionData = plugin.getSessionManager().getPlayerData(player);
                        if (sessionData != null) {
                            sessionData.setPasswordHash(data.getPasswordHash());
                            sessionData.setRegistrationDate(data.getRegistrationDate());
                        }

                        plugin.getSessionManager().setAuthenticated(player, true);

                        player.setWalkSpeed(0.2f);
                        player.setFlySpeed(0.1f);

                        // RESTAURAR ubicación del jugador (desde VoidAuthWorld al mundo real)
                        plugin.getLogger().info(">>> BEFORE restoreLocation for " + player.getName() +
                                " - Current location: " + player.getLocation().getWorld().getName() +
                                " (" + player.getLocation().getBlockX() + ", " +
                                player.getLocation().getBlockY() + ", " +
                                player.getLocation().getBlockZ() + ")");

                        plugin.getLocationManager().restoreLocation(player);

                        plugin.getLogger().info(">>> AFTER restoreLocation for " + player.getName() +
                                " - New location: " + player.getLocation().getWorld().getName() +
                                " (" + player.getLocation().getBlockX() + ", " +
                                player.getLocation().getBlockY() + ", " +
                                player.getLocation().getBlockZ() + ")");

                        String joinMsg = "§e" + player.getName() + " joined the game";
                        plugin.getServer().broadcastMessage(joinMsg);

                        // 🎵 DETENER MÚSICA
                        plugin.getMusicManager().stopMusic(player);

                        plugin.getEffectsManager().showRegisterSuccessTitle(player);
                        plugin.getEffectsManager().playRegisterSound(player);
                        plugin.getEffectsManager().playLoginParticles(player);
                        plugin.getEffectsManager().removeBossBar(player);

                        msg.sendMessage(player, "auth.register-success");

                        plugin.getLogger().info(player.getName() + " has registered successfully.");
                    });
                });
    }

    private void handleFailedRegistration(Player player) {
        MessageManager msg = plugin.getMessageManager();
        plugin.getEffectsManager().playErrorSound(player);
        player.sendMessage(
                msg.colorize("&cHubo un error al registrar tu cuenta. Por favor contacta a un administrador."));
    }
}
