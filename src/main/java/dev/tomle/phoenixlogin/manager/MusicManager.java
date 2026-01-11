package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona la música de fondo durante el proceso de autenticación
 * Soporta tanto sonidos vanilla de Minecraft como archivos .nbs (NoteBlockAPI)
 * Usa reflexión para cargar NoteBlockAPI dinámicamente si está disponible
 */
public class MusicManager {

    private final PhoenixLogin plugin;
    private final Map<UUID, BukkitTask> activeMusicTasks;
    private final Map<UUID, Object> activeSongPlayers; // Object para evitar imports directos
    private boolean noteBlockAPIAvailable = false;

    // Clases de NoteBlockAPI cargadas dinámicamente
    private Class<?> songClass;
    private Class<?> nbsDecoderClass;
    private Class<?> radioSongPlayerClass;
    private Class<?> repeatModeClass;

    public MusicManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        this.activeMusicTasks = new ConcurrentHashMap<>();
        this.activeSongPlayers = new ConcurrentHashMap<>();

        // Verificar si NoteBlockAPI está disponible
        checkNoteBlockAPI();
    }

    /**
     * Verifica si NoteBlockAPI está instalado y carga las clases necesarias
     */
    private void checkNoteBlockAPI() {
        try {
            // Intentar cargar las clases de NoteBlockAPI
            songClass = Class.forName("com.xxmicloxx.NoteBlockAPI.model.Song");
            nbsDecoderClass = Class.forName("com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder");
            radioSongPlayerClass = Class.forName("com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer");
            repeatModeClass = Class.forName("com.xxmicloxx.NoteBlockAPI.model.RepeatMode");

            noteBlockAPIAvailable = true;
            dev.tomle.phoenixlogin.util.ConsoleLogger.success("NoteBlockAPI detected - NBS music support enabled");
        } catch (ClassNotFoundException e) {
            noteBlockAPIAvailable = false;
        }
    }

    /**
     * Inicia la música de login para un jugador
     */
    public void startLoginMusic(Player player) {
        if (!plugin.getConfigManager().isLoginMusicEnabled()) {
            return;
        }

        // Detener cualquier música anterior
        stopMusic(player);

        // Verificar si debe usar NBS o vanilla
        if (plugin.getConfigManager().useNBSMusic() && noteBlockAPIAvailable) {
            startNBSMusic(player);
        } else if (noteBlockAPIAvailable && !plugin.getConfigManager().useNBSMusic()) {
            // NBS disponible pero no habilitado en config - usar vanilla
            startVanillaMusic(player);
        } else {
            startVanillaMusic(player);
        }
    }

    /**
     * Inicia música usando archivos .nbs (NoteBlockAPI) mediante reflexión
     */
    private void startNBSMusic(Player player) {
        String nbsFileName = plugin.getConfigManager().getNBSFileName();
        File nbsFile = new File(plugin.getDataFolder(), "music/" + nbsFileName);

        if (!nbsFile.exists()) {
            startVanillaMusic(player);
            return;
        }

        try {
            // NBSDecoder.parse(file)
            Method parseMethod = nbsDecoderClass.getMethod("parse", File.class);
            Object song = parseMethod.invoke(null, nbsFile);

            if (song == null) {
                startVanillaMusic(player);
                return;
            }

            // new RadioSongPlayer(song)
            Constructor<?> constructor = radioSongPlayerClass.getConstructor(songClass);
            Object songPlayer = constructor.newInstance(song);

            // Get RepeatMode.ALL enum value
            Method valuesMethod = repeatModeClass.getMethod("values");
            Object[] modes = (Object[]) valuesMethod.invoke(null);
            Object repeatAll = null;
            for (Object mode : modes) {
                if (mode.toString().equals("ALL")) {
                    repeatAll = mode;
                    break;
                }
            }

            // songPlayer.setRepeatMode(RepeatMode.ALL)
            Method setRepeatModeMethod = radioSongPlayerClass.getMethod("setRepeatMode", repeatModeClass);
            setRepeatModeMethod.invoke(songPlayer, repeatAll);

            // songPlayer.setVolume((byte) volume)
            Method setVolumeMethod = radioSongPlayerClass.getMethod("setVolume", byte.class);
            setVolumeMethod.invoke(songPlayer, (byte) (plugin.getConfigManager().getNBSVolume() * 100));

            // songPlayer.addPlayer(player)
            Method addPlayerMethod = radioSongPlayerClass.getMethod("addPlayer", Player.class);
            addPlayerMethod.invoke(songPlayer, player);

            // songPlayer.setPlaying(true)
            Method setPlayingMethod = radioSongPlayerClass.getMethod("setPlaying", boolean.class);
            setPlayingMethod.invoke(songPlayer, true);

            // Guardar referencia
            activeSongPlayers.put(player.getUniqueId(), songPlayer);

            // Get song title
            Method getTitleMethod = songClass.getMethod("getTitle");
            String title = (String) getTitleMethod.invoke(song);

        } catch (Exception e) {
            startVanillaMusic(player);
        }
    }

    /**
     * Inicia música usando sonidos vanilla de Minecraft
     */
    private void startVanillaMusic(Player player) {
        String soundName = plugin.getConfigManager().getLoginMusicSound();
        float volume = plugin.getConfigManager().getLoginMusicVolume();
        float pitch = plugin.getConfigManager().getLoginMusicPitch();
        int loopInterval = plugin.getConfigManager().getLoginMusicLoopInterval();

        Sound sound;
        try {
            sound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            return;
        }

        // Reproducir la música inmediatamente
        player.playSound(player.getLocation(), sound, volume, pitch);

        // Crear tarea para reproducir en loop
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopMusic(player);
                return;
            }

            // Si ya está autenticado, detener
            if (plugin.getSessionManager().isAuthenticated(player)) {
                stopMusic(player);
                return;
            }

            // Reproducir el sonido
            player.playSound(player.getLocation(), sound, volume, pitch);
        }, loopInterval, loopInterval);

        activeMusicTasks.put(player.getUniqueId(), task);
        // plugin.getLogger().info("Started vanilla music for " + player.getName() + ":
        // " + soundName);
    }

    /**
     * Detiene la música de un jugador
     */
    public void stopMusic(Player player) {
        UUID uuid = player.getUniqueId();

        // Detener música vanilla
        BukkitTask task = activeMusicTasks.remove(uuid);
        if (task != null) {
            task.cancel();
            player.stopAllSounds();
        }

        // Detener música NBS
        Object songPlayer = activeSongPlayers.remove(uuid);
        if (songPlayer != null) {
            try {
                // songPlayer.removePlayer(player)
                Method removePlayerMethod = radioSongPlayerClass.getMethod("removePlayer", Player.class);
                removePlayerMethod.invoke(songPlayer, player);

                // songPlayer.setPlaying(false)
                Method setPlayingMethod = radioSongPlayerClass.getMethod("setPlaying", boolean.class);
                setPlayingMethod.invoke(songPlayer, false);

                // songPlayer.destroy()
                Method destroyMethod = radioSongPlayerClass.getMethod("destroy");
                destroyMethod.invoke(songPlayer);
            } catch (Exception ignored) {
            }
        }

        // plugin.getLogger().info("Stopped music for " + player.getName());
    }

    /**
     * Limpieza al desactivar el plugin
     */
    public void shutdown() {
        // Detener todas las músicas vanilla
        for (BukkitTask task : activeMusicTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        activeMusicTasks.clear();

        // Detener todas las músicas NBS
        if (noteBlockAPIAvailable) {
            for (Object songPlayer : activeSongPlayers.values()) {
                if (songPlayer != null) {
                    try {
                        Method setPlayingMethod = radioSongPlayerClass.getMethod("setPlaying", boolean.class);
                        setPlayingMethod.invoke(songPlayer, false);

                        Method destroyMethod = radioSongPlayerClass.getMethod("destroy");
                        destroyMethod.invoke(songPlayer);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        activeSongPlayers.clear();
    }

    /**
     * Limpia la música de un jugador específico
     */
    public void cleanup(Player player) {
        stopMusic(player);
    }

    /**
     * Verifica si NoteBlockAPI está disponible
     */
    public boolean isNoteBlockAPIAvailable() {
        return noteBlockAPIAvailable;
    }
}
