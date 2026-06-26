package dev.tomle.phoenixlogin.listener;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.EffectsManager;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;

import java.util.Arrays;
import java.util.List;

/**
 * Restricts unauthenticated players from performing most in-game actions.
 * Players in the "loading" state (async data load) are NOT blocked to
 * prevent false positives during rapid join/quit cycles.
 */
public class ProtectionListener implements Listener {

    private final PhoenixLogin plugin;

    private static final List<String> ALLOWED_COMMANDS = Arrays.asList(
            "/login", "/register", "/l", "/reg", "/captcha", "/premium", "/prem", "/cracked");

    public ProtectionListener(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns true if this player should be blocked by protection.
     * Returns false if the player is authenticated, bypassed, or still loading.
     */
    private boolean shouldBlock(Player player) {
        if (plugin.getSessionManager().isAuthenticated(player))
            return false;
        if (player.hasPermission("phoenixlogin.bypass"))
            return false;

        // Don't block during async data load — session hasn't been set up yet
        ConnectionListener cl = plugin.getConnectionListener();
        if (cl != null && cl.isLoading(player))
            return false;

        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!shouldBlock(player))
            return;
        if (!plugin.getConfigManager().isBlockMovement())
            return;

        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            if (plugin.getWorldManager().isInVoidWorld(player)) {
                // Preserve head rotation so players can look at captcha maps
                org.bukkit.Location voidSpawn = plugin.getWorldManager().getVoidSpawnLocation().clone();
                voidSpawn.setYaw(event.getTo().getYaw());
                voidSpawn.setPitch(event.getTo().getPitch());
                event.setTo(voidSpawn);
            } else {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!shouldBlock(player))
            return;
        if (!plugin.getConfigManager().isBlockCommands())
            return;

        String message = event.getMessage().toLowerCase().split(" ")[0];

        if (!ALLOWED_COMMANDS.contains(message)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "blocked.command");
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!shouldBlock(player))
            return;

        event.setCancelled(true);
        plugin.getMessageManager().sendMessage(player, "blocked.chat");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!shouldBlock(player))
            return;
        if (!plugin.getConfigManager().isBlockInteract())
            return;

        // Allow interact in void world for ITEM captcha
        if (plugin.getWorldManager().isInVoidWorld(player) &&
                plugin.getCaptchaManager().hasPendingCaptcha(player)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();

        // Always cancel damage from our login fireworks
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) event;
            if (damageByEntity.getDamager() instanceof Firework) {
                Firework fw = (Firework) damageByEntity.getDamager();
                if (fw.hasMetadata(EffectsManager.FIREWORK_META_KEY)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (!shouldBlock(player))
            return;
        if (!plugin.getConfigManager().isBlockDamage())
            return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Cancel firework damage from login celebrations
        if (event.getDamager() instanceof Firework) {
            Firework fw = (Firework) event.getDamager();
            if (fw.hasMetadata(EffectsManager.FIREWORK_META_KEY)) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (shouldBlock(damager)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!shouldBlock(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!shouldBlock(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!shouldBlock(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        if (!shouldBlock(player))
            return;
        event.setCancelled(true);
    }
}
