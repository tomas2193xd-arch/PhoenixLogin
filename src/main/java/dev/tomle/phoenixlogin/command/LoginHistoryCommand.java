package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.LoginHistoryManager;
import dev.tomle.phoenixlogin.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Command to view login history.
 */
public class LoginHistoryCommand implements CommandExecutor {

    private final PhoenixLogin plugin;

    public LoginHistoryCommand(PhoenixLogin plugin) {
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

        if (!plugin.getSessionManager().isAuthenticated(player)) {
            msg.sendMessage(player, "auth.please-login");
            return true;
        }

        String targetPlayer = player.getName();
        boolean isAdmin = player.hasPermission("phoenixlogin.admin");

        if (args.length > 0 && isAdmin) {
            targetPlayer = args[0];
        }

        final String queryPlayer = targetPlayer;
        final boolean showingOther = !targetPlayer.equals(player.getName());

        plugin.getLoginHistoryManager().getLoginHistory(queryPlayer, 10)
                .thenAccept(history -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (history.isEmpty()) {
                            player.sendMessage(msg.getMessage("history.no-history"));
                            return;
                        }

                        player.sendMessage(msg.getMessage("history.header"));
                        if (showingOther) {
                            Map<String, String> placeholders = MessageManager.createPlaceholders(
                                    "player", queryPlayer);
                            player.sendMessage(msg.getMessage("history.title-other", placeholders));
                        } else {
                            player.sendMessage(msg.getMessage("history.title"));
                        }
                        player.sendMessage(msg.getMessage("history.header"));

                        for (LoginHistoryManager.LoginEntry entry : history) {
                            String status = entry.getStatusColor() + entry.getStatusSymbol();
                            String date = entry.getFormattedDate();
                            String ip = entry.getIpAddress();
                            String method = entry.getMethod();

                            player.sendMessage(
                                    status + " §7" + date + " §8| §f" + ip + " §8| §e" + method);
                        }

                        player.sendMessage(msg.getMessage("history.footer"));
                        Map<String, String> countPlaceholders = MessageManager.createPlaceholders(
                                "count", String.valueOf(history.size()));
                        player.sendMessage(msg.getMessage("history.showing", countPlaceholders));
                    });
                });

        return true;
    }
}
