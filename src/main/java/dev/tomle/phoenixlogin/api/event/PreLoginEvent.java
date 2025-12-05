package dev.tomle.phoenixlogin.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called before a player attempts to login.
 * <p>
 * This event is cancellable. If cancelled, the login attempt will be blocked
 * and the player will receive a message (if provided via {@link #setCancelMessage(String)}).
 * </p>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * @EventHandler
 * public void onPreLogin(PreLoginEvent event) {
 *     Player player = event.getPlayer();
 *     
 *     // Block login if player is banned from custom system
 *     if (myBanSystem.isBanned(player)) {
 *         event.setCancelled(true);
 *         event.setCancelMessage("§cYou are banned from this server!");
 *     }
 * }
 * }</pre>
 * 
 * @author TomLe
 * @version 1.2.0
 * @since 1.2.0
 */
public class PreLoginEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private String cancelMessage = null;
    
    private final Player player;
    private final String password;
    
    /**
     * Creates a new PreLoginEvent.
     * 
     * @param player The player attempting to login
     * @param password The password provided by the player
     */
    public PreLoginEvent(@NotNull Player player, @NotNull String password) {
        this.player = player;
        this.password = password;
    }
    
    /**
     * Gets the player attempting to login.
     * 
     * @return The player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Gets the password provided by the player.
     * <p>
     * <strong>Warning:</strong> This is the plain-text password. Handle with care
     * and never log or store it.
     * </p>
     * 
     * @return The player's password
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
