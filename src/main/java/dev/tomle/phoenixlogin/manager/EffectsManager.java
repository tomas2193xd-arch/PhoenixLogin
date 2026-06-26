package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EffectsManager {

    private final PhoenixLogin plugin;
    private final Map<UUID, BossBar> activeBossBars;

    // Metadata key used to identify our fireworks 
    public static final String FIREWORK_META_KEY = "phoenixlogin_firework";

    public EffectsManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        this.activeBossBars = new ConcurrentHashMap<>();
    }

    // === BOSS BAR ===

    public void showLoginBossBar(Player player, int timeRemaining) {
        if (!plugin.getConfigManager().isBossBarEnabled())
            return;
        removeBossBar(player);

        String message = plugin.getMessageManager().getMessage("bossbar.please-login")
                .replace("{time}", String.valueOf(timeRemaining));

        BossBar bossBar = BossBar.bossBar(
                Component.text(message), 1.0f, getBossBarColor(), getBossBarOverlay());

        plugin.adventure().player(player).showBossBar(bossBar);
        activeBossBars.put(player.getUniqueId(), bossBar);
    }

    public void showRegisterBossBar(Player player, int timeRemaining) {
        if (!plugin.getConfigManager().isBossBarEnabled())
            return;
        removeBossBar(player);

        String message = plugin.getMessageManager().getMessage("bossbar.please-register")
                .replace("{time}", String.valueOf(timeRemaining));

        BossBar bossBar = BossBar.bossBar(
                Component.text(message), 1.0f, getBossBarColor(), getBossBarOverlay());

        plugin.adventure().player(player).showBossBar(bossBar);
        activeBossBars.put(player.getUniqueId(), bossBar);
    }

    public void showCaptchaBossBar(Player player) {
        if (!plugin.getConfigManager().isBossBarEnabled())
            return;
        removeBossBar(player);

        String message = plugin.getMessageManager().getMessage("bossbar.captcha");

        BossBar bossBar = BossBar.bossBar(
                Component.text(message), 1.0f, BossBar.Color.YELLOW, getBossBarOverlay());

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
        if (!plugin.getConfigManager().isTitlesEnabled())
            return;
        String titleText = plugin.getMessageManager().getMessage("titles.welcome.title");
        String subtitleText = plugin.getMessageManager().getMessage("titles.welcome.subtitle");
        showTitle(player, titleText, subtitleText);
    }

    public void showLoginSuccessTitle(Player player) {
        if (!plugin.getConfigManager().isTitlesEnabled())
            return;
        String titleText = plugin.getMessageManager().getMessage("titles.login.title");
        String subtitleText = plugin.getMessageManager().getMessage("titles.login.subtitle");
        showTitle(player, titleText, subtitleText);
    }

    public void showRegisterSuccessTitle(Player player) {
        if (!plugin.getConfigManager().isTitlesEnabled())
            return;
        String titleText = plugin.getMessageManager().getMessage("titles.register.title");
        String subtitleText = plugin.getMessageManager().getMessage("titles.register.subtitle");
        showTitle(player, titleText, subtitleText);
    }

    public void showErrorTitle(Player player, String errorMessage) {
        if (!plugin.getConfigManager().isTitlesEnabled())
            return;
        String titleText = plugin.getMessageManager().getMessage("titles.error.title");
        String subtitleText = plugin.getMessageManager().getMessage("titles.error.subtitle")
                .replace("{message}", errorMessage);
        showTitle(player, titleText, subtitleText);
    }

    public void showCaptchaTitle(Player player) {
        // Captcha info is shown via chat only — no title needed
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
        playSound(player, plugin.getConfigManager().getSoundOnLogin());
    }

    public void playRegisterSound(Player player) {
        playSound(player, plugin.getConfigManager().getSoundOnRegister());
    }

    public void playErrorSound(Player player) {
        playSound(player, plugin.getConfigManager().getSoundOnError());
    }

    private void playSound(Player player, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName);
        }
    }

    // === PARTICLES ===

    public void playLoginParticles(Player player) {
        spawnParticles(player, plugin.getConfigManager().getParticleOnLogin(), 5);
        spawnElegantFirework(player);
    }

    /**
     * Firework is tagged with metadata so ProtectionListener can cancel
     * its damage.
     * Also scheduled to detonate after 2 ticks to rise slightly.
     */
    private void spawnElegantFirework(Player player) {
        Location loc = player.getLocation().add(0, 1, 0); // Spawn slightly above player
        org.bukkit.entity.Firework fw = (org.bukkit.entity.Firework) loc.getWorld().spawnEntity(loc,
                org.bukkit.entity.EntityType.FIREWORK);
        org.bukkit.inventory.meta.FireworkMeta fwm = fw.getFireworkMeta();

        org.bukkit.FireworkEffect effect = org.bukkit.FireworkEffect.builder()
                .with(org.bukkit.FireworkEffect.Type.BALL)
                .withColor(org.bukkit.Color.AQUA)
                .withFade(org.bukkit.Color.WHITE)
                .flicker(true)
                .build();

        fwm.addEffect(effect);
        fwm.setPower(0);
        fw.setFireworkMeta(fwm);

        // Tag the firework so we can cancel its damage in ProtectionListener
        fw.setMetadata(FIREWORK_META_KEY, new FixedMetadataValue(plugin, true));

        // Make firework silent and detonating quickly
        fw.setSilent(true);
        plugin.getServer().getScheduler().runTaskLater(plugin, fw::detonate, 2L);
    }

    public void playErrorParticles(Player player) {
        spawnParticles(player, plugin.getConfigManager().getParticleOnError(), 10);
    }

    /**
     * Tries the configured particle name, then common alternatives for
     * compatibility.
     */
    private void spawnParticles(Player player, String particleType, int count) {
        Location loc = player.getLocation().add(0, 1, 0);

        // Try the configured name first
        if (trySpawnParticle(player, loc, particleType, count))
            return;

        // Fallback mappings for version compatibility
        switch (particleType.toUpperCase()) {
            case "VILLAGER_HAPPY":
                if (trySpawnParticle(player, loc, "HAPPY_VILLAGER", count))
                    return;
                break;
            case "HAPPY_VILLAGER":
                if (trySpawnParticle(player, loc, "VILLAGER_HAPPY", count))
                    return;
                break;
            case "VILLAGER_ANGRY":
                if (trySpawnParticle(player, loc, "ANGRY_VILLAGER", count))
                    return;
                break;
            case "ANGRY_VILLAGER":
                if (trySpawnParticle(player, loc, "VILLAGER_ANGRY", count))
                    return;
                break;
        }

        // If nothing works, just log once
        plugin.getLogger().warning("Could not spawn particle: " + particleType + " (not available in this version)");
    }

    private boolean trySpawnParticle(Player player, Location loc, String name, int count) {
        try {
            Particle particle = Particle.valueOf(name);
            player.getWorld().spawnParticle(particle, loc, count, 0.5, 0.5, 0.5, 0.1);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // === CLEANUP ===

    public void cleanup(Player player) {
        removeBossBar(player);
    }

    public void cleanupAll() {
        activeBossBars.clear();
    }

    // === PLAYER VISIBILITY ===

    public void hidePlayers(Player player) {
        if (!plugin.getConfigManager().isHidePlayers())
            return;
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            player.hidePlayer(plugin, online);
            online.hidePlayer(plugin, player);
        }
    }

    public void showPlayers(Player player) {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            player.showPlayer(plugin, online);
            online.showPlayer(plugin, player);
        }
    }
}
