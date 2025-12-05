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

public class CaptchaListener implements Listener {

    private final PhoenixLogin plugin;

    public CaptchaListener(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Verificar si tiene un captcha pendiente
        if (!plugin.getCaptchaManager().hasPendingCaptcha(player)) {
            return;
        }

        // Verificar si es un captcha de tipo ITEM
        if (!plugin.getConfigManager().getCaptchaType().equals("ITEM")) {
            return;
        }

        // Permitir clicks en el inventario para resolver el captcha
        // El jugador debe arrastrar el item al slot correcto

        // Esperar un tick para que el item se mueva
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getCaptchaManager().verifyCaptcha(player, null)) {
                handleCaptchaSuccess(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        // Si cierra el inventario con captcha pendiente, verificar
        if (plugin.getCaptchaManager().hasPendingCaptcha(player)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getCaptchaManager().verifyCaptcha(player, null)) {
                    handleCaptchaSuccess(player);
                } else {
                    // Si falla, regenerar captcha
                    handleCaptchaFailure(player);
                }
            }, 1L);
        }
    }

    private void handleCaptchaSuccess(Player player) {
        MessageManager msg = plugin.getMessageManager();

        // Mensaje de éxito
        msg.sendMessage(player, "captcha.success");
        plugin.getEffectsManager().playLoginSound(player);
        plugin.getEffectsManager().removeBossBar(player);

        // Limpiar inventario
        player.getInventory().clear();

        // Mostrar mensaje apropiado según si está registrado o no
        PlayerData data = plugin.getSessionManager().getPlayerData(player);
        boolean isRegistered = (data != null && data.isRegistered());

        if (isRegistered) {
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

        plugin.getLogger().info(player.getName() + " completed the captcha successfully.");
    }

    private void handleCaptchaFailure(Player player) {
        MessageManager msg = plugin.getMessageManager();

        // Mensaje de error
        msg.sendMessage(player, "captcha.failed");
        plugin.getEffectsManager().playErrorSound(player);

        // Regenerar captcha
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getCaptchaManager().generateCaptcha(player);
        }, 20L);
    }
}
