package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

/**
 * Manages the void authentication world.
 */
public class WorldManager {

    private final PhoenixLogin plugin;
    private World voidWorld;
    private Location voidSpawnLocation;
    private static final String VOID_WORLD_NAME = "phoenixlogin_void";

    public WorldManager(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (!plugin.getConfigManager().isVoidWorldEnabled())
            return;

        try {
            voidWorld = Bukkit.getWorld(VOID_WORLD_NAME);

            if (voidWorld == null) {
                WorldCreator creator = new WorldCreator(VOID_WORLD_NAME);
                creator.environment(Environment.NORMAL);
                creator.type(WorldType.NORMAL);
                creator.generateStructures(false);
                creator.generator(new VoidWorldGenerator());

                voidWorld = creator.createWorld();

                if (voidWorld == null) {
                    plugin.getLogger().severe("Failed to create void auth world!");
                    return;
                }

                configureWorldRules(voidWorld);
            }

            setupVoidSpawn();

        } catch (Exception e) {
            plugin.getLogger().severe("Error creating void auth world: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configureWorldRules(World world) {
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(true);
        world.setSpawnFlags(false, false);
        world.setPVP(false);
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(Integer.MAX_VALUE);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
        world.setTime(6000);
        world.setFullTime(6000);
    }

    private void setupVoidSpawn() {
        if (voidWorld == null)
            return;
        voidSpawnLocation = new Location(voidWorld, 0.5, 100.0, 0.5, 0.0f, 0.0f);
        voidWorld.setSpawnLocation(voidSpawnLocation);
    }

    public void teleportToVoid(Player player) {
        if (voidWorld == null || voidSpawnLocation == null) {
            plugin.getLogger().warning("Void world unavailable for " + player.getName());
            return;
        }

        player.teleport(voidSpawnLocation);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    public boolean isVoidWorldActive() {
        return voidWorld != null && plugin.getConfigManager().isVoidWorldEnabled();
    }

    public World getVoidWorld() {
        return voidWorld;
    }

    public Location getVoidSpawnLocation() {
        return voidSpawnLocation;
    }

    public boolean isInVoidWorld(Player player) {
        return voidWorld != null && player.getWorld().equals(voidWorld);
    }

    public void shutdown() {
        if (voidWorld != null) {
            for (Player player : voidWorld.getPlayers()) {
                Location mainSpawn = plugin.getConfigManager().getSpawnLocation();
                if (mainSpawn != null) {
                    player.teleport(mainSpawn);
                } else {
                    player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                }
            }
        }
    }

    /**
     * Chunk generator that creates completely empty chunks (void).
     */
    public static class VoidWorldGenerator extends ChunkGenerator {

        @Override
        public void generateNoise(org.bukkit.generator.WorldInfo worldInfo, java.util.Random random, int chunkX,
                int chunkZ, ChunkData chunkData) {
            // Empty — generates a void world
        }

        @Override
        public boolean shouldGenerateNoise() {
            return false;
        }

        @Override
        public boolean shouldGenerateSurface() {
            return false;
        }

        @Override
        public boolean shouldGenerateCaves() {
            return false;
        }

        @Override
        public boolean shouldGenerateDecorations() {
            return false;
        }

        @Override
        public boolean shouldGenerateMobs() {
            return false;
        }

        @Override
        public boolean shouldGenerateStructures() {
            return false;
        }
    }
}
