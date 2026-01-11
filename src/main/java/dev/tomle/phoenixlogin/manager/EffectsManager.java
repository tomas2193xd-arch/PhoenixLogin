package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EffectsManager {

    private final PhoenixLogin plugin;
    private final Map<UUID, BossBar> activeBossBars;

    public EffectsManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        this.activeBossBars = new ConcurrentHashMap<>();
    }

    // === BOSS BAR ===

    public void showLoginBossBar(Player player, int timeRemaining) {
        if (!plugin.getConfigManager().isBossBarEnabled()) {
            return;
        }

        removeBossBar(player);

        String message = plugin.getMessageManager().getMessage("bossbar.please-login")
                .replace("{time}", String.valueOf(timeRemaining));

        BossBar bossBar = BossBar.bossBar(
                Component.text(message),
                1.0f,
                getBossBarColor(),
                getBossBarOverlay());

        plugin.adventure().player(player).showBossBar(bossBar);
        activeBossBars.put(player.getUniqueId(), bossBar);
    }

    public void showRegisterBossBar(Player player, int timeRemaining) {
        if (!plugin.getConfigManager().isBossBarEnabled()) {
            return;
        }

        removeBossBar(player);

        String message = plugin.getMessageManager().getMessage("bossbar.please-register")
                .replace("{time}", String.valueOf(timeRemaining));

        BossBar bossBar = BossBar.bossBar(
                Component.text(message),
                1.0f,
                getBossBarColor(),
                getBossBarOverlay());

        plugin.adventure().player(player).showBossBar(bossBar);
        activeBossBars.put(player.getUniqueId(), bossBar);
    }

    public void showCaptchaBossBar(Player player) {
        if (!plugin.getConfigManager().isBossBarEnabled()) {
            return;
        }

        removeBossBar(player);

        String message = plugin.getMessageManager().getMessage("bossbar.captcha");

        BossBar bossBar = BossBar.bossBar(
                Component.text(message),
                1.0f,
                BossBar.Color.YELLOW,
                getBossBarOverlay());

        plugin.adventure().player(player).showBossBar(bossBar);
        activeBossBars.put(player.getUniqueId(), bossBar);
    }

    public void updateBossBarProgress(Player player, float progress) {
        BossBar bossBar = activeBossBars.get(player.getUniqueId());
        if (bossBar != null) {
            bossBar.progress(Math.max(0.0f, Math.min(1.0f, progress)));
        }
    }

    public void removeBossBar(Player player) {
        BossBar bossBar = activeBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            plugin.adventure().player(player).hideBossBar(bossBar);
        }
    }

    private BossBar.Color getBossBarColor() {
        try {
            return BossBar.Color.valueOf(plugin.getConfigManager().getBossBarColor());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.RED;
        }
    }

    private BossBar.Overlay getBossBarOverlay() {
        String style = plugin.getConfigManager().getBossBarStyle();

        switch (style) {
            case "SEGMENTED_6":
                return BossBar.Overlay.NOTCHED_6;
            case "SEGMENTED_10":
                return BossBar.Overlay.NOTCHED_10;
            case "SEGMENTED_12":
                return BossBar.Overlay.NOTCHED_12;
            case "SEGMENTED_20":
                return BossBar.Overlay.NOTCHED_20;
            default:
                return BossBar.Overlay.PROGRESS;
        }
    }

    // === TITLES ===

    public void showWelcomeTitle(Player player) {
        if (!plugin.getConfigManager().isTitlesEnabled()) {
            return;
        }

        String titleText = plugin.getMessageManager().getMessage("titles.welcome.title");
        String subtitleText = plugin.getMessageManager().getMessage("titles.welcome.subtitle");

        showTitle(player, titleText, subtitleText);
    }

    public void showLoginSuccessTitle(Player player) {
        if (!plugin.getConfigManager().isTitlesEnabled()) {
            return;
        }

        String titleText = plugin.getMessageManager().getMessage("titles.login.title");
        String subtitleText = plugin.getMessageManager().getMessage("titles.login.subtitle");

        showTitle(player, titleText, subtitleText);
    }

    public void showRegisterSuccessTitle(Player player) {
        if (!plugin.getConfigManager().isTitlesEnabled()) {
            return;
        }

        String titleText = plugin.getMessageManager().getMessage("titles.register.title");
        String subtitleText = plugin.getMessageManager().getMessage("titles.register.subtitle");

        showTitle(player, titleText, subtitleText);
    }

    public void showErrorTitle(Player player, String errorMessage) {
        if (!plugin.getConfigManager().isTitlesEnabled()) {
            return;
        }

        String titleText = plugin.getMessageManager().getMessage("titles.error.title");
        String subtitleText = plugin.getMessageManager().getMessage("titles.error.subtitle")
                .replace("{message}", errorMessage);

        showTitle(player, titleText, subtitleText);
    }

    /**
     * Muestra título de CAPTCHA con instrucciones claras en pantalla
     */
    public void showCaptchaTitle(Player player) {
        if (!plugin.getConfigManager().isTitlesEnabled()) {
            return;
        }

        String titleText = "§6§l🔒 VERIFICACIÓN";
        String subtitleText = "§e§lMira el MAPA §7y usa §f/captcha <código>";

        // Mostrar con más duración para que lean las instrucciones
        Component titleComponent = Component.text(plugin.getMessageManager().colorize(titleText));
        Component subtitleComponent = Component.text(plugin.getMessageManager().colorize(subtitleText));

        Title.Times times = Title.Times.times(
                Duration.ofMillis(10 * 50),
                Duration.ofMillis(200 * 50), // 10 segundos de stay
                Duration.ofMillis(20 * 50));

        Title displayTitle = Title.title(titleComponent, subtitleComponent, times);
        plugin.adventure().player(player).showTitle(displayTitle);
    }

    private void showTitle(Player player, String title, String subtitle) {
        Component titleComponent = Component.text(plugin.getMessageManager().colorize(title));
        Component subtitleComponent = Component.text(plugin.getMessageManager().colorize(subtitle));

        Title.Times times = Title.Times.times(
                Duration.ofMillis(plugin.getConfigManager().getTitleFadeIn() * 50),
                Duration.ofMillis(plugin.getConfigManager().getTitleStay() * 50),
                Duration.ofMillis(plugin.getConfigManager().getTitleFadeOut() * 50));

        Title displayTitle = Title.title(titleComponent, subtitleComponent, times);
        plugin.adventure().player(player).showTitle(displayTitle);
    }

    // === SOUNDS ===

    public void playLoginSound(Player player) {
        try {
            Sound sound = Sound.valueOf(plugin.getConfigManager().getSoundOnLogin());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger()
                    .warning("Invalid sound configured for login: " + plugin.getConfigManager().getSoundOnLogin());
        }
    }

    public void playRegisterSound(Player player) {
        try {
            Sound sound = Sound.valueOf(plugin.getConfigManager().getSoundOnRegister());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(
                    "Invalid sound configured for register: " + plugin.getConfigManager().getSoundOnRegister());
        }
    }

    public void playErrorSound(Player player) {
        try {
            Sound sound = Sound.valueOf(plugin.getConfigManager().getSoundOnError());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger()
                    .warning("Invalid sound configured for error: " + plugin.getConfigManager().getSoundOnError());
        }
    }

    // === PARTICLES ===

    public void playLoginParticles(Player player) {
        // Reducido de 20 a 50 para un efecto más suave (TOTEM es rápido) o 5 para
        // FIREWORKS
        // Si el usuario quiere "menos cohetes", podemos asumir que FIREWORKS_SPARK es
        // mejor en menor cantidad
        // O simplemente spawnear un solo Firework real.
        // Por ahora, reduciremos la cantidad de partículas del config.
        spawnParticles(player, plugin.getConfigManager().getParticleOnLogin(), 5); // Reducido drásticamente de 20 (o
                                                                                   // 100 si era FIREWORKS)

        // Opcional: Lanzar un solo fuego artificial pequeño
        spawnElegantFirework(player);
    }

    private void spawnElegantFirework(Player player) {
        Location loc = player.getLocation();
        org.bukkit.entity.Firework fw = (org.bukkit.entity.Firework) loc.getWorld().spawnEntity(loc,
                org.bukkit.entity.EntityType.FIREWORK);
        org.bukkit.inventory.meta.FireworkMeta fwm = fw.getFireworkMeta();

        // Un solo efecto elegante: Bola pequeña, Aqua y Blanco
        org.bukkit.FireworkEffect effect = org.bukkit.FireworkEffect.builder()
                .with(org.bukkit.FireworkEffect.Type.BALL)
                .withColor(org.bukkit.Color.AQUA)
                .withFade(org.bukkit.Color.WHITE)
                .flicker(true)
                .build();

        fwm.addEffect(effect);
        fwm.setPower(0); // Altura mínima para que explote cerca pero no dañe (power 0 o 1)
        fw.setFireworkMeta(fwm);

        // Detonar casi instantáneamente (1 tick después)
        plugin.getServer().getScheduler().runTaskLater(plugin, fw::detonate, 2L);
    }

    public void playErrorParticles(Player player) {
        spawnParticles(player, plugin.getConfigManager().getParticleOnError(), 10);
    }

    private void spawnParticles(Player player, String particleType, int count) {
        try {
            Particle particle = Particle.valueOf(particleType);
            Location loc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(particle, loc, count, 0.5, 0.5, 0.5, 0.1);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type: " + particleType);
        }
    }

    // === CLEANUP ===

    public void cleanup(Player player) {
        removeBossBar(player);
    }

    public void cleanupAll() {
        activeBossBars.clear();
    }
}
