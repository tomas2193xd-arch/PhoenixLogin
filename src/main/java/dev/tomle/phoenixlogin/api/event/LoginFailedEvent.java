package dev.tomle.phoenixlogin.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player fails a login attempt.
 * <p>
 * This event is NOT cancellable.
 * </p>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * @EventHandler
 * public void onLoginFailed(LoginFailedEvent event) {
 *     Player player = event.getPlayer();
 *     
 *     // Log to custom security system
 *     securityLogger.log(player.getName() + " failed login: " + event.getReason());
 *     
 *     // Kick player after 5 attempts
 *     if (event.getAttempts() >= 5) {
 *         player.kickPlayer("§cToo many failed attempts!");
 *     }
 * }
 * }</pre>
 * 
 * @author TomLe
 * @version 1.2.0
 * @since 1.2.0
 */
public class LoginFailedEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final FailReason reason;
    private final int attempts;
    
    /**
     * Creates a new LoginFailedEvent.
     * 
     * @param player The player who failed to login
     * @param reason The reason for failure
     * @param attempts The number of failed attempts so far
     */
    public LoginFailedEvent(@NotNull Player player, @NotNull FailReason reason, int attempts) {
        this.player = player;
        this.reason = reason;
        this.attempts = attempts;
    }
    
    /**
     * Gets the player who failed to login.
     * 
     * @return The player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Gets the reason why the login failed.
     * 
     * @return The fail reason
     */
    @NotNull
    public FailReason getReason() {
        return reason;
    }
    
    /**
     * Gets the number of failed login attempts for this player.
     * 
     * @return The number of attempts
     */
    public int getAttempts() {
        return attempts;
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
    
    /**
     * Represents the reason why a login attempt failed.
     */
    public enum FailReason {
        /**
         * The provided password was incorrect.
         */
        WRONG_PASSWORD,
        
        /**
         * The player is not registered.
         */
        NOT_REGISTERED,
        
        /**
         * The player failed the captcha challenge.
         */
        CAPTCHA_FAILED,
        
        /**
         * The player is temporarily locked out due to too many failed attempts.
         */
        LOCKED_OUT,
        
        /**
         * An internal error occurred during authentication.
         */
        INTERNAL_ERROR
    }
}
