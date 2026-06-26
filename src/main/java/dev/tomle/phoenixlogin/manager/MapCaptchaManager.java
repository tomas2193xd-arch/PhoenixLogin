package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages map-based captcha with visual code rendering.
 */
public class MapCaptchaManager {

    private final PhoenixLogin plugin;
    private final Map<UUID, String> activeCaptchas = new ConcurrentHashMap<>();
    private final Map<UUID, Long> captchaExpiry = new ConcurrentHashMap<>();

    private static final int CAPTCHA_LENGTH = 6;
    private static final long CAPTCHA_EXPIRY_TIME = 60000;
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public MapCaptchaManager(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    public String createCaptcha(Player player) {
        String code = generateCaptchaCode();

        activeCaptchas.put(player.getUniqueId(), code);
        captchaExpiry.put(player.getUniqueId(), System.currentTimeMillis() + CAPTCHA_EXPIRY_TIME);

        showCaptchaToPlayer(player, code);

        return code;
    }

    public boolean verifyCaptcha(Player player, String input) {
        UUID uuid = player.getUniqueId();

        if (!activeCaptchas.containsKey(uuid))
            return false;

        Long expiry = captchaExpiry.get(uuid);
        if (expiry != null && System.currentTimeMillis() > expiry) {
            removeCaptcha(player);
            return false;
        }

        String correctCode = activeCaptchas.get(uuid);
        boolean correct = correctCode.equalsIgnoreCase(input.trim());

        if (correct) {
            removeCaptcha(player);
        }

        return correct;
    }

    public void removeCaptcha(Player player) {
        UUID uuid = player.getUniqueId();
        activeCaptchas.remove(uuid);
        captchaExpiry.remove(uuid);
    }

    public boolean hasCaptcha(Player player) {
        return activeCaptchas.containsKey(player.getUniqueId());
    }

    private String generateCaptchaCode() {
        // Use SecureRandom instead of predictable Random
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        StringBuilder code = new StringBuilder(CAPTCHA_LENGTH);
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            code.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

    @SuppressWarnings("deprecation")
    private void showCaptchaToPlayer(Player player, String code) {
        MessageManager msg = plugin.getMessageManager();

        MapView mapView = Bukkit.createMap(player.getWorld());
        for (MapRenderer renderer : mapView.getRenderers()) {
            mapView.removeRenderer(renderer);
        }
        mapView.addRenderer(new CaptchaRenderer(code));

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        meta.setMapView(mapView);
        meta.setDisplayName(msg.getMessage("captcha.map-title"));
        List<String> lore = new ArrayList<>();
        lore.add(msg.getMessage("captcha.map-lore-1"));
        lore.add(msg.getMessage("captcha.map-lore-2"));
        meta.setLore(lore);
        mapItem.setItemMeta(meta);

        player.getInventory().clear();
        player.getInventory().setItem(0, mapItem);
        player.updateInventory();

        plugin.getEffectsManager().showCaptchaTitle(player);

        player.sendMessage("");
        player.sendMessage(msg.getMessage("captcha.map-chat-header"));
        player.sendMessage(msg.getMessage("captcha.map-chat-info"));
        player.sendMessage(msg.getMessage("captcha.map-chat-usage"));
        player.sendMessage(msg.getMessage("captcha.map-chat-header"));
        player.sendMessage("");
    }

    /**
     * Custom map renderer for captcha codes.
     */
    private static class CaptchaRenderer extends MapRenderer {

        private final String code;
        private boolean rendered = false;

        public CaptchaRenderer(String code) {
            super(true);
            this.code = code;
        }

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            if (rendered)
                return;

            BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            g.setColor(new Color(45, 52, 54));
            g.fillRect(0, 0, 128, 128);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString("VERIFICATION CODE", 15, 20);

            g.setFont(new Font("Monospaced", Font.BOLD, 20));
            g.setColor(Color.GREEN);
            g.drawString(code, 20, 70);

            g.dispose();

            canvas.drawImage(0, 0, MapPalette.resizeImage(image));
            rendered = true;
        }
    }
}
