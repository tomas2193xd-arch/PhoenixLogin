package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

/**
 * Gestiona el mundo void para autenticación
 */
public class WorldManager {

    private final PhoenixLogin plugin;
    private World voidWorld;
    private Location voidSpawnLocation;
    private static final String VOID_WORLD_NAME = "phoenixlogin_void";

    public WorldManager(PhoenixLogin plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicializa el mundo void de autenticación
     */
    public void initialize() {
        if (!plugin.getConfigManager().isVoidWorldEnabled()) {
            plugin.getLogger().info("VoidAuthWorld disabled in config.");
            return;
        }

        plugin.getLogger().info("Creating/loading VoidAuthWorld...");

        try {
            // Verificar si el mundo ya existe
            voidWorld = Bukkit.getWorld(VOID_WORLD_NAME);

            if (voidWorld == null) {
                // Crear el mundo void
                WorldCreator creator = new WorldCreator(VOID_WORLD_NAME);
                creator.environment(Environment.NORMAL);
                creator.type(WorldType.FLAT);
                creator.generateStructures(false);
                creator.generator(new VoidWorldGenerator()); // Generador custom para mundo completamente vacío

                voidWorld = creator.createWorld();

                if (voidWorld == null) {
                    plugin.getLogger().severe("Failed to create VoidAuthWorld!");
                    return;
                }

                // Configurar reglas del mundo
                configureWorldRules(voidWorld);
            }

            // Configurar la ubicación de spawn en el void
            setupVoidSpawn();

            plugin.getLogger().info("VoidAuthWorld loaded successfully: " + voidWorld.getName());

        } catch (Exception e) {
            plugin.getLogger().severe("Error creating VoidAuthWorld: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Configura las reglas del mundo void
     */
    private void configureWorldRules(World world) {
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(true);
        world.setSpawnFlags(false, false); // No mobs
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

        world.setTime(6000); // Mediodía
        world.setFullTime(6000);
    }

    /**
     * Configura la ubicación de spawn en el void
     */
    private void setupVoidSpawn() {
        if (voidWorld == null)
            return;

        // Spawn en Y=100 para que el jugador esté flotando en el void
        double x = 0.5;
        double y = 100.0;
        double z = 0.5;
        float yaw = 0.0f;
        float pitch = 0.0f;

        voidSpawnLocation = new Location(voidWorld, x, y, z, yaw, pitch);
        voidWorld.setSpawnLocation(voidSpawnLocation);

        plugin.getLogger().info("Void spawn set at: " + x + ", " + y + ", " + z);
    }

    /**
     * Teletransporta un jugador al mundo void
     */
    public void teleportToVoid(Player player) {
        if (voidWorld == null || voidSpawnLocation == null) {
            plugin.getLogger().warning("VoidWorld not available! Cannot teleport " + player.getName());
            return;
        }

        // El LocationManager guardará la ubicación previa
        player.teleport(voidSpawnLocation);

        // Efectos visuales
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);

        plugin.getLogger().info(player.getName() + " teleported to VoidAuthWorld.");
    }

    /**
     * Verifica si el mundo void está activo
     */
    public boolean isVoidWorldActive() {
        return voidWorld != null && plugin.getConfigManager().isVoidWorldEnabled();
    }

    /**
     * Obtiene el mundo void
     */
    public World getVoidWorld() {
        return voidWorld;
    }

    /**
     * Obtiene la ubicación de spawn del void
     */
    public Location getVoidSpawnLocation() {
        return voidSpawnLocation;
    }

    /**
     * Verifica si un jugador está en el mundo void
     */
    public boolean isInVoidWorld(Player player) {
        return voidWorld != null && player.getWorld().equals(voidWorld);
    }

    /**
     * Limpieza al desactivar el plugin
     */
    public void shutdown() {
        if (voidWorld != null) {
            plugin.getLogger().info("Unloading VoidAuthWorld...");

            // Teletransportar todos los jugadores del void al spawn principal
            for (Player player : voidWorld.getPlayers()) {
                Location mainSpawn = plugin.getConfigManager().getSpawnLocation();
                if (mainSpawn != null) {
                    player.teleport(mainSpawn);
                } else {
                    player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                }
            }

            // No descargamos el mundo para evitar problemas, solo registramos
            plugin.getLogger().info("VoidAuthWorld prepared for shutdown.");
        }
    }

    /**
     * Generador de mundo completamente vacío
     */
    public static class VoidWorldGenerator extends ChunkGenerator {

        @Override
        public ChunkData generateChunkData(World world, java.util.Random random, int x, int z, BiomeGrid biome) {
            // Configurar bioma predeterminado (PLAINS) para evitar warnings
            for (int bx = 0; bx < 16; bx++) {
                for (int bz = 0; bz < 16; bz++) {
                    for (int by = world.getMinHeight(); by < world.getMaxHeight(); by++) {
                        biome.setBiome(bx, by, bz, org.bukkit.block.Biome.PLAINS);
                    }
                }
            }

            // Retornar chunk vacío - no generamos bloques
            return createChunkData(world);
        }
    }
}
