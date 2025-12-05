package dev.tomle.phoenixlogin.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called before a player registers a new account.
 * <p>
 * This event is cancellable. If cancelled, the registration will be blocked.
 * </p>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * @EventHandler
 * public void onPreRegister(PreRegisterEvent event) {
 *     Player player = event.getPlayer();
 *     
 *     // Require players to be on whitelist before registering
 *     if (!player.isWhitelisted()) {
 *         event.setCancelled(true);
 *         event.setCancelMessage("§cYou must be whitelisted to register!");
 *     }
 * }
 * }</pre>
 * 
 * @author TomLe
 * @version 1.2.0
 * @since 1.2.0
 */
public class PreRegisterEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private String cancelMessage = null;
    
    private final Player player;
    private final String password;
    
    /**
     * Creates a new PreRegisterEvent.
     * 
     * @param player The player attempting to register
     * @param password The password they want to use
     */
    public PreRegisterEvent(@NotNull Player player, @NotNull String password) {
        this.player = player;
        this.password = password;
    }
    
    /**
     * Gets the player attempting to register.
     * 
     * @return The player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Gets the password the player wants to use.
     * <p>
     * <strong>Warning:</strong> This is the plain-text password. Handle with care.
     * </p>
     * 
     * @return The password
     */
    @NotNull
    public String getPassword() {
        return password;
    }
    
    /**
     * Gets the message to send to the player if the event is cancelled.
     * 
     * @return The cancel message, or null if not set
     */
    public String getCancelMessage() {
        return cancelMessage;
    }
    
    /**
     * Sets the message to send to the player if the event is cancelled.
     * 
     * @param message The message to send
     */
    public void setCancelMessage(String message) {
        this.cancelMessage = message;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
