package dev.tomle.phoenixlogin;

import dev.tomle.phoenixlogin.manager.*;
import dev.tomle.phoenixlogin.listener.*;
import dev.tomle.phoenixlogin.command.*;
import dev.tomle.phoenixlogin.api.PhoenixLoginAPI;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PhoenixLogin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private SessionManager sessionManager;
    private CaptchaManager captchaManager;
    private AuthSecurityManager authSecurityManager;
    private EffectsManager effectsManager;
    private WorldManager worldManager;
    private LocationManager locationManager;
    private MusicManager musicManager;
    private LoginHistoryManager loginHistoryManager;
    private InventoryManager inventoryManager;
    private AntiBotManager antiBotManager;
    private PremiumManager premiumManager;

    private ConnectionListener connectionListener;
    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        dev.tomle.phoenixlogin.util.ConsoleLogger.setLogger(getLogger());
        dev.tomle.phoenixlogin.util.ConsoleLogger.showBanner(getDescription().getVersion());

        // Filter passwords from console logs
        PasswordLogFilter.register();

        this.adventure = BukkitAudiences.create(this);

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();
        saveResource("messages.yml", false);

        initializeManagers();

        PhoenixLoginAPI.initialize(this);

        registerCommands();
        registerListeners();

        // bStats
        new Metrics(this, 23456);

        int playerCount = databaseManager.getRegisteredPlayersCount();
        String dbType = configManager.getDatabaseType().toUpperCase();

        dev.tomle.phoenixlogin.util.ConsoleLogger.showStartupStats(playerCount, dbType);

        if (premiumManager.isEnabled()) {
            getLogger().info("Premium (Mojang) auto-login: ENABLED");
            // Warn if server is in offline-mode (premium UUID comparison will always fail)
            if (!getServer().getOnlineMode()) {
                getLogger().warning("========================================");
                getLogger().warning("Server is in OFFLINE MODE!");
                getLogger().warning("Premium auto-login via UUID comparison");
                getLogger().warning("will NOT work unless you are behind a");
                getLogger().warning("BungeeCord/Velocity proxy with online-mode.");
                getLogger().warning("========================================");
            }
        }

        // Clean up old login history entries on startup
        int cleanupDays = configManager.getHistoryCleanupDays();
        if (cleanupDays > 0) {
            loginHistoryManager.cleanupOldEntries(cleanupDays);
        }

        long loadTime = System.currentTimeMillis() - startTime;
        dev.tomle.phoenixlogin.util.ConsoleLogger.loaded(loadTime);
    }

    @Override
    public void onDisable() {
        dev.tomle.phoenixlogin.util.ConsoleLogger.shutdown();

        // Restore inventories for any players still authenticating
        for (Player player : getServer().getOnlinePlayers()) {
            if (!sessionManager.isAuthenticated(player)) {
                locationManager.restoreLocation(player);
                inventoryManager.restoreInventory(player);
            }
        }

        if (antiBotManager != null) {
            antiBotManager.shutdown();
        }

        if (worldManager != null) {
            worldManager.shutdown();
        }

        if (musicManager != null) {
            musicManager.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
    }

    private void initializeManagers() {
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.sessionManager = new SessionManager(this);
        this.captchaManager = new CaptchaManager(this);
        this.authSecurityManager = new AuthSecurityManager(this);
        this.effectsManager = new EffectsManager(this);
        this.locationManager = new LocationManager(this);
        this.worldManager = new WorldManager(this);
        this.musicManager = new MusicManager(this);
        this.loginHistoryManager = new LoginHistoryManager(this);
        this.inventoryManager = new InventoryManager(this);
        this.antiBotManager = new AntiBotManager(this);
        this.premiumManager = new PremiumManager(this);

        databaseManager.initialize();
        worldManager.initialize();
        loginHistoryManager.initialize();
        antiBotManager.initialize();
        premiumManager.initialize();
    }

    private void registerCommands() {
        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("captcha").setExecutor(new CaptchaCommand(this));
        getCommand("changepassword").setExecutor(new ChangePasswordCommand(this));
        getCommand("unregister").setExecutor(new UnregisterCommand(this));
        getCommand("phoenixlogin").setExecutor(new AdminCommand(this));
        getCommand("phoenixlogin").setTabCompleter(new AdminCommandTabCompleter());
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
        getCommand("loginhistory").setExecutor(new LoginHistoryCommand(this));
        getCommand("premium").setExecutor(new PremiumCommand(this));
        getCommand("cracked").setExecutor(new CrackedCommand(this));
    }

    private void registerListeners() {
        this.connectionListener = new ConnectionListener(this);
        getServer().getPluginManager().registerEvents(connectionListener, this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new CaptchaListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new CleanChatListener(this), this);
    }

    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public BukkitAudiences adventure() {
        if (adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return adventure;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public CaptchaManager getCaptchaManager() {
        return captchaManager;
    }

    public AuthSecurityManager getAuthSecurityManager() {
        return authSecurityManager;
    }

    public EffectsManager getEffectsManager() {
        return effectsManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public MusicManager getMusicManager() {
        return musicManager;
    }

    public LoginHistoryManager getLoginHistoryManager() {
        return loginHistoryManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public AntiBotManager getAntiBotManager() {
        return antiBotManager;
    }

    public PremiumManager getPremiumManager() {
        return premiumManager;
    }
}
