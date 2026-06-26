package dev.tomle.phoenixlogin.listener;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.api.event.PlayerLoginEvent;
import dev.tomle.phoenixlogin.manager.MessageManager;
import dev.tomle.phoenixlogin.manager.PremiumManager;
import dev.tomle.phoenixlogin.model.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionListener implements Listener {

    private final PhoenixLogin plugin;
    private final Map<UUID, BukkitTask> kickTasks = new ConcurrentHashMap<>();

    // Players whose data is still loading (don't block them yet)
    private final java.util.Set<UUID> loadingPlayers = ConcurrentHashMap.newKeySet();

    // Cache player data loaded during AsyncPlayerPreLoginEvent to avoid double
    // query
    private final Map<String, PlayerData> preloadedData = new ConcurrentHashMap<>();

    // Cache Mojang profile lookups from async pre-login
    private final Map<String, PremiumManager.MojangProfile> preloadedProfiles = new ConcurrentHashMap<>();

    // Players waiting at the "Are you premium or cracked?" prompt
    private final java.util.Set<UUID> waitingForPremiumChoice = ConcurrentHashMap.newKeySet();

    public ConnectionListener(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();

        // === ANTI-BOT CHECK (happens before EVERYTHING — as user requested) ===
        String denyReason = plugin.getAntiBotManager().evaluateConnection(
                playerName, event.getAddress());

        if (denyReason != null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, denyReason);
            return;
        }

        // Pre-load player data and cache it for onPlayerJoin 
        try {
            PlayerData data = plugin.getDatabaseManager().loadPlayerDataAsync(playerName).get();
            if (data != null) {
                preloadedData.put(playerName.toLowerCase(), data);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to pre-load data for " + playerName);
            e.printStackTrace();
        }

        // Pre-load Mojang profile if premium system is enabled
        if (plugin.getPremiumManager() != null && plugin.getPremiumManager().isEnabled()) {
            try {
                PremiumManager.MojangProfile profile = plugin.getPremiumManager().lookupMojangProfile(playerName).get();
                if (profile != null) {
                    preloadedProfiles.put(playerName.toLowerCase(), profile);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Mojang lookup failed for " + playerName + " during pre-login.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getEffectsManager().hidePlayers(player);
        MessageManager msg = plugin.getMessageManager();

        event.setJoinMessage(null);

        // Cache and clear inventory to prevent visual glitch
        if (!player.hasPermission("phoenixlogin.bypass")) {
            plugin.getInventoryManager().cacheAndClearInventory(player);
        }

        // Mark as loading so ProtectionListener doesn't block before session setup
        loadingPlayers.add(player.getUniqueId());

        // Try to use pre-loaded data from AsyncPlayerPreLoginEvent 
        PlayerData cachedData = preloadedData.remove(player.getName().toLowerCase());

        if (cachedData != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                loadingPlayers.remove(player.getUniqueId());
                if (!player.isOnline())
                    return;
                processPlayerJoin(player, cachedData, msg);
            });
        } else {
            plugin.getDatabaseManager().loadPlayerDataAsync(player.getName())
                    .thenAccept(data -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            loadingPlayers.remove(player.getUniqueId());
                            if (!player.isOnline())
                                return;
                            processPlayerJoin(player, data, msg);
                        });
                    });
        }
    }

    private void processPlayerJoin(Player player, PlayerData data, MessageManager msg) {
        plugin.getSessionManager().createSession(player, data);
        clearPlayerChat(player);

        // === BYPASS CHECK ===
        if (player.hasPermission("phoenixlogin.bypass")) {
            plugin.getSessionManager().setAuthenticated(player, true);
            showJoinMessage(player);
            if (!plugin.getConfigManager().isCleanConsole()) {
                plugin.getLogger().info(player.getName() + " bypassed authentication.");
            }
            Bukkit.getPluginManager().callEvent(new PlayerLoginEvent(player, false));
            return;
        }

        // === EXISTING SESSION CHECK (async) ===
        if (data.isRegistered()) {
            plugin.getSessionManager().checkExistingSessionAsync(player, data)
                    .thenAccept(hasSession -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (!player.isOnline())
                                return;

                            if (hasSession) {
                                completeSessionRestore(player, msg);
                            } else {
                                // No valid session — check premium before showing login
                                checkPremiumBeforeAuth(player, data);
                            }
                        });
                    });
        } else {
            // Not registered — check premium before showing register
            checkPremiumBeforeAuth(player, data);
        }
    }

    /**
     * Restores a player from an existing session.
     */
    private void completeSessionRestore(Player player, MessageManager msg) {
        plugin.getSessionManager().setAuthenticated(player, true);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);

        // Restore location AND gamemode 
        plugin.getLocationManager().restoreLocation(player);
        plugin.getInventoryManager().restoreInventory(player);

        msg.sendMessage(player, "auth.session-restored");
        plugin.getEffectsManager().playLoginSound(player);
        showJoinMessage(player);
        if (!plugin.getConfigManager().isCleanConsole()) {
            plugin.getLogger().info(player.getName() + " session restored.");
        }
        Bukkit.getPluginManager().callEvent(new PlayerLoginEvent(player, true));
    }

    /**
     * Checks premium status BEFORE showing the authentication flow.
     *
     * Flow:
     * 1. Is premium system enabled?
     * 2. Is this username a premium Mojang account?
     * 3. Is the player marked as premium in DB? (returning premium player)
     * → If yes + UUID matches → auto-login!
     * → If yes + UUID doesn't match → someone stole the name → require password
     * 4. Name IS premium but not marked in DB?
     * → Show "Are you premium or cracked?" prompt
     * 5. Name is NOT premium?
     * → Normal login/register flow
     */
    private void checkPremiumBeforeAuth(Player player, PlayerData data) {
        PremiumManager premiumManager = plugin.getPremiumManager();

        // If premium system is disabled → go straight to normal auth
        if (premiumManager == null || !premiumManager.isEnabled()) {
            handleUnauthenticatedPlayer(player, data);
            return;
        }

        // Get cached Mojang profile from pre-login
        PremiumManager.MojangProfile cachedProfile = preloadedProfiles.remove(player.getName().toLowerCase());

        if (cachedProfile != null) {
            processPremiumCheck(player, data, cachedProfile);
        } else {
            // Fallback — lookup now (shouldn't normally happen)
            premiumManager.lookupMojangProfile(player.getName()).thenAccept(profile -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline())
                        return;
                    processPremiumCheck(player, data, profile);
                });
            });
        }
    }

    private void processPremiumCheck(Player player, PlayerData data, PremiumManager.MojangProfile profile) {
        PremiumManager premiumManager = plugin.getPremiumManager();
        MessageManager msg = plugin.getMessageManager();

        // API error or couldn't resolve — fall through to normal auth
        if (profile == null) {
            plugin.getLogger()
                    .warning("Mojang API unavailable for " + player.getName() + ", falling back to password.");
            handleUnauthenticatedPlayer(player, data);
            return;
        }

        // === NOT A PREMIUM NAME → normal auth ===
        if (!profile.isPremium()) {
            handleUnauthenticatedPlayer(player, data);
            return;
        }

        // === NAME IS PREMIUM → check if player is already marked as premium in DB ===
        if (data.isRegistered()) {
            premiumManager.getPremiumStatusAsync(player.getName()).thenAccept(status -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline())
                        return;

                    if (status.isPremium() && premiumManager.isAutoLoginEnabled()) {
                        // Player was previously marked as premium — try auto-login
                        if (premiumManager.isVerifiedPremium(player, profile)) {
                            // UUID matches → auto-login!
                            performPremiumAutoLogin(player, profile);
                        } else {
                            // UUID doesn't match → name theft attempt or offline-mode
                            // Require password
                            msg.sendMessage(player, "premium.session-unverified");
                            handleUnauthenticatedPlayer(player, data);
                        }
                    } else {
                        // Registered but not marked as premium → show premium prompt
                        showPremiumPrompt(player, data);
                    }
                });
            });
        } else {
            // Not registered yet but name is premium → show premium prompt
            showPremiumPrompt(player, data);
        }
    }

    /**
     * Shows the "Are you premium or cracked?" prompt.
     */
    private void showPremiumPrompt(Player player, PlayerData data) {
        MessageManager msg = plugin.getMessageManager();

        // Teleport to void world / freeze like normal
        prepareUnauthenticatedPlayer(player);

        // Mark as waiting for premium choice
        waitingForPremiumChoice.add(player.getUniqueId());

        // Show the prompt
        msg.sendMessage(player, "premium.prompt.header");
        msg.sendMessage(player, "premium.prompt.question");
        msg.sendMessage(player, "premium.prompt.option-premium");
        msg.sendMessage(player, "premium.prompt.option-cracked");
        msg.sendMessage(player, "premium.prompt.footer");

        // Start auto-kick timer (they still need to act)
        if (plugin.getConfigManager().isAutoKickEnabled()) {
            int delay = plugin.getConfigManager().getAutoKickDelay();
            plugin.getEffectsManager().showLoginBossBar(player, delay);
            startAutoKickTimer(player);
        }

        // Show welcome title
        plugin.getEffectsManager().showWelcomeTitle(player);
    }

    /**
     * Performs premium auto-login — skips password entirely.
     */
    private void performPremiumAutoLogin(Player player, PremiumManager.MojangProfile profile) {
        MessageManager msg = plugin.getMessageManager();

        // Update premium status
        plugin.getPremiumManager().setPremiumStatusAsync(player.getName(), true, profile.getUuid());

        // Authenticate
        plugin.getSessionManager().setAuthenticated(player, true);

        String ip = player.getAddress().getAddress().getHostAddress();
        plugin.getDatabaseManager().updateLoginAsync(player.getName(), ip);
        plugin.getLoginHistoryManager().logLoginAttempt(player.getName(), ip, true, "premium-auto");

        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);

        // Restore location AND gamemode (FIX #1/#2)
        plugin.getLocationManager().restoreLocation(player);
        plugin.getInventoryManager().restoreInventory(player);

        // Broadcast join
        showJoinMessage(player);

        // Effects
        plugin.getMusicManager().stopMusic(player);
        plugin.getEffectsManager().showLoginSuccessTitle(player);
        plugin.getEffectsManager().playLoginSound(player);
        plugin.getEffectsManager().playLoginParticles(player);
        plugin.getEffectsManager().removeBossBar(player);

        msg.sendMessage(player, "premium.auto-login-success");

        if (!plugin.getConfigManager().isCleanConsole()) {
            plugin.getLogger().info(player.getName() + " auto-logged in (premium verified).");
        }

        Bukkit.getPluginManager().callEvent(new PlayerLoginEvent(player, false));
    }

    /**
     * Returns true if the player is waiting at the premium/cracked prompt.
     */
    public boolean isWaitingForPremiumChoice(Player player) {
        return waitingForPremiumChoice.contains(player.getUniqueId());
    }

    /**
     * Removes the player from the premium choice waiting state.
     */
    public void resolvePremiumChoice(Player player) {
        waitingForPremiumChoice.remove(player.getUniqueId());
    }

    // =====================================================================
    // ORIGINAL METHODS (cleaned up)
    // =====================================================================

    /**
     * Prepares an unauthenticated player — teleport, freeze, music.
     * Shared between normal flow and premium prompt.
     */
    private void prepareUnauthenticatedPlayer(Player player) {
        String currentWorld = player.getWorld().getName();
        String voidWorldName = plugin.getConfigManager().getVoidWorldName();

        if (!currentWorld.equals(voidWorldName)) {
            plugin.getLocationManager().saveLocation(player);
        }

        if (plugin.getWorldManager().isVoidWorldActive()) {
            if (!currentWorld.equals(voidWorldName)) {
                plugin.getWorldManager().teleportToVoid(player);
            }
        } else {
            if (plugin.getConfigManager().isTeleportToSpawn()) {
                Location spawnLoc = plugin.getConfigManager().getSpawnLocation();
                if (spawnLoc != null) {
                    player.teleport(spawnLoc);
                }
            }
        }

        if (plugin.getConfigManager().isFreezePlayer()) {
            player.setWalkSpeed(0);
            player.setFlySpeed(0);
        }

        plugin.getMusicManager().startLoginMusic(player);
    }

    private void handleUnauthenticatedPlayer(Player player, PlayerData data) {
        prepareUnauthenticatedPlayer(player);
        initializeAuthentication(player, data);
    }

    private void initializeAuthentication(Player player, PlayerData data) {
        MessageManager msg = plugin.getMessageManager();

        plugin.getEffectsManager().showWelcomeTitle(player);

        // Captcha required for all players
        if (plugin.getCaptchaManager().isCaptchaRequired()) {
            plugin.getCaptchaManager().generateCaptcha(player);
            if (plugin.getConfigManager().isAutoKickEnabled()) {
                startAutoKickTimer(player);
            }
            return;
        }

        // No captcha — show relevant message
        if (data.isRegistered()) {
            msg.sendMessage(player, "auth.please-login");
            if (plugin.getConfigManager().isAutoKickEnabled()) {
                int delay = plugin.getConfigManager().getAutoKickDelay();
                plugin.getEffectsManager().showLoginBossBar(player, delay);
                startAutoKickTimer(player);
            }
        } else {
            msg.sendMessage(player, "auth.please-register");
            if (plugin.getConfigManager().isAutoKickEnabled()) {
                int delay = plugin.getConfigManager().getAutoKickDelay();
                plugin.getEffectsManager().showRegisterBossBar(player, delay);
                startAutoKickTimer(player);
            }
        }
    }

    private void clearPlayerChat(Player player) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("\n");
        }
        player.sendMessage(sb.toString());
    }

    private void showJoinMessage(Player player) {
        if (!plugin.getConfigManager().isJoinMessageEnabled()) return;
        MessageManager msg = plugin.getMessageManager();
        String joinMsg = msg.getMessage("join.message",
                MessageManager.createPlaceholders("player", player.getName()));
        plugin.getServer().broadcastMessage(joinMsg);
    }

    private void startAutoKickTimer(Player player) {
        int seconds = plugin.getConfigManager().getAutoKickDelay();
        final int[] remaining = { seconds };
        MessageManager msg = plugin.getMessageManager();

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancelKickTask(player.getUniqueId());
                return;
            }

            if (plugin.getSessionManager().isAuthenticated(player)) {
                cancelKickTask(player.getUniqueId());
                return;
            }

            remaining[0]--;

            float progress = (float) remaining[0] / seconds;
            int timeLeft = remaining[0];

            plugin.getEffectsManager().updateBossBarProgress(player, progress);
            showActionBar(player, timeLeft, progress);

            if (timeLeft <= 10 && timeLeft > 0) {
                showCriticalWarning(player, timeLeft);
            } else if (timeLeft == 30 || timeLeft == 20) {
                showWarning(player, timeLeft);
            }

            if (timeLeft <= 0) {
                String kickMessage = msg.getMessage("kick.timeout");
                player.kickPlayer(kickMessage);
                cancelKickTask(player.getUniqueId());
            }
        }, 0L, 20L);

        kickTasks.put(player.getUniqueId(), task);
    }

    private void showActionBar(Player player, int timeLeft, float progress) {
        int totalBars = 20;
        int filledBars = (int) (progress * totalBars);

        StringBuilder bar = new StringBuilder();
        bar.append("§8[");

        String barColor;
        if (progress > 0.6f)
            barColor = "§a";
        else if (progress > 0.3f)
            barColor = "§e";
        else
            barColor = "§c";

        for (int i = 0; i < filledBars; i++)
            bar.append(barColor).append("█");
        for (int i = filledBars; i < totalBars; i++)
            bar.append("§7▒");

        bar.append("§8] ");

        String timeColor = progress > 0.3f ? "§f" : "§c§l";
        bar.append(timeColor).append(timeLeft).append("s");

        PlayerData data = plugin.getSessionManager().getPlayerData(player);
        String action;
        if (waitingForPremiumChoice.contains(player.getUniqueId())) {
            action = "§6PREMIUM?";
        } else if (data != null && data.isRegistered()) {
            action = "LOGIN";
        } else {
            action = "REGISTER";
        }
        bar.append(" §8| §7").append(action);

        plugin.adventure().player(player).sendActionBar(Component.text(bar.toString()));
    }

    private void showCriticalWarning(Player player, int timeLeft) {
        MessageManager msg = plugin.getMessageManager();

        String titleColor = timeLeft % 2 == 0 ? "§c§l" : "§4§l";
        player.sendTitle(titleColor + "!! " + timeLeft + " !!", "§e§lAUTHENTICATE NOW", 0, 15, 5);

        player.playSound(player.getLocation(),
                org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 2.0f - (timeLeft / 10.0f));

        if (timeLeft % 2 == 0) {
            try {
                player.getWorld().spawnParticle(
                        org.bukkit.Particle.REDSTONE,
                        player.getLocation().add(0, 1, 0),
                        10, 0.5, 0.5, 0.5,
                        new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
            } catch (Exception e) {
                try {
                    org.bukkit.Particle modernParticle = org.bukkit.Particle.valueOf("DUST");
                    player.getWorld().spawnParticle(
                            modernParticle,
                            player.getLocation().add(0, 1, 0),
                            10, 0.5, 0.5, 0.5,
                            new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
                } catch (Exception ignored) {
                }
            }
        }

        if (timeLeft == 10 || timeLeft == 5) {
            Map<String, String> placeholders = MessageManager.createPlaceholders("time", String.valueOf(timeLeft));
            player.sendMessage("");
            player.sendMessage(msg.getMessage("timer.critical-warning.title"));
            player.sendMessage(msg.getMessage("timer.critical-warning.message", placeholders));

            PlayerData data = plugin.getSessionManager().getPlayerData(player);
            String cmdKey = (data != null && data.isRegistered())
                    ? "timer.critical-warning.command-login"
                    : "timer.critical-warning.command-register";
            player.sendMessage(msg.getMessage(cmdKey));
            player.sendMessage("");
        }
    }

    private void showWarning(Player player, int timeLeft) {
        MessageManager msg = plugin.getMessageManager();
        Map<String, String> placeholders = MessageManager.createPlaceholders("time", String.valueOf(timeLeft));

        String title = msg.getMessage("titles.warning-medium.title", placeholders);
        String subtitle = msg.getMessage("titles.warning-medium.subtitle");
        player.sendTitle(title, subtitle, 5, 30, 10);

        player.playSound(player.getLocation(),
                org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);

        try {
            player.getWorld().spawnParticle(
                    org.bukkit.Particle.FLAME,
                    player.getLocation().add(0, 2, 0),
                    3, 0.3, 0.3, 0.3, 0.01);
        } catch (Exception ignored) {
        }

        player.sendMessage("");
        player.sendMessage(msg.getMessage("timer.medium-warning.message", placeholders));
        player.sendMessage("");
    }

    private void cancelKickTask(UUID uuid) {
        BukkitTask task = kickTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        cancelKickTask(uuid);
        loadingPlayers.remove(uuid);
        preloadedData.remove(player.getName().toLowerCase());
        preloadedProfiles.remove(player.getName().toLowerCase());
        waitingForPremiumChoice.remove(uuid);

        if (plugin.getSessionManager().isAuthenticated(player)) {
            // Already authenticated
        } else {
            plugin.getInventoryManager().restoreInventory(player);
        }

        plugin.getSessionManager().removeSession(player);
        plugin.getAuthSecurityManager().cleanup(uuid);
        plugin.getEffectsManager().cleanup(player);
        plugin.getCaptchaManager().removeCaptcha(player);
        plugin.getLocationManager().clearLocation(uuid);
        plugin.getMusicManager().cleanup(player);

        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
    }

    public boolean isLoading(Player player) {
        return loadingPlayers.contains(player.getUniqueId());
    }
}
