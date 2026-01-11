package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona las ubicaciones guardadas de los jugadores
 * para restaurarlas después de la autenticación
 */
public class LocationManager {

    private final PhoenixLogin plugin;
    private final Map<UUID, Location> savedLocations;

    public LocationManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        this.savedLocations = new ConcurrentHashMap<>();
    }

    /**
     * Guarda la ubicación actual de un jugador
     */
    public void saveLocation(Player player) {
        Location location = player.getLocation();
        savedLocations.put(player.getUniqueId(), location);
        // plugin.getLogger().info("Saved location for " + player.getName() + " at: " +
        // formatLocation(location));
    }

    /**
     * Restaura la ubicación guardada de un jugador
     * Prioridad: 1) Ubicación guardada, 2) /setspawn, 3) Bloque seguro en mundo
     * principal
     */
    public void restoreLocation(Player player) {
        UUID uuid = player.getUniqueId();
        Location savedLocation = savedLocations.remove(uuid);

        // PRIORIDAD 1: Restaurar ubicación guardada (jugadores que ya estaban en el
        // servidor)
        if (savedLocation != null && savedLocation.getWorld() != null) {
            if (Bukkit.getWorld(savedLocation.getWorld().getName()) != null) {
                player.teleport(savedLocation);
                // plugin.getLogger().info("Restored location for " + player.getName() + " to: "
                // + formatLocation(savedLocation));
                return;
            } else {
                plugin.getLogger().warning("Saved world for " + player.getName() +
                        " no longer exists! Using fallback...");
            }
        }

        // PRIORIDAD 2: Spawn configurado con /setspawn
        Location configSpawn = plugin.getConfigManager().getSpawnLocation();
        if (configSpawn != null && configSpawn.getWorld() != null) {
            player.teleport(configSpawn);
            // plugin.getLogger().info("Teleported " + player.getName() + " to configured
            // spawn (/setspawn)");
            return;
        }

        // PRIORIDAD 3: Buscar bloque seguro en mundo principal
        // plugin.getLogger().info("No spawn configured, searching for safe location for
        // " + player.getName());
        Location safeLocation = findSafeLocation();
        if (safeLocation != null) {
            player.teleport(safeLocation);
            // plugin.getLogger().info("Teleported " + player.getName() + " to safe
            // location: " + formatLocation(safeLocation));
            return;
        }

        // PRIORIDAD 4: Último recurso - spawn del primer mundo disponible
        plugin.getLogger().warning("Could not find safe location, using world spawn for " + player.getName());
        if (!Bukkit.getWorlds().isEmpty()) {
            Location worldSpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
            player.teleport(worldSpawn);
        } else {
            plugin.getLogger().severe("CRITICAL: No worlds available to teleport " + player.getName());
        }
    }

    /**
     * Busca una ubicación segura en el mundo principal
     * Evita el VoidAuthWorld y busca un bloque sólido seguro
     */
    private Location findSafeLocation() {
        String voidWorldName = plugin.getConfigManager().getVoidWorldName();

        // Buscar el primer mundo que NO sea el VoidAuthWorld
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            if (!world.getName().equals(voidWorldName)) {
                // Obtener spawn del mundo
                Location worldSpawn = world.getSpawnLocation();

                // Verificar si el spawn es seguro
                if (isSafeLocation(worldSpawn)) {
                    return worldSpawn;
                }

                // Si el spawn no es seguro, buscar un bloque seguro cercano
                Location safeNearby = findSafeNearby(worldSpawn, 50);
                if (safeNearby != null) {
                    return safeNearby;
                }
            }
        }

        return null;
    }

    /**
     * Verifica si una ubicación es segura para teletransportar
     */
    private boolean isSafeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }

        // Verificar que haya un bloque sólido debajo
        org.bukkit.block.Block below = loc.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
        if (!below.getType().isSolid()) {
            return false;
        }

        // Verificar que los 2 bloques arriba estén vacíos (espacio para el jugador)
        org.bukkit.block.Block at = loc.getBlock();
        org.bukkit.block.Block above = at.getRelative(org.bukkit.block.BlockFace.UP);

        return !at.getType().isSolid() && !above.getType().isSolid();
    }

    /**
     * Busca un bloque seguro cerca de una ubicación
     */
    private Location findSafeNearby(Location center, int radius) {
        if (center == null || center.getWorld() == null) {
            return null;
        }

        org.bukkit.World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();

        // Buscar en espiral desde el centro
        for (int r = 0; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    // Buscar desde Y=64 hasta Y=255 (rango seguro)
                    for (int y = 64; y < 256; y++) {
                        Location testLoc = new Location(world, centerX + x, y, centerZ + z);
                        if (isSafeLocation(testLoc)) {
                            return testLoc.add(0.5, 0, 0.5); // Centrar en el bloque
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Limpia la ubicación guardada de un jugador
     */
    public void clearLocation(UUID uuid) {
        savedLocations.remove(uuid);
    }

    /**
     * Verifica si hay una ubicación guardada para un jugador
     */
    public boolean hasLocation(UUID uuid) {
        return savedLocations.containsKey(uuid);
    }

    /**
     * Obtiene la ubicación guardada de un jugador (puede ser null)
     */
    public Location getLocation(UUID uuid) {
        return savedLocations.get(uuid);
    }

    /**
     * Limpia todas las ubicaciones guardadas
     */
    public void cleanup() {
        savedLocations.clear();
    }

    /**
     * Formatea una ubicación para logs
     */
    private String formatLocation(Location loc) {
        if (loc == null)
            return "null";
        return String.format("%s (%.1f, %.1f, %.1f)",
                loc.getWorld() != null ? loc.getWorld().getName() : "null",
                loc.getX(), loc.getY(), loc.getZ());
    }
}
