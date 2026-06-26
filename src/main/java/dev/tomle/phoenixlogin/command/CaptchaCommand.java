package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CaptchaCommand implements CommandExecutor {

    private final PhoenixLogin plugin;

    public CaptchaCommand(PhoenixLogin plugin) {
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

        if (plugin.getSessionManager().isAuthenticated(player)) {
            msg.sendMessage(player, "captcha.already-authenticated");
            return true;
        }

        if (!plugin.getCaptchaManager().isCaptchaRequired()) {
            msg.sendMessage(player, "captcha.not-required");
            return true;
        }

        if (!plugin.getCaptchaManager().hasCaptcha(player)) {
            msg.sendMessage(player, "captcha.no-pending");
            return true;
        }

        if (args.length != 1) {
            msg.sendMessage(player, "captcha.suggestion");
            return true;
        }

        String code = args[0];

        if (plugin.getCaptchaManager().verifyCaptcha(player, code)) {
            msg.sendMessage(player, "captcha.success");
            player.getInventory().clear();
            plugin.getEffectsManager().playLoginSound(player);

            player.sendMessage("");
            dev.tomle.phoenixlogin.model.PlayerData data = plugin.getSessionManager().getPlayerData(player);
            if (data != null && data.isRegistered()) {
                player.sendMessage(msg.getMessage("captcha.next-step-login"));
            } else {
                player.sendMessage(msg.getMessage("captcha.next-step-register"));
            }
            player.sendMessage("");
        } else {
            msg.sendMessage(player, "captcha.failed");
            plugin.getEffectsManager().playErrorSound(player);
        }

        return true;
    }
}
