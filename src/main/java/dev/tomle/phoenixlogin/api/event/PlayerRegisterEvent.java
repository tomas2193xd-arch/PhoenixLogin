package dev.tomle.phoenixlogin.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called after a player successfully registers a new account.
 * <p>
 * This event is NOT cancellable as the account has already been created.
 * </p>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * @EventHandler
 * public void onPlayerRegister(PlayerRegisterEvent event) {
 *     Player player = event.getPlayer();
 *     
 *     // Give starter items
 *     player.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
 *     
 *     // Broadcast to server
 *     Bukkit.broadcastMessage("§e" + player.getName() + " §7has registered!");
 * }
 * }</pre>
 * 
 * @author TomLe
 * @version 1.2.0
 * @since 1.2.0
 */
public class PlayerRegisterEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    
    /**
     * Creates a new PlayerRegisterEvent.
     * 
     * @param player The player who registered
     */
    public PlayerRegisterEvent(@NotNull Player player) {
        this.player = player;
    }
    
    /**
     * Gets the player who registered.
     * 
     * @return The player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }
    
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
