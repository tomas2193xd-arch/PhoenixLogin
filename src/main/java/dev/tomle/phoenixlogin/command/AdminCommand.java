package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class AdminCommand implements CommandExecutor {

    private final PhoenixLogin plugin;

    public AdminCommand(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (!sender.hasPermission("phoenixlogin.admin")) {
            msg.sendMessage((Player) sender, "commands.admin.no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;

            case "info":
                if (args.length < 2) {
                    sender.sendMessage(msg.getMessage("commands.admin.usage-info"));
                    return true;
                }
                handleInfo(sender, args[1]);
                break;

            case "unregister":
                if (args.length < 2) {
                    sender.sendMessage(msg.getMessage("commands.admin.usage-unregister"));
                    return true;
                }
                handleUnregister(sender, args[1]);
                break;

            case "stats":
                handleStats(sender);
                break;

            default:
                sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        MessageManager msg = plugin.getMessageManager();
        sender.sendMessage(msg.getMessage("commands.admin.help.header"));
        sender.sendMessage(msg.getMessage("commands.admin.help.title"));
        sender.sendMessage(msg.getMessage("commands.admin.help.header"));
        sender.sendMessage(msg.getMessage("commands.admin.help.reload"));
        sender.sendMessage(msg.getMessage("commands.admin.help.info"));
        sender.sendMessage(msg.getMessage("commands.admin.help.unregister"));
        sender.sendMessage(msg.getMessage("commands.admin.help.stats"));
        sender.sendMessage(msg.getMessage("commands.admin.help.footer"));
    }

    private void handleReload(CommandSender sender) {
        MessageManager msg = plugin.getMessageManager();

        plugin.getConfigManager().reload();
        plugin.getMessageManager().reload();

        if (sender instanceof Player) {
            msg.sendMessage((Player) sender, "commands.admin.reload");
        } else {
            sender.sendMessage(msg.getMessage("commands.admin.reload"));
        }

        plugin.getLogger().info("Configuration reloaded by " + sender.getName());
    }

    private void handleInfo(CommandSender sender, String playerName) {
        MessageManager msg = plugin.getMessageManager();

        plugin.getDatabaseManager().loadPlayerDataAsync(playerName)
                .thenAccept(data -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!data.isRegistered()) {
                            if (sender instanceof Player) {
                                msg.sendMessage((Player) sender, "commands.admin.player-not-found");
                            } else {
                                sender.sendMessage(msg.getMessage("commands.admin.player-not-found"));
                            }
                            return;
                        }

                        String registered = data.getRegistrationDate() > 0
                                ? new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                                        .format(new java.util.Date(data.getRegistrationDate()))
                                : "N/A";

                        String lastLogin = data.getLastLogin() > 0
                                ? new java.text.SimpleDateFormat(msg.getMessage("format.date"))
                                        .format(new java.util.Date(data.getLastLogin()))
                                : msg.getMessage("format.never");

                        java.util.Map<String, String> placeholders = MessageManager.createPlaceholders(
                                "player", playerName,
                                "registered", registered,
                                "ip",
                                data.getLastIP() != null ? data.getLastIP() : msg.getMessage("format.not-available"),
                                "last-login", lastLogin);

                        String message = msg.getMessage("commands.admin.user-info", placeholders);
                        sender.sendMessage(msg.colorize(message));
                    });
                });
    }

    private void handleUnregister(CommandSender sender, String playerName) {
        MessageManager msg = plugin.getMessageManager();

        plugin.getDatabaseManager().unregisterPlayerAsync(playerName)
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            java.util.Map<String, String> placeholders = MessageManager.createPlaceholders(
                                    "player", playerName);

                            String message = msg.getMessage("commands.admin.unregister-player", placeholders);
                            sender.sendMessage(msg.colorize(message));

                            plugin.getLogger().warning(sender.getName() + " unregistered account for: " + playerName);

                            // Si el jugador está online, kickearlo
                            Player target = plugin.getServer().getPlayer(playerName);
                            if (target != null && target.isOnline()) {
                                target.kickPlayer(msg.getMessage("commands.admin.account-deleted-by-admin"));
                            }
                        } else {
                            if (sender instanceof Player) {
                                msg.sendMessage((Player) sender, "commands.admin.player-not-found");
                            } else {
                                sender.sendMessage(msg.getMessage("commands.admin.player-not-found"));
                            }
                        }
                    });
                });
    }

    private void handleStats(CommandSender sender) {
        MessageManager msg = plugin.getMessageManager();

        int activeSessions = plugin.getSessionManager().getActiveSessionsCount();
        int authenticated = plugin.getSessionManager().getAuthenticatedCount();
        String dbType = plugin.getConfigManager().getDatabaseType();
        String language = plugin.getConfigManager().getLanguage();

        Map<String, String> placeholders = MessageManager.createPlaceholders(
                "sessions", String.valueOf(activeSessions),
                "authenticated", String.valueOf(authenticated),
                "database", dbType,
                "language", language);

        sender.sendMessage(msg.getMessage("commands.admin.stats.header"));
        sender.sendMessage(msg.getMessage("commands.admin.stats.title"));
        sender.sendMessage(msg.getMessage("commands.admin.stats.header"));
        sender.sendMessage(msg.getMessage("commands.admin.stats.active-sessions", placeholders));
        sender.sendMessage(msg.getMessage("commands.admin.stats.authenticated", placeholders));
        sender.sendMessage(msg.getMessage("commands.admin.stats.database", placeholders));
        sender.sendMessage(msg.getMessage("commands.admin.stats.language", placeholders));
        sender.sendMessage(msg.getMessage("commands.admin.stats.footer"));
    }
}
