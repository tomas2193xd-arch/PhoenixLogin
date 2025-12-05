# PhoenixLogin API Documentation

## Table of Contents
- [Getting Started](#getting-started)
- [Maven/Gradle Setup](#mavengradle-setup)
- [API Reference](#api-reference)
- [Events](#events)
- [Examples](#examples)

---

## Getting Started

The PhoenixLogin API provides a comprehensive interface for external plugins to interact with the authentication system. All methods are thread-safe and well-documented with JavaDoc.

### Quick Example

```java
// Get API instance
PhoenixLoginAPI api = PhoenixLoginAPI.getInstance();

// Check if player is authenticated
if (api.isAuthenticated(player)) {
    player.sendMessage("You are logged in!");
}

// Get player data asynchronously
api.getPlayerDataAsync(player.getUniqueId()).thenAccept(data -> {
    data.ifPresent(playerData -> {
        // Do something with player data
    });
});
```

---

## Maven/Gradle Setup

### Maven

Add PhoenixLogin as a dependency in your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>dev.tomle</groupId>
        <artifactId>PhoenixLogin</artifactId>
        <version>1.2.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Gradle

Add PhoenixLogin as a dependency in your `build.gradle`:

```gradle
dependencies {
    compileOnly 'dev.tomle:PhoenixLogin:1.2.0'
}
```

### plugin.yml

Add PhoenixLogin as a dependency:

```yaml
depend: [PhoenixLogin]
```

Or as a soft dependency:

```yaml
softdepend: [PhoenixLogin]
```

---

## API Reference

### PhoenixLoginAPI

**Location**: `dev.tomle.phoenixlogin.api.PhoenixLoginAPI`

#### Getting the API Instance

```java
PhoenixLoginAPI api = PhoenixLoginAPI.getInstance();
```

**Throws**: `IllegalStateException` if PhoenixLogin is not loaded.

#### Authentication Methods

##### isAuthenticated(Player)
Checks if a player is currently authenticated.

```java
boolean isAuthenticated(Player player)
```

**Parameters**:
- `player` - The player to check

**Returns**: `true` if authenticated, `false` otherwise

**Throws**: `NullPointerException` if player is null

**Example**:
```java
if (api.isAuthenticated(player)) {
    // Player is logged in
}
```

---

##### forceAuthenticate(Player)
Forces a player to be authenticated without password verification.

```java
void forceAuthenticate(Player player)
```

**Parameters**:
- `player` - The player to authenticate

**Throws**: `NullPointerException` if player is null

**Warning**: Use with caution! This bypasses all authentication checks.

**Example**:
```java
// Authenticate player through custom OAuth system
if (oauthSystem.verify(player)) {
    api.forceAuthenticate(player);
}
```

---

##### forceLogout(Player)
Forces a player to logout and reapply login restrictions.

```java
void forceLogout(Player player)
```

**Parameters**:
- `player` - The player to logout

**Throws**: `NullPointerException` if player is null

**Example**:
```java
// Force logout on suspicious activity
api.forceLogout(player);
player.sendMessage("§cSecurity alert! Please login again.");
```

---

#### Player Data Methods

##### isRegisteredAsync(UUID)
Checks if a player is registered in the database (asynchronous).

```java
CompletableFuture<Boolean> isRegisteredAsync(UUID uuid)
```

**Parameters**:
- `uuid` - The player's UUID

**Returns**: CompletableFuture that completes with `true` if registered

**Throws**: `NullPointerException` if uuid is null

**Example**:
```java
api.isRegisteredAsync(player.getUniqueId()).thenAccept(registered -> {
    if (registered) {
        player.sendMessage("§aWelcome back!");
    } else {
        player.sendMessage("§eYou need to register!");
    }
});
```

---

##### getPlayerDataAsync(UUID)
Retrieves player data from the database (asynchronous).

```java
CompletableFuture<Optional<PlayerData>> getPlayerDataAsync(UUID uuid)
```

**Parameters**:
- `uuid` - The player's UUID

**Returns**: CompletableFuture containing Optional of PlayerData

**Throws**: `NullPointerException` if uuid is null

**Example**:
```java
api.getPlayerDataAsync(player.getUniqueId()).thenAccept(dataOpt -> {
    dataOpt.ifPresent(data -> {
        // Access player data
        boolean has2FA = data.isTwoFactorEnabled();
    });
});
```

---

##### has2FAEnabled(UUID)
Checks if a player has two-factor authentication enabled (asynchronous).

```java
CompletableFuture<Boolean> has2FAEnabled(UUID uuid)
```

**Parameters**:
- `uuid` - The player's UUID

**Returns**: CompletableFuture that completes with `true` if 2FA is enabled

**Throws**: `NullPointerException` if uuid is null

**Example**:
```java
api.has2FAEnabled(player.getUniqueId()).thenAccept(enabled -> {
    if (enabled) {
        player.sendMessage("§a2FA is enabled on your account!");
    }
});
```

---

#### Statistics Methods

##### getTotalRegisteredPlayers()
Gets the total number of registered players (asynchronous).

```java
CompletableFuture<Integer> getTotalRegisteredPlayers()
```

**Returns**: CompletableFuture containing the player count

**Example**:
```java
api.getTotalRegisteredPlayers().thenAccept(count -> {
    Bukkit.broadcastMessage("§e" + count + " §7players have registered!");
});
```

---

#### Configuration Methods

##### areSessionsEnabled()
Checks if sessions are enabled in the configuration.

```java
boolean areSessionsEnabled()
```

**Returns**: `true` if sessions are enabled

---

##### getSessionDuration()
Gets the configured session duration in seconds.

```java
int getSessionDuration()
```

**Returns**: Session duration in seconds

---

##### getMaxLoginAttempts()
Gets the maximum allowed login attempts before lockout.

```java
int getMaxLoginAttempts()
```

**Returns**: Maximum login attempts

---

##### getLockoutDuration()
Gets the lockout duration in seconds.

```java
int getLockoutDuration()
```

**Returns**: Lockout duration in seconds

---

## Events

PhoenixLogin provides several events for integrating with the authentication lifecycle.

### PreLoginEvent

**Location**: `dev.tomle.phoenixlogin.api.event.PreLoginEvent`

Called before a player attempts to login. **Cancellable**.

```java
@EventHandler
public void onPreLogin(PreLoginEvent event) {
    Player player = event.getPlayer();
    String password = event.getPassword();
    
    // Block login based on custom logic
    if (customBanSystem.isBanned(player)) {
        event.setCancelled(true);
        event.setCancelMessage("§cYou are banned!");
    }
}
```

**Methods**:
- `Player getPlayer()` - Gets the player attempting to login
- `String getPassword()` - Gets the password (use with care!)
- `boolean isCancelled()` - Checks if cancelled
- `void setCancelled(boolean)` - Cancel the login
- `String getCancelMessage()` - Gets the cancel message
- `void setCancelMessage(String)` - Sets the cancel message

---

### PlayerLoginEvent

**Location**: `dev.tomle.phoenixlogin.api.event.PlayerLoginEvent`

Called after a player successfully logs in. **Not cancellable**.

```java
@EventHandler
public void onPlayerLogin(PlayerLoginEvent event) {
    Player player = event.getPlayer();
    boolean fromSession = event.isFromSession();
    
    if (fromSession) {
        player.sendMessage("§aWelcome back! (auto-login)");
    } else {
        player.sendMessage("§aSuccessfully logged in!");
        // Give login rewards
        player.getInventory().addItem(new ItemStack(Material.DIAMOND));
    }
}
```

**Methods**:
- `Player getPlayer()` - Gets the player who logged in
- `boolean isFromSession()` - Returns true if session-based login

---

### PreRegisterEvent

**Location**: `dev.tomle.phoenixlogin.api.event.PreRegisterEvent`

Called before a player registers a new account. **Cancellable**.

```java
@EventHandler
public void onPreRegister(PreRegisterEvent event) {
    Player player = event.getPlayer();
    String password = event.getPassword();
    
    // Require whitelist before registration
    if (!player.isWhitelisted()) {
        event.setCancelled(true);
        event.setCancelMessage("§cYou must be whitelisted to register!");
    }
    
    // Check password strength
    if (password.length() < 8) {
        event.setCancelled(true);
        event.setCancelMessage("§cPassword must be at least 8 characters!");
    }
}
```

**Methods**:
- `Player getPlayer()` - Gets the player attempting to register
- `String getPassword()` - Gets the password
- `boolean isCancelled()` - Checks if cancelled
- `void setCancelled(boolean)` - Cancel the registration
- `String getCancelMessage()` - Gets the cancel message
- `void setCancelMessage(String)` - Sets the cancel message

---

### PlayerRegisterEvent

**Location**: `dev.tomle.phoenixlogin.api.event.PlayerRegisterEvent`

Called after a player successfully registers. **Not cancellable**.

```java
@EventHandler
public void onPlayerRegister(PlayerRegisterEvent event) {
    Player player = event.getPlayer();
    
    // Give starter kit
    player.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
    player.getInventory().addItem(new ItemStack(Material.BREAD, 16));
    
    // Broadcast to server
    Bukkit.broadcastMessage("§e" + player.getName() + " §7just registered!");
}
```

**Methods**:
- `Player getPlayer()` - Gets the player who registered

---

### LoginFailedEvent

**Location**: `dev.tomle.phoenixlogin.api.event.LoginFailedEvent`

Called when a login attempt fails. **Not cancellable**.

```java
@EventHandler
public void onLoginFailed(LoginFailedEvent event) {
    Player player = event.getPlayer();
    FailReason reason = event.getReason();
    int attempts = event.getAttempts();
    
    // Log to custom security system
    securityLogger.log(player.getName() + " failed login: " + reason);
    
    // Kick after 5 attempts
    if (attempts >= 5) {
        player.kickPlayer("§cToo many failed attempts!");
    }
}
```

**Methods**:
- `Player getPlayer()` - Gets the player who failed to login
- `FailReason getReason()` - Gets the failure reason
- `int getAttempts()` - Gets the number of failed attempts

**FailReason Enum**:
- `WRONG_PASSWORD` - Incorrect password
- `NOT_REGISTERED` - Player not registered
- `CAPTCHA_FAILED` - Failed captcha challenge
- `LOCKED_OUT` - Temporarily locked out
- `INTERNAL_ERROR` - Internal error occurred

---

## Examples

### Example 1: Reward System

```java
public class LoginRewards implements Listener {
    
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        
        // Don't reward session logins
        if (event.isFromSession()) {
            return;
        }
        
        // Give daily reward
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
        player.sendMessage("§a+5 Diamonds for logging in!");
    }
}
```

### Example 2: Security Monitoring

```java
public class SecurityMonitor implements Listener {
    
    private final Map<UUID, Integer> failedAttempts = new HashMap<>();
    
    @EventHandler
    public void onLoginFailed(LoginFailedEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Track failed attempts
        int attempts = failedAttempts.getOrDefault(uuid, 0) + 1;
        failedAttempts.put(uuid, attempts);
        
        // Alert admins on suspicious activity
        if (attempts >= 10) {
            Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("security.alerts"))
                .forEach(admin -> admin.sendMessage(
                    "§c[SECURITY] " + player.getName() + " has " + attempts + " failed login attempts!"
                ));
        }
    }
    
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        // Clear failed attempts on successful login
        failedAttempts.remove(event.getPlayer().getUniqueId());
    }
}
```

### Example 3: Custom Authentication Integration

```java
public class CustomAuthIntegration {
    
    private final PhoenixLoginAPI api = PhoenixLoginAPI.getInstance();
    
    public void authenticateViaOAuth(Player player, String oauthToken) {
        // Verify OAuth token with external service
        myOAuthService.verify(oauthToken).thenAccept(valid -> {
            if (valid) {
                // Force authenticate the player
                api.forceAuthenticate(player);
                player.sendMessage("§aAuthenticated via OAuth!");
            } else {
                player.sendMessage("§cInvalid OAuth token!");
            }
        });
    }
}
```

---

## Version

**API Version**: 1.2.0  
**Compatible with**: PhoenixLogin 1.2.0+

---

## Support

For questions, issues, or feature requests:

- **GitHub Issues**: [Report here](https://github.com/tomas2193xd-arch/PhoenixLogin/issues)
- **Discord**: Tomas2193

---

**Made with ❤️ by TomLe**
