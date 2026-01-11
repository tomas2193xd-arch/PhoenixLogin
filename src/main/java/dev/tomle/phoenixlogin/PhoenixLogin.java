package dev.tomle.phoenixlogin;

import dev.tomle.phoenixlogin.manager.*;
import dev.tomle.phoenixlogin.listener.*;
import dev.tomle.phoenixlogin.command.*;
import dev.tomle.phoenixlogin.api.PhoenixLoginAPI;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
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

    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // Mostrar banner épico
        dev.tomle.phoenixlogin.util.ConsoleLogger.showBanner(getDescription().getVersion());

        // 🔒 SEGURIDAD: Filtrar contraseñas de los logs de consola
        dev.tomle.phoenixlogin.listener.PasswordLogFilter.register();

        this.adventure = BukkitAudiences.create(this);

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();
        saveResource("messages_es.yml", false);
        saveResource("messages_en.yml", false);

        initializeManagers();

        // Initialize API for external plugins
        PhoenixLoginAPI.initialize(this);

        registerCommands();
        registerListeners();

        // Initialize bStats (silent)
        int pluginId = 23456;
        new Metrics(this, pluginId);

        // Mostrar estadísticas
        int playerCount = databaseManager.getRegisteredPlayersCount();
        String dbType = configManager.getDatabaseType().toUpperCase();
        String language = configManager.getLanguage().toUpperCase();

        dev.tomle.phoenixlogin.util.ConsoleLogger.showStartupStats(playerCount, dbType, language);

        long loadTime = System.currentTimeMillis() - startTime;
        dev.tomle.phoenixlogin.util.ConsoleLogger.loaded(loadTime);
    }

    @Override
    public void onDisable() {
        dev.tomle.phoenixlogin.util.ConsoleLogger.shutdown();

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

        databaseManager.initialize();
        worldManager.initialize();
        loginHistoryManager.initialize();
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
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
    }

    private void registerListeners() {

        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new CaptchaListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatBlockListener(this), this);
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
}
