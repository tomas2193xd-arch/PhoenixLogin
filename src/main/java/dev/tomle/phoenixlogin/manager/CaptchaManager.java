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

    // Lista de items posibles para captcha
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
            default:
                captcha = generateItemCaptcha();
        }

        activeCaptchas.put(player.getUniqueId(), captcha);
        displayCaptcha(player, captcha);
    }

    private CaptchaData generateItemCaptcha() {
        // Elegir item aleatorio
        Material requiredItem = CAPTCHA_ITEMS[random.nextInt(CAPTCHA_ITEMS.length)];

        // Elegir slot aleatorio (0-8 para hotbar)
        int targetSlot = random.nextInt(9);

        return new CaptchaData(CaptchaType.ITEM, requiredItem, targetSlot);
    }

    private CaptchaData generateMathCaptcha() {
        String difficulty = plugin.getConfigManager().getCaptchaMathDifficulty();

        int num1, num2, answer;
        String question;

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

        int operation = random.nextInt(2); // 0 = suma, 1 = resta

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
                    "slot", String.valueOf(captcha.getTargetSlot() + 1)); // +1 porque slots son 0-8 pero mostramos 1-9

            msg.sendMessage(player, "captcha.item-instruction", placeholders);

            // Limpiar inventario y dar el item
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
        CaptchaData captcha = activeCaptchas.get(player.getUniqueId());

        if (captcha == null) {
            return false;
        }

        boolean success = false;

        if (captcha.getType() == CaptchaType.ITEM) {
            // Verificar si el item está en el slot correcto
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
        return activeCaptchas.containsKey(player.getUniqueId());
    }

    public void removeCaptcha(Player player) {
        activeCaptchas.remove(player.getUniqueId());
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

        // Constructor para ITEM captcha
        public CaptchaData(CaptchaType type, Material requiredItem, int targetSlot) {
            this.type = type;
            this.requiredItem = requiredItem;
            this.targetSlot = targetSlot;
        }

        // Constructor para MATH captcha
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
