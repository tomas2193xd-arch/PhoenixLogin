package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnregisterCommand implements CommandExecutor {

    private final PhoenixLogin plugin;

    public UnregisterCommand(PhoenixLogin plugin) {
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

        // Verificar si está autenticado
        if (!plugin.getSessionManager().isAuthenticated(player)) {
            msg.sendMessage(player, "auth.please-login");
            return true;
        }

        // Verificar argumentos
        if (args.length != 1) {
            msg.sendMessage(player, "commands.unregister.usage");
            return true;
        }

        String password = args[0];

        // Verificar contraseña
        plugin.getDatabaseManager().verifyPasswordAsync(player.getName(), password)
                .thenAccept(correct -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (correct) {
                            // Eliminar cuenta
                            plugin.getDatabaseManager().unregisterPlayerAsync(player.getName())
                                    .thenAccept(success -> {
                                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                                            if (success) {
                                                msg.sendMessage(player, "commands.unregister.success");
                                                plugin.getLogger()
                                                        .warning(player.getName() + " has unregistered their account!");

                                                // Kickear al jugador después de 3 segundos
                                                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                                    if (player.isOnline()) {
                                                        player.kickPlayer(
                                                                msg.getMessage("commands.unregister.kick-message"));
                                                    }
                                                }, 60L);
                                            } else {
                                                msg.sendMessage(player, "commands.unregister.failed");
                                                plugin.getEffectsManager().playErrorSound(player);
                                            }
                                        });
                                    });
                        } else {
                            msg.sendMessage(player, "commands.unregister.wrong-password");
                            plugin.getEffectsManager().playErrorSound(player);
                        }
                    });
                });

        return true;
    }
}
