package dev.tomle.phoenixlogin.listener;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hides command suggestions from unauthenticated players.
 * Only auth-related commands are visible until authentication.
 * The allowed commands list is configurable via config.yml.
 */
public class ChatBlockListener implements Listener {

    private final PhoenixLogin plugin;
    private final Set<String> allowedCommands;

    public ChatBlockListener(PhoenixLogin plugin) {
        this.plugin = plugin;
        this.allowedCommands = new HashSet<>();
        loadAllowedCommands();
    }

    /**
     * Loads allowed commands from config, with sensible defaults.
     */
    public void loadAllowedCommands() {
        allowedCommands.clear();

        // Built-in defaults that should always be allowed
        Set<String> defaults = Set.of(
                "login", "l",
                "register", "reg",
                "captcha",
                "premium", "prem",
                "cracked",
                "verify"
        );
        allowedCommands.addAll(defaults);

        // Load additional allowed commands from config
        List<String> configCommands = plugin.getConfig().getStringList("login.allowed-commands");
        for (String cmd : configCommands) {
            allowedCommands.add(cmd.toLowerCase().replace("/", ""));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();

        // Authenticated or still loading — keep normal command visibility
        if (plugin.getSessionManager().isAuthenticated(player))
            return;
        if (player.hasPermission("phoenixlogin.bypass"))
            return;
        ConnectionListener cl = plugin.getConnectionListener();
        if (cl != null && cl.isLoading(player))
            return;

        // Unauthenticated players only see auth commands
        event.getCommands().removeIf(cmd -> !allowedCommands.contains(cmd.toLowerCase()));
    }
}
