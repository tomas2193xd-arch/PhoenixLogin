package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages map-based captcha system.
 * Displays a verification code on a Minecraft map that players must type to
 * pass the captcha.
 */
public class MapCaptchaManager {

    private final PhoenixLogin plugin;
    private final Map<UUID, String> activeCaptchas = new ConcurrentHashMap<>();
    private final Map<UUID, Long> captchaExpiry = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // Captcha configuration
    private static final int CAPTCHA_LENGTH = 6;
    private static final long CAPTCHA_EXPIRY_TIME = 60000; // 60 seconds
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Excluding confusing chars

    public MapCaptchaManager(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates and sends a captcha to the player.
     *
     * @param player The player to send the captcha to
     * @return The generated captcha code
     */
    public String createCaptcha(Player player) {
        // Generate random code
        String code = generateCaptchaCode();

        // Store captcha
        activeCaptchas.put(player.getUniqueId(), code);
        captchaExpiry.put(player.getUniqueId(), System.currentTimeMillis() + CAPTCHA_EXPIRY_TIME);

        // Create and give map to player
        giveCaptchaMap(player, code);

        return code;
    }

    /**
     * Verifies if the player's input matches their captcha.
     *
     * @param player The player
     * @param input  The input to verify
     * @return true if the captcha is correct, false otherwise
     */
    public boolean verifyCaptcha(Player player, String input) {
        UUID uuid = player.getUniqueId();

        // Check if captcha exists
        if (!activeCaptchas.containsKey(uuid)) {
            return false;
        }

        // Check if captcha expired
        Long expiry = captchaExpiry.get(uuid);
        if (expiry != null && System.currentTimeMillis() > expiry) {
            removeCaptcha(player);
            return false;
        }

        // Verify code (case insensitive)
        String correctCode = activeCaptchas.get(uuid);
        boolean correct = correctCode.equalsIgnoreCase(input.trim());

        if (correct) {
            removeCaptcha(player);
        }

        return correct;
    }

    /**
     * Removes the captcha for a player.
     *
     * @param player The player
     */
    public void removeCaptcha(Player player) {
        UUID uuid = player.getUniqueId();
        activeCaptchas.remove(uuid);
        captchaExpiry.remove(uuid);
    }

    /**
     * Checks if a player has an active captcha.
     *
     * @param player The player
     * @return true if the player has an active captcha
     */
    public boolean hasCaptcha(Player player) {
        return activeCaptchas.containsKey(player.getUniqueId());
    }

    /**
     * Generates a random captcha code.
     *
     * @return The generated code
     */
    private String generateCaptchaCode() {
        StringBuilder code = new StringBuilder(CAPTCHA_LENGTH);
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

    /**
     * Creates and gives a captcha map to the player.
     *
     * @param player The player
     * @param code   The captcha code to display
     */
    private void giveCaptchaMap(Player player, String code) {
        // Create map item
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();

        // Create map view
        MapView mapView = Bukkit.createMap(player.getWorld());

        // Clear default renderers
        mapView.getRenderers().forEach(mapView::removeRenderer);

        // Add custom captcha renderer
        mapView.addRenderer(new CaptchaRenderer(code));

        // Set map view to item
        meta.setMapView(mapView);
        mapItem.setItemMeta(meta);

        // Clear inventory and give map
        player.getInventory().clear();
        player.getInventory().setItem(0, mapItem);
        player.getInventory().setHeldItemSlot(0);

        // Send messages
        player.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§6§lVERIFICACIÓN CAPTCHA");
        player.sendMessage("");
        player.sendMessage("§7Mira el mapa que tienes en tu mano.");
        player.sendMessage("§7Escribe el código que ves en el chat.");
        player.sendMessage("");
        player.sendMessage("§eEjemplo: §f/login " + code.substring(0, 3) + "...");
        player.sendMessage("§7Tienes §c60 segundos§7 para completar el captcha.");
        player.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * Custom map renderer for displaying captcha codes.
     */
    private static class CaptchaRenderer extends MapRenderer {

        private final String code;
        private boolean rendered = false;

        public CaptchaRenderer(String code) {
            super(true); // Contextual rendering
            this.code = code;
        }

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            if (rendered) {
                return;
            }

            // Create captcha image
            BufferedImage image = createCaptchaImage(code);

            // Draw image on map
            canvas.drawImage(0, 0, image);

            rendered = true;
        }

        /**
         * Creates a captcha image with the code.
         *
         * @param code The code to display
         * @return The generated image
         */
        private BufferedImage createCaptchaImage(String code) {
            // Map size is 128x128
            BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // Enable anti-aliasing
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Background with gradient
            GradientPaint gradient = new GradientPaint(0, 0, new Color(45, 52, 54),
                    128, 128, new Color(99, 110, 114));
            g.setPaint(gradient);
            g.fillRect(0, 0, 128, 128);

            // Add noise (dots)
            Random random = new Random();
            for (int i = 0; i < 200; i++) {
                int x = random.nextInt(128);
                int y = random.nextInt(128);
                int size = random.nextInt(2) + 1;
                g.setColor(new Color(random.nextInt(100) + 100,
                        random.nextInt(100) + 100,
                        random.nextInt(100) + 100, 100));
                g.fillOval(x, y, size, size);
            }

            // Add random lines
            g.setStroke(new BasicStroke(2));
            for (int i = 0; i < 5; i++) {
                g.setColor(new Color(random.nextInt(150) + 50,
                        random.nextInt(150) + 50,
                        random.nextInt(150) + 50, 50));
                int x1 = random.nextInt(128);
                int y1 = random.nextInt(128);
                int x2 = random.nextInt(128);
                int y2 = random.nextInt(128);
                g.drawLine(x1, y1, x2, y2);
            }

            // Draw title
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.setColor(Color.WHITE);
            String title = "VERIFICATION";
            FontMetrics fmTitle = g.getFontMetrics();
            int titleWidth = fmTitle.stringWidth(title);
            g.drawString(title, (128 - titleWidth) / 2, 20);

            // Draw captcha code with variations
            g.setFont(new Font("Monospaced", Font.BOLD, 24));
            FontMetrics fm = g.getFontMetrics();
            int totalWidth = fm.stringWidth(code);
            int startX = (128 - totalWidth) / 2;
            int y = 75;

            // Draw each character with slight variations
            for (int i = 0; i < code.length(); i++) {
                char c = code.charAt(i);
                String character = String.valueOf(c);

                // Random rotation
                double rotation = (random.nextDouble() - 0.5) * 0.3;
                g.rotate(rotation, startX, y);

                // Random color (bright colors for readability)
                g.setColor(new Color(200 + random.nextInt(55),
                        200 + random.nextInt(55),
                        200 + random.nextInt(55)));

                // Draw character
                g.drawString(character, startX, y);

                // Reset rotation
                g.rotate(-rotation, startX, y);

                // Move to next position
                startX += fm.stringWidth(character);
            }

            // Draw bottom instruction
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            g.setColor(Color.LIGHT_GRAY);
            String instruction = "Type this code in chat";
            FontMetrics fmInst = g.getFontMetrics();
            int instWidth = fmInst.stringWidth(instruction);
            g.drawString(instruction, (128 - instWidth) / 2, 110);

            g.dispose();
            return image;
        }
    }
}
