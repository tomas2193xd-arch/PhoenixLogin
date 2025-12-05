package dev.tomle.phoenixlogin.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called after a player successfully logs in.
 * <p>
 * This event is NOT cancellable as the player has already been authenticated.
 * </p>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * @EventHandler
 * public void onPlayerLogin(PlayerLoginEvent event) {
 *     Player player = event.getPlayer();
 *     
 *     // Give welcome items
 *     player.getInventory().addItem(new ItemStack(Material.DIAMOND));
 *     
 *     // Send custom message
 *     player.sendMessage("§aWelcome back!");
 * }
 * }</pre>
 * 
 * @author TomLe
 * @version 1.2.0
 * @since 1.2.0
 */
public class PlayerLoginEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final boolean fromSession;
    
    /**
     * Creates a new PlayerLoginEvent.
     * 
     * @param player The player who logged in
     * @param fromSession Whether the login was from a valid session
     */
    public PlayerLoginEvent(@NotNull Player player, boolean fromSession) {
        this.player = player;
        this.fromSession = fromSession;
    }
    
    /**
     * Gets the player who logged in.
     * 
     * @return The player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Checks if the player logged in using a session.
     * <p>
     * If true, the player was automatically authenticated because they had
     * a valid session from a previous login.
     * </p>
     * 
     * @return true if logged in from session, false if from password
     */
    public boolean isFromSession() {
        return fromSession;
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
