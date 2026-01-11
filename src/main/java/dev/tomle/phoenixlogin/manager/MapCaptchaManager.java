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
 * Manages map-based captcha system with visual code rendering.
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

        if (!activeCaptchas.containsKey(uuid)) {
            return false;
        }

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
        Random secureRandom = new Random(System.nanoTime());
        StringBuilder code = new StringBuilder(CAPTCHA_LENGTH);
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            code.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

    @SuppressWarnings("deprecation")
    private void showCaptchaToPlayer(Player player, String code) {
        // Create map with code
        MapView mapView = Bukkit.createMap(player.getWorld());

        // Clear default renderers
        for (MapRenderer renderer : mapView.getRenderers()) {
            mapView.removeRenderer(renderer);
        }

        // Add our renderer
        mapView.addRenderer(new CaptchaRenderer(code));

        // Create map item
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        meta.setMapView(mapView);
        meta.setDisplayName("§6§lVERIFICATION CODE");
        List<String> lore = new ArrayList<>();
        lore.add("§7Look at the map");
        lore.add("§7and use §f/captcha <code>");
        meta.setLore(lore);
        mapItem.setItemMeta(meta);

        // Give map to player
        player.getInventory().clear();
        player.getInventory().setItem(0, mapItem);
        player.updateInventory();

        // 🎯 MOSTRAR TÍTULO EN PANTALLA (más visible que chat)
        plugin.getEffectsManager().showCaptchaTitle(player);

        // Mensaje de chat minimalista (el título ya explica todo)
        player.sendMessage("");
        player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§6§l⚠ ANTI-BOT §8- §7Mira el §fMAPA §7en tu mano");
        player.sendMessage("§7Usa: §f/captcha <código> §8| §c⏱ 60s");
        player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("");
    }

    /**
     * Custom renderer for captcha maps
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
            if (rendered) {
                return;
            }

            // Create image
            BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // Background
            g.setColor(new Color(45, 52, 54));
            g.fillRect(0, 0, 128, 128);

            // Title
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            String title = "VERIFICATION CODE";
            g.drawString(title, 15, 20);

            // Big code
            g.setFont(new Font("Monospaced", Font.BOLD, 20));
            g.setColor(Color.GREEN);
            g.drawString(code, 20, 70);

            g.dispose();

            // Draw on canvas
            canvas.drawImage(0, 0, MapPalette.resizeImage(image));

            rendered = true;
        }
    }
}
