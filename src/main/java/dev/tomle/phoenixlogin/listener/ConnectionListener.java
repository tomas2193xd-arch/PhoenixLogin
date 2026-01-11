package dev.tomle.phoenixlogin.listener;

import dev.tomle.phoenixlogin.PhoenixLogin;
import dev.tomle.phoenixlogin.manager.MessageManager;
import dev.tomle.phoenixlogin.model.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConnectionListener implements Listener {

    private final PhoenixLogin plugin;
    private final Map<UUID, BukkitTask> kickTasks = new HashMap<>();

    public ConnectionListener(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();

        try {
            plugin.getDatabaseManager().loadPlayerDataAsync(playerName).get();
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading data for " + playerName);
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MessageManager msg = plugin.getMessageManager();

        event.setJoinMessage(null);

        // 🛡 CRÍTICO: Ocultar inventario INMEDIATAMENTE para evitar glitch visual
        // Guardamos copia de seguridad si no existe una ya (previniendo sobreescritura
        // de inventario vacío)
        if (!player.hasPermission("phoenixlogin.bypass")) {
            plugin.getInventoryManager().cacheAndClearInventory(player);
        }

        plugin.getDatabaseManager().loadPlayerDataAsync(player.getName())
                .thenAccept(data -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getSessionManager().createSession(player, data);

                        clearPlayerChat(player);

                        // Verificar bypass ANTES de cualquier teleport
                        if (player.hasPermission("phoenixlogin.bypass")) {
                            plugin.getSessionManager().setAuthenticated(player, true);
                            // Restaurar inventario si fue ocultado (por si acaso la perm no estaba lista
                            // antes, aunque lo chequeamos arriba)
                            // Nota: Si arriba no entró al if, aquí no hay nada que restaurar porque no se
                            // borró.
                            // Pero si la perm cambió o algo raro:
                            // plugin.getInventoryManager().restoreInventory(player);
                            // Lo dejaremos asimétrico: Si tiene bypass, arriba NO limpiamos.

                            showJoinMessage(player);
                            plugin.getLogger()
                                    .info(player.getName() + " bypassed authentication (has bypass permission).");
                            return;
                        }

                        // Verificar sesión existente
                        if (data.isRegistered() && plugin.getSessionManager().checkExistingSession(player, data)) {
                            plugin.getSessionManager().setAuthenticated(player, true);

                            player.setWalkSpeed(0.2f);
                            player.setFlySpeed(0.1f);

                            // ✅ Restaurar inventario al restaurar sesión
                            plugin.getInventoryManager().restoreInventory(player);

                            msg.sendMessage(player, "auth.session-restored");
                            plugin.getEffectsManager().playLoginSound(player);
                            showJoinMessage(player);
                            plugin.getLogger().info(player.getName() + " logged in automatically (valid session).");
                            return;
                        }

                        // Jugador NO autenticado - manejar VoidAuthWorld
                        handleUnauthenticatedPlayer(player, data);
                    });
                });
    }

    private void clearPlayerChat(Player player) {
        for (int i = 0; i < 100; i++) {
            player.sendMessage("");
        }
    }

    private void showJoinMessage(Player player) {
        String joinMsg = "§e" + player.getName() + " joined the game";
        plugin.getServer().broadcastMessage(joinMsg);
    }

    /**
     * Maneja jugadores no autenticados - teletransporte a VoidAuthWorld
     */
    private void handleUnauthenticatedPlayer(Player player, PlayerData data) {
        MessageManager msg = plugin.getMessageManager();

        // Obtener mundo actual del jugador
        String currentWorld = player.getWorld().getName();
        String voidWorldName = plugin.getConfigManager().getVoidWorldName();

        // SOLO guardar ubicación si NO está en el VoidWorld
        // (Jugadores nuevos spawnean en void, no queremos guardar eso)
        if (!currentWorld.equals(voidWorldName)) {
            plugin.getLocationManager().saveLocation(player);
            // plugin.getLogger().info("Saved location for " + player.getName() + " (not in
            // void): " + currentWorld);
        } else {
            // plugin.getLogger().info("Skipped saving location for " + player.getName() + "
            // (already in void)");
        }

        // Verificar si VoidAuthWorld está activado
        if (plugin.getWorldManager().isVoidWorldActive()) {
            // Solo teletransportar si NO está ya en el void
            if (!currentWorld.equals(voidWorldName)) {
                plugin.getWorldManager().teleportToVoid(player);
                // plugin.getLogger().info(player.getName() + " teleported to VoidAuthWorld for
                // authentication.");
            } else {
                // plugin.getLogger().info(player.getName() + " already in VoidAuthWorld.");
            }
        } else {
            // Fallback: usar el sistema de spawn tradicional
            if (plugin.getConfigManager().isTeleportToSpawn()) {
                Location spawnLoc = plugin.getConfigManager().getSpawnLocation();
                if (spawnLoc != null) {
                    player.teleport(spawnLoc);
                    // plugin.getLogger().info(player.getName() + " teleported to spawn (VoidWorld
                    // disabled).");
                }
            }
        }

        // 🎵 INICIAR MÚSICA DE LOGIN
        plugin.getMusicManager().startLoginMusic(player);

        // Continuar con la autenticación normal
        initializeAuthentication(player, data);
    }

    private void initializeAuthentication(Player player, PlayerData data) {
        MessageManager msg = plugin.getMessageManager();

        if (plugin.getConfigManager().isFreezePlayer()) {
            player.setWalkSpeed(0);
            player.setFlySpeed(0);
        }

        plugin.getEffectsManager().showWelcomeTitle(player);

        // 🛡 CAPTCHA OBLIGATORIO PARA TODOS (registrados y nuevos)
        if (plugin.getCaptchaManager().isCaptchaRequired()) {
            plugin.getCaptchaManager().generateCaptcha(player);

            if (plugin.getConfigManager().isAutoKickEnabled()) {
                startAutoKickTimer(player);
            }
            return;
        }

        // Si captcha está desactivado, mostrar mensaje apropiado
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

    private void startAutoKickTimer(Player player) {
        int seconds = plugin.getConfigManager().getAutoKickDelay();
        final int[] remaining = { seconds };

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

            // ====== 🎨 SISTEMA VISUAL ÉPICO ======
            float progress = (float) remaining[0] / seconds;
            int timeLeft = remaining[0];

            // 1. ACTUALIZAR BOSSBAR
            plugin.getEffectsManager().updateBossBarProgress(player, progress);

            // 2. ACTIONBAR animada con barra ASCII
            showEpicActionBar(player, timeLeft, progress);

            // 3. EFECTOS VISUALES según tiempo restante
            if (timeLeft <= 10 && timeLeft > 0) {
                // CRÍTICO - Títulos pulsantes + sonido + partículas
                showCriticalWarning(player, timeLeft);
            } else if (timeLeft == 30 || timeLeft == 20) {
                // ADVERTENCIA MEDIA
                showWarning(player, timeLeft);
            }

            // 4. KICK si se acabó el tiempo
            if (timeLeft <= 0) {
                String kickMessage = plugin.getMessageManager().getMessage("kick.timeout");
                player.kickPlayer(kickMessage);
                cancelKickTask(player.getUniqueId());
            }
        }, 0L, 20L);

        kickTasks.put(player.getUniqueId(), task);
    }

    /**
     * Muestra ActionBar épica con barra de progreso ASCII
     */
    private void showEpicActionBar(Player player, int timeLeft, float progress) {
        // Crear barra ASCII moderna
        int totalBars = 20;
        int filledBars = (int) (progress * totalBars);

        StringBuilder bar = new StringBuilder();
        bar.append("§8[");

        // Color según tiempo
        String barColor;
        if (progress > 0.6f) {
            barColor = "§a"; // Verde
        } else if (progress > 0.3f) {
            barColor = "§e"; // Amarillo
        } else {
            barColor = "§c"; // Rojo
        }

        // Barras llenas
        for (int i = 0; i < filledBars; i++) {
            bar.append(barColor).append("█");
        }

        // Barras vacías
        for (int i = filledBars; i < totalBars; i++) {
            bar.append("§7▒");
        }

        bar.append("§8] ");

        // Tiempo con color dinámico
        String timeColor = progress > 0.3f ? "§f" : "§c§l";
        bar.append(timeColor).append(timeLeft).append("s");

        // Mensaje según contexto
        PlayerData data = plugin.getSessionManager().getPlayerData(player);
        String action = (data != null && data.isRegistered()) ? "LOGIN" : "REGISTER";
        bar.append(" §8| §7").append(action);

        // Enviar ActionBar usando Adventure API
        plugin.adventure().player(player).sendActionBar(Component.text(bar.toString()));
    }

    /**
     * Advertencia CRÍTICA - Últimos 10 segundos
     */
    private void showCriticalWarning(Player player, int timeLeft) {
        MessageManager msg = plugin.getMessageManager();

        // Título pulsante
        String titleColor = timeLeft % 2 == 0 ? "§c§l" : "§4§l";
        player.sendTitle(
                titleColor + "⚠ " + timeLeft + " ⚠",
                "§e§lAUTENTÍCATE AHORA",
                0, 15, 5);

        // Sonido tick (más rápido cuanto menos tiempo quede)
        player.playSound(player.getLocation(),
                org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT,
                1.0f,
                2.0f - (timeLeft / 10.0f));

        // Partículas rojas alrededor
        if (timeLeft % 2 == 0) {
            player.getWorld().spawnParticle(
                    org.bukkit.Particle.REDSTONE,
                    player.getLocation().add(0, 1, 0),
                    10,
                    0.5, 0.5, 0.5,
                    new org.bukkit.Particle.DustOptions(
                            org.bukkit.Color.RED, 1.5f));
        }

        // Mensaje de chat cada 5 segundos
        if (timeLeft == 10 || timeLeft == 5) {
            player.sendMessage("");
            player.sendMessage(msg.colorize("§c§l⚠ ¡ADVERTENCIA! ⚠"));
            player.sendMessage(msg.colorize("§e¡Solo quedan §c" + timeLeft + " segundos §epara autenticarte!"));

            PlayerData data = plugin.getSessionManager().getPlayerData(player);
            String command = (data != null && data.isRegistered())
                    ? "§f/login <contraseña>"
                    : "§f/register <contraseña> <confirmar>";
            player.sendMessage(msg.colorize("§7Usa: " + command));
            player.sendMessage("");
        }
    }

    /**
     * Advertencia MEDIA - 30s y 20s
     */
    private void showWarning(Player player, int timeLeft) {
        MessageManager msg = plugin.getMessageManager();

        // Título naranja
        player.sendTitle(
                "§6⏱ " + timeLeft + " segundos",
                "§e¡Autentícate pronto!",
                5, 30, 10);

        // Sonido suave
        player.playSound(player.getLocation(),
                org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,
                0.7f, 1.5f);

        // Partículas naranjas
        player.getWorld().spawnParticle(
                org.bukkit.Particle.FLAME,
                player.getLocation().add(0, 2, 0),
                3,
                0.3, 0.3, 0.3,
                0.01);

        // Mensaje en chat
        player.sendMessage("");
        player.sendMessage(msg.colorize("§e⏰ Quedan §6" + timeLeft + " segundos §epara autenticarte."));
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

        plugin.getSessionManager().removeSession(player);
        plugin.getAuthSecurityManager().cleanup(uuid);
        plugin.getEffectsManager().cleanup(player);
        plugin.getCaptchaManager().removeCaptcha(player);
        plugin.getLocationManager().clearLocation(uuid);
        plugin.getMusicManager().cleanup(player); // 🎵 Detener música

        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
    }
}
