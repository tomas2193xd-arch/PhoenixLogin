package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final PhoenixLogin plugin;
    private FileConfiguration config;

    public ConfigManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // Language
    public String getLanguage() {
        return config.getString("language", "en");
    }

    // Database
    public String getDatabaseType() {
        return config.getString("database.type", "SQLITE").toUpperCase();
    }

    public String getMySQLHost() {
        return config.getString("database.mysql.host", "localhost");
    }

    public int getMySQLPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return config.getString("database.mysql.database", "phoenixlogin");
    }

    public String getMySQLUsername() {
        return config.getString("database.mysql.username", "root");
    }

    public String getMySQLPassword() {
        return config.getString("database.mysql.password", "password");
    }

    public int getMySQLPoolSize() {
        return config.getInt("database.mysql.pool-size", 10);
    }

    // Security - Password
    public int getMinPasswordLength() {
        return config.getInt("security.password.min-length", 4);
    }

    public int getMaxPasswordLength() {
        return config.getInt("security.password.max-length", 32);
    }

    public boolean isPasswordRequireUppercase() {
        return config.getBoolean("security.password.require-uppercase", false);
    }

    public boolean isPasswordRequireNumbers() {
        return config.getBoolean("security.password.require-numbers", false);
    }

    public boolean isPasswordRequireSpecial() {
        return config.getBoolean("security.password.require-special", false);
    }

    // Security - Brute Force
    public int getMaxLoginAttempts() {
        return config.getInt("security.max-login-attempts", 3);
    }

    public int getLockoutDuration() {
        return config.getInt("security.lockout-duration", 300);
    }

    // Security - Sessions
    public boolean isSessionsEnabled() {
        return config.getBoolean("security.sessions.enabled", true);
    }

    public int getSessionDuration() {
        return config.getInt("security.sessions.duration", 86400);
    }

    public boolean isRememberIP() {
        return config.getBoolean("security.sessions.remember-ip", true);
    }

    // Captcha
    public boolean isCaptchaEnabled() {
        return config.getBoolean("captcha.enabled", true);
    }

    public String getCaptchaType() {
        return config.getString("captcha.type", "ITEM").toUpperCase();
    }

    public String getCaptchaItemType() {
        return config.getString("captcha.item.required-item", "EMERALD").toUpperCase();
    }

    public int getCaptchaTargetSlot() {
        return config.getInt("captcha.item.target-slot", 4);
    }

    public String getCaptchaMathDifficulty() {
        return config.getString("captcha.math.difficulty", "EASY").toUpperCase();
    }

    // Login
    public boolean isFreezePlayer() {
        return config.getBoolean("login.freeze-player", true);
    }

    public boolean isAutoKickEnabled() {
        return config.getBoolean("login.auto-kick.enabled", true);
    }

    public int getAutoKickDelay() {
        return config.getInt("login.auto-kick.delay", 60);
    }

    public boolean isBlockMovement() {
        return config.getBoolean("login.block-movement", true);
    }

    public boolean isBlockInteract() {
        return config.getBoolean("login.block-interact", true);
    }

    public boolean isBlockDamage() {
        return config.getBoolean("login.block-damage", true);
    }

    public boolean isBlockCommands() {
        return config.getBoolean("login.block-commands", true);
    }

    public boolean isTeleportToSpawn() {
        return config.getBoolean("login.teleport-to-spawn", false);
    }

    public Location getSpawnLocation() {
        if (!isTeleportToSpawn()) {
            return null;
        }

        String worldName = config.getString("login.spawn-location.world");
        if (worldName == null) {
            return null;
        }

        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = config.getDouble("login.spawn-location.x", 0);
        double y = config.getDouble("login.spawn-location.y", 64);
        double z = config.getDouble("login.spawn-location.z", 0);
        float yaw = (float) config.getDouble("login.spawn-location.yaw", 0);
        float pitch = (float) config.getDouble("login.spawn-location.pitch", 0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    // VoidAuthWorld
    public boolean isVoidWorldEnabled() {
        return config.getBoolean("void-world.enabled", true);
    }

    public String getVoidWorldName() {
        return config.getString("void-world.world-name", "phoenixlogin_void");
    }

    public boolean isVoidWorldFallbackToSpawn() {
        return config.getBoolean("void-world.fallback-to-spawn", true);
    }

    // Effects
    public boolean isBossBarEnabled() {
        return config.getBoolean("effects.bossbar.enabled", true);
    }

    public String getBossBarColor() {
        return config.getString("effects.bossbar.color", "RED").toUpperCase();
    }

    public String getBossBarStyle() {
        return config.getString("effects.bossbar.style", "SOLID").toUpperCase();
    }

    public boolean isTitlesEnabled() {
        return config.getBoolean("effects.titles.enabled", true);
    }

    public int getTitleFadeIn() {
        return config.getInt("effects.titles.fade-in", 10);
    }

    public int getTitleStay() {
        return config.getInt("effects.titles.stay", 70);
    }

    public int getTitleFadeOut() {
        return config.getInt("effects.titles.fade-out", 20);
    }

    public String getSoundOnLogin() {
        return config.getString("effects.sounds.on-login", "ENTITY_PLAYER_LEVELUP");
    }

    public String getSoundOnRegister() {
        return config.getString("effects.sounds.on-register", "ENTITY_EXPERIENCE_ORB_PICKUP");
    }

    public String getSoundOnError() {
        return config.getString("effects.sounds.on-error", "ENTITY_VILLAGER_NO");
    }

    public String getParticleOnLogin() {
        return config.getString("effects.particles.on-login", "VILLAGER_HAPPY");
    }

    public String getParticleOnError() {
        return config.getString("effects.particles.on-error", "VILLAGER_ANGRY");
    }

    // Premium
    public boolean isPremiumEnabled() {
        return config.getBoolean("premium.enabled", false);
    }

    public boolean isPremiumAutoLogin() {
        return config.getBoolean("premium.auto-login", false);
    }

    // Discord
    public boolean isDiscordEnabled() {
        return config.getBoolean("discord.enabled", false);
    }

    public String getDiscordWebhookUrl() {
        return config.getString("discord.webhook-url", "");
    }

    public boolean isNotifyNewRegister() {
        return config.getBoolean("discord.notify-new-register", true);
    }

    public boolean isNotifySuspiciousLogin() {
        return config.getBoolean("discord.notify-suspicious-login", true);
    }

    // Advanced
    public boolean isTwoFactorEnabled() {
        return config.getBoolean("advanced.two-factor.enabled", false);
    }

    public boolean isIPFilteringEnabled() {
        return config.getBoolean("advanced.ip-filtering.enabled", false);
    }

    public boolean isLoggingEnabled() {
        return config.getBoolean("advanced.logging.enabled", true);
    }

    public String getLogFile() {
        return config.getString("advanced.logging.log-file", "phoenixlogin.log");
    }

    public String getLogLevel() {
        return config.getString("advanced.logging.log-level", "INFO").toUpperCase();
    }

    // Login Music
    public boolean isLoginMusicEnabled() {
        return config.getBoolean("login-music.enabled", true);
    }

    public String getLoginMusicSound() {
        return config.getString("login-music.sound", "MUSIC_DISC_13");
    }

    public float getLoginMusicVolume() {
        return (float) config.getDouble("login-music.volume", 0.3);
    }

    public float getLoginMusicPitch() {
        return (float) config.getDouble("login-music.pitch", 1.0);
    }

    public int getLoginMusicLoopInterval() {
        return config.getInt("login-music.loop-interval", 100);
    }

    // NBS Music (NoteBlockAPI)
    public boolean useNBSMusic() {
        return config.getBoolean("login-music.use-nbs", false);
    }

    public String getNBSFileName() {
        return config.getString("login-music.nbs-file", "login.nbs");
    }

    public float getNBSVolume() {
        return (float) config.getDouble("login-music.nbs-volume", 1.0);
    }
}
