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
 * Manages background music during authentication.
 * Supports vanilla Minecraft sounds and .nbs files (NoteBlockAPI).
 * NoteBlockAPI is loaded dynamically via reflection if available.
 */
public class MusicManager {

    private final PhoenixLogin plugin;
    private final Map<UUID, BukkitTask> activeMusicTasks;
    private final Map<UUID, Object> activeSongPlayers;
    private boolean noteBlockAPIAvailable = false;

    private Class<?> songClass;
    private Class<?> nbsDecoderClass;
    private Class<?> radioSongPlayerClass;
    private Class<?> repeatModeClass;

    public MusicManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        this.activeMusicTasks = new ConcurrentHashMap<>();
        this.activeSongPlayers = new ConcurrentHashMap<>();
        checkNoteBlockAPI();
    }

    private void checkNoteBlockAPI() {
        try {
            songClass = Class.forName("com.xxmicloxx.NoteBlockAPI.model.Song");
            nbsDecoderClass = Class.forName("com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder");
            radioSongPlayerClass = Class.forName("com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer");
            repeatModeClass = Class.forName("com.xxmicloxx.NoteBlockAPI.model.RepeatMode");

            noteBlockAPIAvailable = true;
            dev.tomle.phoenixlogin.util.ConsoleLogger.success("NoteBlockAPI detected — NBS music enabled");
        } catch (ClassNotFoundException e) {
            noteBlockAPIAvailable = false;
        }
    }

    public void startLoginMusic(Player player) {
        if (!plugin.getConfigManager().isLoginMusicEnabled())
            return;

        stopMusic(player);

        if (plugin.getConfigManager().useNBSMusic() && noteBlockAPIAvailable) {
            startNBSMusic(player);
        } else {
            startVanillaMusic(player);
        }
    }

    private void startNBSMusic(Player player) {
        String nbsFileName = plugin.getConfigManager().getNBSFileName();
        File nbsFile = new File(plugin.getDataFolder(), "music/" + nbsFileName);

        if (!nbsFile.exists()) {
            startVanillaMusic(player);
            return;
        }

        try {
            Method parseMethod = nbsDecoderClass.getMethod("parse", File.class);
            Object song = parseMethod.invoke(null, nbsFile);

            if (song == null) {
                startVanillaMusic(player);
                return;
            }

            Constructor<?> constructor = radioSongPlayerClass.getConstructor(songClass);
            Object songPlayer = constructor.newInstance(song);

            Method valuesMethod = repeatModeClass.getMethod("values");
            Object[] modes = (Object[]) valuesMethod.invoke(null);
            Object repeatAll = null;
            for (Object mode : modes) {
                if (mode.toString().equals("ALL")) {
                    repeatAll = mode;
                    break;
                }
            }

            Method setRepeatModeMethod = radioSongPlayerClass.getMethod("setRepeatMode", repeatModeClass);
            setRepeatModeMethod.invoke(songPlayer, repeatAll);

            Method setVolumeMethod = radioSongPlayerClass.getMethod("setVolume", byte.class);
            setVolumeMethod.invoke(songPlayer, (byte) (plugin.getConfigManager().getNBSVolume() * 100));

            Method addPlayerMethod = radioSongPlayerClass.getMethod("addPlayer", Player.class);
            addPlayerMethod.invoke(songPlayer, player);

            Method setPlayingMethod = radioSongPlayerClass.getMethod("setPlaying", boolean.class);
            setPlayingMethod.invoke(songPlayer, true);

            activeSongPlayers.put(player.getUniqueId(), songPlayer);

        } catch (Exception e) {
            startVanillaMusic(player);
        }
    }

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

        player.playSound(player.getLocation(), sound, volume, pitch);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopMusic(player);
                return;
            }
            if (plugin.getSessionManager().isAuthenticated(player)) {
                stopMusic(player);
                return;
            }
            player.playSound(player.getLocation(), sound, volume, pitch);
        }, loopInterval, loopInterval);

        activeMusicTasks.put(player.getUniqueId(), task);
    }

    public void stopMusic(Player player) {
        UUID uuid = player.getUniqueId();

        BukkitTask task = activeMusicTasks.remove(uuid);
        if (task != null) {
            task.cancel();
            player.stopAllSounds();
        }

        Object songPlayer = activeSongPlayers.remove(uuid);
        if (songPlayer != null) {
            try {
                Method removePlayerMethod = radioSongPlayerClass.getMethod("removePlayer", Player.class);
                removePlayerMethod.invoke(songPlayer, player);

                Method setPlayingMethod = radioSongPlayerClass.getMethod("setPlaying", boolean.class);
                setPlayingMethod.invoke(songPlayer, false);

                Method destroyMethod = radioSongPlayerClass.getMethod("destroy");
                destroyMethod.invoke(songPlayer);
            } catch (Exception ignored) {
            }
        }
    }

    public void shutdown() {
        for (BukkitTask task : activeMusicTasks.values()) {
            if (task != null)
                task.cancel();
        }
        activeMusicTasks.clear();

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

    public void cleanup(Player player) {
        stopMusic(player);
    }

    public boolean isNoteBlockAPIAvailable() {
        return noteBlockAPIAvailable;
    }
}
