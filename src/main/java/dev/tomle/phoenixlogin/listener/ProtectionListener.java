package dev.tomle.phoenixlogin.listener;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

public class ProtectionListener implements Listener {

    private final PhoenixLogin plugin;

    public ProtectionListener(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    // Bloquear movimiento
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfigManager().isBlockMovement()) {
            return;
        }

        Player player = event.getPlayer();

        if (!plugin.getSessionManager().isAuthenticated(player)) {
            // Permitir SOLO rotación de cabeza (yaw/pitch), bloquear movimiento X/Y/Z
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getZ() != event.getTo().getZ() ||
                    Math.abs(event.getFrom().getY() - event.getTo().getY()) > 0.1) {
                // Preservar la rotación de la cabeza pero mover al jugador de vuelta
                event.setTo(event.getFrom().setDirection(event.getTo().getDirection()));
            }
        }
    }

    // Bloquear comandos
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfigManager().isBlockCommands()) {
            return;
        }

        Player player = event.getPlayer();

        if (!plugin.getSessionManager().isAuthenticated(player)) {
            String command = event.getMessage().toLowerCase().split(" ")[0];

            // Lista de comandos SIEMPRE permitidos (autenticación)
            String[] allowedCommands = {
                    "/login",
                    "/register",
                    "/captcha",
                    "/l",
                    "/reg",
                    "/loguear",
                    "/registrar"
            };

            // Verificar si el comando está en la lista de permitidos
            boolean allowed = false;
            for (String cmd : allowedCommands) {
                if (command.equalsIgnoreCase(cmd)) {
                    allowed = true;
                    break;
                }
            }

            if (!allowed) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "blocked.command");
            }
        }
    }

    // Bloquear chat
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getSessionManager().isAuthenticated(player)) {
            event.setCancelled(true);

            // Enviar mensaje en el thread principal
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getMessageManager().sendMessage(player, "blocked.chat");
            });
        }
    }

    // Bloquear interacciones
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfigManager().isBlockInteract()) {
            return;
        }

        Player player = event.getPlayer();

        if (!plugin.getSessionManager().isAuthenticated(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.getConfigManager().isBlockInteract()) {
            return;
        }

        Player player = event.getPlayer();

        if (!plugin.getSessionManager().isAuthenticated(player)) {
            event.setCancelled(true);
        }
    }

    // Bloquear daño
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (!plugin.getSessionManager().isAuthenticated(player)) {
            // SIEMPRE cancelar daño si no está autenticado
            event.setCancelled(true);

            // Extra seguridad: si el jugador está cayendo en el void, teletransportarlo de
            // vuelta
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (plugin.getWorldManager().isInVoidWorld(player)) {
                        player.teleport(plugin.getWorldManager().getVoidSpawnLocation());
                    }
                });
            }

            // Si el jugador está debajo de Y=0 en el VoidWorld, teletransportarlo de vuelta
            if (plugin.getWorldManager().isInVoidWorld(player) && player.getLocation().getY() < 0) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.teleport(plugin.getWorldManager().getVoidSpawnLocation());
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getConfigManager().isBlockDamage()) {
            return;
        }

        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();

            if (!plugin.getSessionManager().isAuthenticated(player)) {
                event.setCancelled(true);
            }
        }
    }

    // Bloquear rotura de bloques
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getSessionManager().isAuthenticated(player)) {
            event.setCancelled(true);
        }
    }

    // Bloquear colocación de bloques
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getSessionManager().isAuthenticated(player)) {
            event.setCancelled(true);
        }
    }

    // Bloquear drops
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getSessionManager().isAuthenticated(player)) {
            event.setCancelled(true);
        }
    }

    // Bloquear pickups
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getSessionManager().isAuthenticated(player)) {
            event.setCancelled(true);
        }
    }

    // Bloquear inventory clicks (excepto para captcha)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (!plugin.getSessionManager().isAuthenticated(player)) {
            // No cancelar si hay un captcha activo (el listener de captcha lo manejará)
            if (!plugin.getCaptchaManager().hasPendingCaptcha(player)) {
                event.setCancelled(true);
            }
        }
    }
}
