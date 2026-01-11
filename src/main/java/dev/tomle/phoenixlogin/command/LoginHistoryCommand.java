package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.LoginHistoryManager;
import dev.tomle.phoenixlogin.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Comando para ver el historial de logins
 */
public class LoginHistoryCommand implements CommandExecutor {

    private final PhoenixLogin plugin;

    public LoginHistoryCommand(PhoenixLogin plugin) {
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

        // Verificar que esté autenticado
        if (!plugin.getSessionManager().isAuthenticated(player)) {
            msg.sendMessage(player, "auth.please-login");
            return true;
        }

        // Determinar qué jugador consultar
        String targetPlayer = player.getName();
        boolean isAdmin = player.hasPermission("phoenixlogin.admin");

        if (args.length > 0 && isAdmin) {
            targetPlayer = args[0];
        }

        final String queryPlayer = targetPlayer;
        final boolean showingOther = !targetPlayer.equals(player.getName());

        // Obtener historial
        plugin.getLoginHistoryManager().getLoginHistory(queryPlayer, 10)
                .thenAccept(history -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (history.isEmpty()) {
                            player.sendMessage("§cNo hay historial de logins disponible.");
                            return;
                        }

                        // Mostrar historial
                        player.sendMessage("§6§m-----------------------------");
                        player.sendMessage("§6§l📊 Login History" + (showingOther ? " - " + queryPlayer : ""));
                        player.sendMessage("§6§m-----------------------------");

                        for (LoginHistoryManager.LoginEntry entry : history) {
                            String status = entry.getStatusColor() + entry.getStatusSymbol();
                            String date = entry.getFormattedDate();
                            String ip = entry.getIpAddress();
                            String method = entry.getMethod();

                            player.sendMessage(
                                    status + " §7" + date + " §8| §f" + ip + " §8| §e" + method);
                        }

                        player.sendMessage("§6§m-----------------------------");
                        player.sendMessage("§7Showing last " + history.size() + " login(s)");
                    });
                });

        return true;
    }
}
