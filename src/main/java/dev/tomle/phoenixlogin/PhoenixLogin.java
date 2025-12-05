package dev.tomle.phoenixlogin;

import dev.tomle.phoenixlogin.manager.*;
import dev.tomle.phoenixlogin.listener.*;
import dev.tomle.phoenixlogin.command.*;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
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

    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        getLogger().info("=================================");
        getLogger().info("  PhoenixLogin - Starting...");
        getLogger().info("=================================");

        this.adventure = BukkitAudiences.create(this);

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();
        saveResource("messages_es.yml", false);
        saveResource("messages_en.yml", false);

        initializeManagers();
        registerCommands();
        registerListeners();

        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info("=================================");
        getLogger().info("  PhoenixLogin - Enabled!");
        getLogger().info("  Language: " + configManager.getLanguage());
        getLogger().info("  Database: " + configManager.getDatabaseType());
        getLogger().info("  Load time: " + loadTime + "ms");
        getLogger().info("=================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("=================================");
        getLogger().info("  PhoenixLogin - Disabling...");
        getLogger().info("=================================");

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

        getLogger().info("=================================");
        getLogger().info("  PhoenixLogin - Disabled!");
        getLogger().info("=================================");
    }

    private void initializeManagers() {
        getLogger().info("Initializing managers...");

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

        databaseManager.initialize();
        worldManager.initialize();

        getLogger().info("All managers initialized successfully!");
    }

    private void registerCommands() {
        getLogger().info("Registering commands...");

        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("changepassword").setExecutor(new ChangePasswordCommand(this));
        getCommand("unregister").setExecutor(new UnregisterCommand(this));
        getCommand("phoenixlogin").setExecutor(new AdminCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));

        getLogger().info("Commands registered successfully!");
    }

    private void registerListeners() {
        getLogger().info("Registering listeners...");

        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new CaptchaListener(this), this);

        getLogger().info("Listeners registered successfully!");
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
}
