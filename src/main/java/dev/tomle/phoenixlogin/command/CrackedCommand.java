package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.MessageManager;
import dev.tomle.phoenixlogin.model.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /cracked command — for players who are NOT premium.
 *
 * When the premium system prompts "Are you premium or cracked?",
 * cracked players type /cracked to proceed to the normal login/register flow.
 * This skips the premium verification and goes straight to password auth.
 */
public class CrackedCommand implements CommandExecutor {

    private final PhoenixLogin plugin;

    public CrackedCommand(PhoenixLogin plugin) {
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

        if (!plugin.getPremiumManager().isEnabled()) {
            msg.sendMessage(player, "premium.disabled");
            return true;
        }

        if (plugin.getSessionManager().isAuthenticated(player)) {
            msg.sendMessage(player, "auth.already-logged");
            return true;
        }

        // Mark this player as having chosen "cracked" mode → show login/register
        PlayerData data = plugin.getSessionManager().getPlayerData(player);
        if (data == null) {
            msg.sendMessage(player, "auth.error");
            return true;
        }

        // Check if player is waiting at the premium prompt
        if (!plugin.getConnectionListener().isWaitingForPremiumChoice(player)) {
            // Not at the premium prompt — just inform them
            if (data.isRegistered()) {
                msg.sendMessage(player, "auth.please-login");
            } else {
                msg.sendMessage(player, "auth.please-register");
            }
            return true;
        }

        // Remove from premium choice state
        plugin.getConnectionListener().resolvePremiumChoice(player);

        // Show the appropriate login/register flow
        if (data.isRegistered()) {
            msg.sendMessage(player, "auth.please-login");
            if (plugin.getConfigManager().isAutoKickEnabled()) {
                int delay = plugin.getConfigManager().getAutoKickDelay();
                plugin.getEffectsManager().showLoginBossBar(player, delay);
            }
        } else {
            msg.sendMessage(player, "auth.please-register");
            if (plugin.getConfigManager().isAutoKickEnabled()) {
                int delay = plugin.getConfigManager().getAutoKickDelay();
                plugin.getEffectsManager().showRegisterBossBar(player, delay);
            }
        }

        msg.sendMessage(player, "premium.cracked-selected");

        if (!plugin.getConfigManager().isCleanConsole()) {
            plugin.getLogger().info(player.getName() + " selected cracked mode.");
        }

        return true;
    }
}
