package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class ChangePasswordCommand implements CommandExecutor {

    private final PhoenixLogin plugin;

    public ChangePasswordCommand(PhoenixLogin plugin) {
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

        // Verificar si está autenticado
        if (!plugin.getSessionManager().isAuthenticated(player)) {
            msg.sendMessage(player, "auth.please-login");
            return true;
        }

        // Verificar argumentos
        if (args.length != 2) {
            msg.sendMessage(player, "commands.changepassword.usage");
            return true;
        }

        String oldPassword = args[0];
        String newPassword = args[1];

        // Validar nueva contraseña
        if (!plugin.getAuthSecurityManager().validatePassword(newPassword)) {
            int minLength = plugin.getConfigManager().getMinPasswordLength();
            int maxLength = plugin.getConfigManager().getMaxPasswordLength();

            if (newPassword.length() < minLength) {
                Map<String, String> placeholders = MessageManager.createPlaceholders(
                        "min", String.valueOf(minLength));
                msg.sendMessage(player, "auth.password-too-short", placeholders);
            } else if (newPassword.length() > maxLength) {
                Map<String, String> placeholders = MessageManager.createPlaceholders(
                        "max", String.valueOf(maxLength));
                msg.sendMessage(player, "auth.password-too-long", placeholders);
            } else {
                player.sendMessage(msg.colorize("&cLa nueva contraseña no cumple con los requisitos de seguridad."));
            }

            plugin.getEffectsManager().playErrorSound(player);
            return true;
        }

        // Verificar contraseña actual
        plugin.getDatabaseManager().verifyPasswordAsync(player.getName(), oldPassword)
                .thenAccept(correct -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (correct) {
                            // Cambiar contraseña
                            plugin.getDatabaseManager().changePasswordAsync(player.getName(), newPassword)
                                    .thenAccept(success -> {
                                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                                            if (success) {
                                                msg.sendMessage(player, "commands.changepassword.success");
                                                plugin.getEffectsManager().playRegisterSound(player);
                                                plugin.getLogger().info(player.getName() + " changed their password.");
                                            } else {
                                                player.sendMessage(msg.colorize("&cError al cambiar la contraseña."));
                                                plugin.getEffectsManager().playErrorSound(player);
                                            }
                                        });
                                    });
                        } else {
                            msg.sendMessage(player, "commands.changepassword.wrong-old");
                            plugin.getEffectsManager().playErrorSound(player);
                        }
                    });
                });

        return true;
    }
}
