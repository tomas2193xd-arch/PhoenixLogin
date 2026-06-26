package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages saved player locations and state for restoring after authentication.
 * Saves: location, gamemode, flight state.
 */
public class LocationManager {

    private final PhoenixLogin plugin;
    private final Map<UUID, SavedPlayerState> savedStates;

    public LocationManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        this.savedStates = new ConcurrentHashMap<>();
    }

    /**
     * Saves the player's current location AND state (gamemode, flight).
     */
    public void saveLocation(Player player) {
        savedStates.put(player.getUniqueId(), new SavedPlayerState(
                player.getLocation(),
                player.getGameMode(),
                player.getAllowFlight(),
                player.isFlying()
        ));
    }

    /**
     * Restores the player's saved location AND state (gamemode, flight).
     * Priority: 1) Saved location, 2) /setspawn, 3) Safe location in main world, 4)
     * World spawn
     */
    public void restoreLocation(Player player) {
        UUID uuid = player.getUniqueId();
        SavedPlayerState savedState = savedStates.remove(uuid);

        Location targetLocation = null;

        // Priority 1: Saved location
        if (savedState != null && savedState.location != null && savedState.location.getWorld() != null) {
            if (Bukkit.getWorld(savedState.location.getWorld().getName()) != null) {
                targetLocation = savedState.location;
            } else {
                plugin.getLogger()
                        .warning("Saved world for " + player.getName() + " no longer exists. Using fallback.");
            }
        }

        // Priority 2: Configured spawn (/setspawn)
        if (targetLocation == null) {
            Location configSpawn = plugin.getConfigManager().getSpawnLocation();
            if (configSpawn != null && configSpawn.getWorld() != null) {
                targetLocation = configSpawn;
            }
        }

        // Priority 3: Safe location in main world
        if (targetLocation == null) {
            targetLocation = findSafeLocation();
        }

        // Priority 4: World spawn fallback
        if (targetLocation == null) {
            plugin.getLogger().warning("No safe location found for " + player.getName() + ", using world spawn.");
            if (!Bukkit.getWorlds().isEmpty()) {
                targetLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
            } else {
                plugin.getLogger().severe("CRITICAL: No worlds available for " + player.getName());
                return;
            }
        }

        // Teleport to target
        player.teleport(targetLocation);

        // Restore player state (gamemode, flight)
        if (savedState != null) {
            player.setGameMode(savedState.gameMode);
            player.setAllowFlight(savedState.allowFlight);
            if (savedState.allowFlight && savedState.flying) {
                player.setFlying(true);
            }
        } else {
            // No saved state — set to server default gamemode
            player.setGameMode(Bukkit.getDefaultGameMode());
        }
    }

    private Location findSafeLocation() {
        String voidWorldName = plugin.getConfigManager().getVoidWorldName();

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            if (!world.getName().equals(voidWorldName)) {
                Location worldSpawn = world.getSpawnLocation();
                if (isSafeLocation(worldSpawn)) {
                    return worldSpawn;
                }
                Location safeNearby = findSafeNearby(worldSpawn, 50);
                if (safeNearby != null) {
                    return safeNearby;
                }
            }
        }
        return null;
    }

    private boolean isSafeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null)
            return false;

        org.bukkit.block.Block below = loc.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
        if (!below.getType().isSolid())
            return false;

        org.bukkit.block.Block at = loc.getBlock();
        org.bukkit.block.Block above = at.getRelative(org.bukkit.block.BlockFace.UP);

        return !at.getType().isSolid() && !above.getType().isSolid();
    }

    /**
     * Efficient safe location search.
     * - Capped radius to prevent server freeze
     * - Uses getHighestBlockYAt instead of iterating all Y levels
     * - Only searches the shell of each radius (not filled)
     * - Hard limit on total iterations
     */
    private Location findSafeNearby(Location center, int radius) {
        if (center == null || center.getWorld() == null)
            return null;

        org.bukkit.World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();

        // Cap radius to prevent excessive iteration
        int maxRadius = Math.min(radius, 10);
        int maxIterations = 10000;
        int iterations = 0;

        for (int r = 0; r <= maxRadius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    // Only check shell (edges) for r > 0
                    if (r > 0 && Math.abs(x) != r && Math.abs(z) != r)
                        continue;

                    if (++iterations > maxIterations)
                        return null;

                    // Use highest block Y instead of iterating all Y levels
                    int highY = world.getHighestBlockYAt(centerX + x, centerZ + z);
                    Location testLoc = new Location(world, centerX + x, highY + 1, centerZ + z);
                    if (isSafeLocation(testLoc)) {
                        return testLoc.add(0.5, 0, 0.5);
                    }
                }
            }
        }
        return null;
    }

    public void clearLocation(UUID uuid) {
        savedStates.remove(uuid);
    }

    public boolean hasLocation(UUID uuid) {
        return savedStates.containsKey(uuid);
    }

    public Location getLocation(UUID uuid) {
        SavedPlayerState state = savedStates.get(uuid);
        return state != null ? state.location : null;
    }

    public void cleanup() {
        savedStates.clear();
    }

    /**
     * Holds a snapshot of the player's state before authentication.
     */
    private static class SavedPlayerState {
        final Location location;
        final GameMode gameMode;
        final boolean allowFlight;
        final boolean flying;

        SavedPlayerState(Location location, GameMode gameMode, boolean allowFlight, boolean flying) {
            this.location = location;
            this.gameMode = gameMode;
            this.allowFlight = allowFlight;
            this.flying = flying;
        }
    }
}
