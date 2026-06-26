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
            if (sender instanceof Player) {
                msg.sendMessage((Player) sender, "commands.admin.no-permission");
            } else {
                sender.sendMessage(msg.getMessage("commands.admin.no-permission"));
            }
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
                                ? new java.text.SimpleDateFormat(msg.getMessage("format.date"))
                                        .format(new java.util.Date(data.getRegistrationDate()))
                                : msg.getMessage("format.not-available");

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

                            plugin.getLogger().warning(sender.getName() + " unregistered: " + playerName);

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

        // AntiBot stats (in-memory, safe for main thread)
        int blocked = plugin.getAntiBotManager().getTotalBlocked();
        int attacks = plugin.getAntiBotManager().getTotalAttacksDetected();
        int blacklisted = plugin.getAntiBotManager().getBlacklistedCount();
        boolean isAttack = plugin.getAntiBotManager().isAttackMode();

        // Fetch registered count async to avoid blocking main thread
        plugin.getDatabaseManager().getRegisteredPlayersCountAsync().thenAccept(registered -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Map<String, String> placeholders = MessageManager.createPlaceholders(
                        "sessions", String.valueOf(activeSessions),
                        "authenticated", String.valueOf(authenticated),
                        "database", dbType,
                        "registered", String.valueOf(registered),
                        "blocked", String.valueOf(blocked),
                        "attacks", String.valueOf(attacks),
                        "blacklisted", String.valueOf(blacklisted),
                        "antibot-status", isAttack ? "&c&lATTACK" : "&aClean");

                sender.sendMessage(msg.getMessage("commands.admin.stats.header"));
                sender.sendMessage(msg.getMessage("commands.admin.stats.title"));
                sender.sendMessage(msg.getMessage("commands.admin.stats.header"));
                sender.sendMessage(msg.getMessage("commands.admin.stats.active-sessions", placeholders));
                sender.sendMessage(msg.getMessage("commands.admin.stats.authenticated", placeholders));
                sender.sendMessage(msg.getMessage("commands.admin.stats.database", placeholders));
                sender.sendMessage(msg.getMessage("commands.admin.stats.registered-players", placeholders));
                sender.sendMessage(msg.getMessage("commands.admin.stats.antibot-status", placeholders));
                sender.sendMessage(msg.getMessage("commands.admin.stats.antibot-blocked", placeholders));
                sender.sendMessage(msg.getMessage("commands.admin.stats.antibot-attacks", placeholders));
                sender.sendMessage(msg.getMessage("commands.admin.stats.antibot-blacklisted", placeholders));
                sender.sendMessage(msg.getMessage("commands.admin.stats.footer"));
            });
        });
    }
}
