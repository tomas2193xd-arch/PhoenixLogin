package dev.tomle.phoenixlogin.listener;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.MessageManager;
import dev.tomle.phoenixlogin.model.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Handles captcha inventory interactions (ITEM type).
 */
public class CaptchaListener implements Listener {

    private final PhoenixLogin plugin;

    public CaptchaListener(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        if (plugin.getSessionManager().isAuthenticated(player))
            return;
        if (!plugin.getCaptchaManager().hasPendingCaptcha(player))
            return;

        // Allow clicks if captcha type is ITEM
        if (!plugin.getConfigManager().getCaptchaType().equals("ITEM")) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;

        Player player = (Player) event.getPlayer();
        if (plugin.getSessionManager().isAuthenticated(player))
            return;
        if (!plugin.getCaptchaManager().hasPendingCaptcha(player))
            return;
        if (!plugin.getConfigManager().getCaptchaType().equals("ITEM"))
            return;

        // Verify captcha on inventory close
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline())
                return;
            if (plugin.getSessionManager().isAuthenticated(player))
                return;

            MessageManager msg = plugin.getMessageManager();

            if (plugin.getCaptchaManager().verifyCaptcha(player, null)) {
                msg.sendMessage(player, "captcha.success");
                player.getInventory().clear();
                plugin.getEffectsManager().removeBossBar(player);
                plugin.getEffectsManager().playLoginSound(player);

                PlayerData data = plugin.getSessionManager().getPlayerData(player);
                if (data != null && data.isRegistered()) {
                    int delay = plugin.getConfigManager().getAutoKickDelay();
                    plugin.getEffectsManager().showLoginBossBar(player, delay);
                    player.sendMessage("");
                    player.sendMessage(msg.getMessage("captcha.next-step-login"));
                    player.sendMessage("");
                } else {
                    int delay = plugin.getConfigManager().getAutoKickDelay();
                    plugin.getEffectsManager().showRegisterBossBar(player, delay);
                    player.sendMessage("");
                    player.sendMessage(msg.getMessage("captcha.next-step-register"));
                    player.sendMessage("");
                }
            } else {
                msg.sendMessage(player, "captcha.failed");
                plugin.getEffectsManager().playErrorSound(player);
            }
        }, 1L);
    }
}
