package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CaptchaManager {

    private final PhoenixLogin plugin;
    private final Random random;
    private final Map<UUID, CaptchaData> activeCaptchas;
    private final MapCaptchaManager mapCaptchaManager;

    private static final Material[] CAPTCHA_ITEMS = {
            Material.EMERALD,
            Material.DIAMOND,
            Material.GOLD_INGOT,
            Material.IRON_INGOT,
            Material.REDSTONE,
            Material.COAL,
            Material.LAPIS_LAZULI,
            Material.QUARTZ
    };

    public CaptchaManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.activeCaptchas = new ConcurrentHashMap<>();
        this.mapCaptchaManager = new MapCaptchaManager(plugin);
    }

    public boolean isCaptchaRequired() {
        return plugin.getConfigManager().isCaptchaEnabled();
    }

    public void generateCaptcha(Player player) {
        String type = plugin.getConfigManager().getCaptchaType();
        CaptchaData captcha = null;

        switch (type) {
            case "ITEM":
                captcha = generateItemCaptcha();
                break;
            case "MATH":
                captcha = generateMathCaptcha();
                break;
            case "MAP":
                generateMapCaptcha(player);
                return;
            default:
                captcha = generateItemCaptcha();
        }

        activeCaptchas.put(player.getUniqueId(), captcha);
        displayCaptcha(player, captcha);
    }

    private void generateMapCaptcha(Player player) {
        mapCaptchaManager.createCaptcha(player);
        plugin.getEffectsManager().showCaptchaBossBar(player);
    }

    private CaptchaData generateItemCaptcha() {
        Material requiredItem = CAPTCHA_ITEMS[random.nextInt(CAPTCHA_ITEMS.length)];
        int targetSlot = random.nextInt(9);
        return new CaptchaData(CaptchaType.ITEM, requiredItem, targetSlot);
    }

    private CaptchaData generateMathCaptcha() {
        String difficulty = plugin.getConfigManager().getCaptchaMathDifficulty();
        int num1, num2;

        switch (difficulty) {
            case "HARD":
                num1 = random.nextInt(50) + 10;
                num2 = random.nextInt(50) + 10;
                break;
            case "MEDIUM":
                num1 = random.nextInt(20) + 5;
                num2 = random.nextInt(20) + 5;
                break;
            default: // EASY
                num1 = random.nextInt(10) + 1;
                num2 = random.nextInt(10) + 1;
        }

        int operation = random.nextInt(2); // 0 = add, 1 = subtract
        String question;
        int answer;

        if (operation == 0) {
            question = num1 + " + " + num2;
            answer = num1 + num2;
        } else {
            if (num1 < num2) {
                int temp = num1;
                num1 = num2;
                num2 = temp;
            }
            question = num1 + " - " + num2;
            answer = num1 - num2;
        }

        return new CaptchaData(CaptchaType.MATH, question, answer);
    }

    private void displayCaptcha(Player player, CaptchaData captcha) {
        MessageManager msg = plugin.getMessageManager();

        if (captcha.getType() == CaptchaType.ITEM) {
            Map<String, String> placeholders = MessageManager.createPlaceholders(
                    "item", captcha.getRequiredItem().toString().toLowerCase().replace("_", " "),
                    "slot", String.valueOf(captcha.getTargetSlot() + 1));

            msg.sendMessage(player, "captcha.item-instruction", placeholders);

            player.getInventory().clear();
            player.getInventory().addItem(new ItemStack(captcha.getRequiredItem(), 1));
            player.updateInventory();

        } else if (captcha.getType() == CaptchaType.MATH) {
            Map<String, String> placeholders = MessageManager.createPlaceholders(
                    "question", captcha.getQuestion());

            msg.sendMessage(player, "captcha.math-instruction", placeholders);
        }

        plugin.getEffectsManager().showCaptchaBossBar(player);
    }

    public boolean verifyCaptcha(Player player, Object answer) {
        // Check MAP captcha first
        if (mapCaptchaManager.hasCaptcha(player)) {
            return mapCaptchaManager.verifyCaptcha(player, answer.toString());
        }

        CaptchaData captcha = activeCaptchas.get(player.getUniqueId());
        if (captcha == null)
            return false;

        boolean success = false;

        if (captcha.getType() == CaptchaType.ITEM) {
            ItemStack item = player.getInventory().getItem(captcha.getTargetSlot());
            if (item != null && item.getType() == captcha.getRequiredItem()) {
                success = true;
            }
        } else if (captcha.getType() == CaptchaType.MATH) {
            try {
                int givenAnswer = Integer.parseInt(answer.toString());
                if (givenAnswer == captcha.getAnswer()) {
                    success = true;
                }
            } catch (NumberFormatException e) {
                success = false;
            }
        }

        if (success) {
            activeCaptchas.remove(player.getUniqueId());
            player.getInventory().clear();
        }

        return success;
    }

    public boolean hasPendingCaptcha(Player player) {
        return activeCaptchas.containsKey(player.getUniqueId()) || mapCaptchaManager.hasCaptcha(player);
    }

    public void removeCaptcha(Player player) {
        activeCaptchas.remove(player.getUniqueId());
        mapCaptchaManager.removeCaptcha(player);
    }

    /**
     * Clears all captcha items from a player's inventory.
     * Call this BEFORE restoring the original inventory.
     */
    public void clearCaptchaItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        removeCaptcha(player);
    }

    public boolean hasCaptcha(Player player) {
        return hasPendingCaptcha(player);
    }

    // === INNER CLASSES ===

    private enum CaptchaType {
        ITEM, MATH, MAP
    }

    private static class CaptchaData {
        private final CaptchaType type;
        private Material requiredItem;
        private int targetSlot;
        private String question;
        private int answer;

        // ITEM constructor
        public CaptchaData(CaptchaType type, Material requiredItem, int targetSlot) {
            this.type = type;
            this.requiredItem = requiredItem;
            this.targetSlot = targetSlot;
        }

        // MATH constructor
        public CaptchaData(CaptchaType type, String question, int answer) {
            this.type = type;
            this.question = question;
            this.answer = answer;
        }

        public CaptchaType getType() {
            return type;
        }

        public Material getRequiredItem() {
            return requiredItem;
        }

        public int getTargetSlot() {
            return targetSlot;
        }

        public String getQuestion() {
            return question;
        }

        public int getAnswer() {
            return answer;
        }
    }
}
