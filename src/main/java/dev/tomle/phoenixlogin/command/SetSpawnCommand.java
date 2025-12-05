package dev.tomle.phoenixlogin.command;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.MessageManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final PhoenixLogin plugin;

    public SetSpawnCommand(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        MessageManager msg = plugin.getMessageManager();

        if (!player.hasPermission("phoenixlogin.setspawn")) {
            player.sendMessage(msg.colorize("&cYou don't have permission to use this command."));
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

        player.sendMessage(msg.colorize("&a✓ Post-login spawn set successfully!"));
        player.sendMessage(msg.colorize("&7World: &f" + loc.getWorld().getName()));
        player.sendMessage(msg.colorize("&7X: &f" + String.format("%.2f", loc.getX())));
        player.sendMessage(msg.colorize("&7Y: &f" + String.format("%.2f", loc.getY())));
        player.sendMessage(msg.colorize("&7Z: &f" + String.format("%.2f", loc.getZ())));
        player.sendMessage(msg.colorize("&eAfter login/register, players will spawn here!"));

        plugin.getLogger().info(player.getName() + " set post-login spawn at: " +
                loc.getWorld().getName() + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

        return true;
    }
}
