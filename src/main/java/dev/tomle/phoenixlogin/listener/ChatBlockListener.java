package dev.tomle.phoenixlogin.listener;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

/**
 * Bloquea listado de comandos SOLO hasta que el jugador se autentique.
 * Después de login/register, todo vuelve a la normalidad (sistema de permisos
 * normal).
 */
public class ChatBlockListener implements Listener {

    private final PhoenixLogin plugin;

    public ChatBlockListener(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();

        // SI YA ESTÁ AUTENTICADO: No tocar nada, sistema de permisos normal
        if (plugin.getSessionManager().isAuthenticated(player)) {
            return; // Todo funciona normal: OPs ven comandos de OP, usuarios ven comandos de
                    // usuario
        }

        // SOLO si NO está autenticado: mostrar solo comandos de login/register
        event.getCommands().removeIf(cmd -> !cmd.equalsIgnoreCase("login") &&
                !cmd.equalsIgnoreCase("register") &&
                !cmd.equalsIgnoreCase("l") &&
                !cmd.equalsIgnoreCase("reg"));
    }
}
