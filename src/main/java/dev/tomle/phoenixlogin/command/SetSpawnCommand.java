package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.MessageManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class SetSpawnCommand implements CommandExecutor {

    private final PhoenixLogin plugin;

    public SetSpawnCommand(PhoenixLogin plugin) {
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

        if (!player.hasPermission("phoenixlogin.setspawn")) {
            msg.sendMessage(player, "commands.setspawn.no-permission");
            return true;
        }

        Location loc = player.getLocation();

        // Guardar spawn en config
        plugin.getConfig().set("login.teleport-to-spawn", true);
        plugin.getConfig().set("login.spawn-location.world", loc.getWorld().getName());
        plugin.getConfig().set("login.spawn-location.x", loc.getX());
        plugin.getConfig().set("login.spawn-location.y", loc.getY());
        plugin.getConfig().set("login.spawn-location.z", loc.getZ());
        plugin.getConfig().set("login.spawn-location.yaw", loc.getYaw());
        plugin.getConfig().set("login.spawn-location.pitch", loc.getPitch());
        plugin.saveConfig();

        // Recargar config manager
        plugin.getConfigManager().reload();

        Map<String, String> placeholders = MessageManager.createPlaceholders(
                "world", loc.getWorld().getName(),
                "x", String.format("%.2f", loc.getX()),
                "y", String.format("%.2f", loc.getY()),
                "z", String.format("%.2f", loc.getZ()));

        msg.sendMessage(player, "commands.setspawn.success");
        player.sendMessage(msg.getMessage("commands.setspawn.world", placeholders));
        player.sendMessage(msg.getMessage("commands.setspawn.coordinates", placeholders));
        msg.sendMessage(player, "commands.setspawn.info");

        plugin.getLogger().info(player.getName() + " set post-login spawn at: " +
                loc.getWorld().getName() + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

        return true;
    }
}
