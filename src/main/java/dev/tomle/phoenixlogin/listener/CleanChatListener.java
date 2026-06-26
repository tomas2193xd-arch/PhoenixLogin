package dev.tomle.phoenixlogin.listener;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Iterator;

/**
 * Prevents unauthenticated players from seeing global chat messages.
 * OPs always see all messages regardless of auth status.
 */
public class CleanChatListener implements Listener {

    private final PhoenixLogin plugin;

    public CleanChatListener(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfigManager().isCleanChat()) {
            return;
        }

        Iterator<Player> iterator = event.getRecipients().iterator();
        while (iterator.hasNext()) {
            Player recipient = iterator.next();

            // OPs always see chat
            if (recipient.isOp()) {
                continue;
            }

            // Remove unauthenticated recipients
            if (!plugin.getSessionManager().isAuthenticated(recipient)) {
                iterator.remove();
            }
        }
    }
}
