package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Multi-language message manager for PhoenixLogin.
 * Supports: en (English), es (Spanish), pt (Portuguese)
 *
 * How it works:
 * 1. Reads "language" from config.yml (default: "en")
 * 2. Loads messages_{lang}.yml from plugin data folder
 * 3. If the file doesn't exist, copies the default from resources
 * 4. Falls back to embedded English defaults for any missing keys
 * 5. Users can customize their language file freely
 * 6. Changing language in config + /plogin reload switches all messages
 */
public class MessageManager {

    private final PhoenixLogin plugin;
    private FileConfiguration messages;
    private FileConfiguration fallbackMessages; // English defaults for missing keys
    private String prefix;
    private String currentLanguage;

    private static final String[] SUPPORTED_LANGUAGES = {"en", "es", "pt"};

    public MessageManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    /**
     * Loads (or reloads) messages for the configured language.
     */
    public void loadMessages() {
        String lang = plugin.getConfig().getString("language", "en").toLowerCase().trim();

        // Validate language
        boolean supported = false;
        for (String s : SUPPORTED_LANGUAGES) {
            if (s.equals(lang)) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            plugin.getLogger().warning("Unsupported language '" + lang + "', falling back to 'en'.");
            plugin.getLogger().warning("Supported languages: en, es, pt");
            lang = "en";
        }

        this.currentLanguage = lang;

        // Migrate old messages.yml → messages_en.yml if needed
        migrateOldMessages();

        // Ensure the language file exists in the data folder
        File langFile = getLanguageFile(lang);
        if (!langFile.exists()) {
            saveDefaultLanguageFile(lang);
        }

        // Load the user's language file (may have customizations)
        this.messages = YamlConfiguration.loadConfiguration(langFile);

        // Load embedded English defaults as fallback
        this.fallbackMessages = loadEmbeddedDefaults("en");

        // Merge: add any missing keys from embedded defaults to user's file
        mergeDefaults(langFile, lang);

        this.prefix = colorize(messages.getString("prefix", "&8[&6PhoenixLogin&8] &r"));

        if (!plugin.getConfigManager().isCleanConsole()) {
            plugin.getLogger().info("Language loaded: " + lang.toUpperCase() +
                    " (" + langFile.getName() + ")");
        }
    }

    /**
     * Migrates the old messages.yml to messages_en.yml if it exists
     * and messages_en.yml doesn't exist yet.
     */
    private void migrateOldMessages() {
        File oldFile = new File(plugin.getDataFolder(), "messages.yml");
        File newFile = getLanguageFile("en");

        if (oldFile.exists() && !newFile.exists()) {
            // Ensure lang directory exists
            newFile.getParentFile().mkdirs();

            // Copy old messages.yml as messages_en.yml
            try {
                java.nio.file.Files.copy(oldFile.toPath(), newFile.toPath());
                plugin.getLogger().info("Migrated messages.yml → lang/messages_en.yml");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to migrate messages.yml: " + e.getMessage());
            }
        }
    }

    /**
     * Saves the embedded default language file from resources to the data folder.
     */
    private void saveDefaultLanguageFile(String lang) {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        String resourcePath = "lang/messages_" + lang + ".yml";
        try {
            InputStream input = plugin.getResource(resourcePath);
            if (input != null) {
                File target = getLanguageFile(lang);
                java.nio.file.Files.copy(input, target.toPath());
                input.close();
                plugin.getLogger().info("Created language file: " + target.getName());
            } else {
                plugin.getLogger().warning("Language resource not found: " + resourcePath);
                // If resource doesn't exist, fall back to English
                if (!lang.equals("en")) {
                    saveDefaultLanguageFile("en");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save language file: " + e.getMessage());
        }
    }

    /**
     * Loads embedded default messages from the JAR resources.
     */
    private FileConfiguration loadEmbeddedDefaults(String lang) {
        String resourcePath = "lang/messages_" + lang + ".yml";
        InputStream input = plugin.getResource(resourcePath);
        if (input != null) {
            InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            return YamlConfiguration.loadConfiguration(reader);
        }
        return new YamlConfiguration();
    }

    /**
     * Merges missing keys from embedded defaults into the user's file.
     * This ensures new messages added in updates are available.
     */
    private void mergeDefaults(File langFile, String lang) {
        FileConfiguration embedded = loadEmbeddedDefaults(lang);
        boolean modified = false;

        for (String key : embedded.getKeys(true)) {
            if (!messages.contains(key) && !embedded.isConfigurationSection(key)) {
                messages.set(key, embedded.get(key));
                modified = true;
            }
        }

        if (modified) {
            try {
                messages.save(langFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save merged language file: " + e.getMessage());
            }
        }
    }

    private File getLanguageFile(String lang) {
        return new File(plugin.getDataFolder(), "lang/messages_" + lang + ".yml");
    }

    public void reload() {
        loadMessages();
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    // === MESSAGE RETRIEVAL ===

    public String getMessage(String path) {
        String message = messages.getString(path);

        // Fallback to English defaults
        if (message == null && fallbackMessages != null) {
            message = fallbackMessages.getString(path);
        }

        if (message == null) {
            plugin.getLogger().warning("Missing message key: " + path + " [" + currentLanguage + "]");
            return "&cMissing: " + path;
        }
        return colorize(message);
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public String getMessageWithPrefix(String path) {
        return prefix + " " + getMessage(path);
    }

    public String getMessageWithPrefix(String path, Map<String, String> placeholders) {
        return prefix + " " + getMessage(path, placeholders);
    }

    // === SEND TO PLAYER ===

    public void sendMessage(Player player, String path) {
        player.sendMessage(getMessageWithPrefix(path));
    }

    public void sendMessage(Player player, String path, Map<String, String> placeholders) {
        player.sendMessage(getMessageWithPrefix(path, placeholders));
    }

    public void sendMessageRaw(Player player, String path) {
        player.sendMessage(getMessage(path));
    }

    public void sendMessageRaw(Player player, String path, Map<String, String> placeholders) {
        player.sendMessage(getMessage(path, placeholders));
    }

    // === ADVENTURE API ===

    public Component getComponent(String path) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(getMessage(path));
    }

    public Component getComponent(String path, Map<String, String> placeholders) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(getMessage(path, placeholders));
    }

    public void sendComponent(Player player, String path) {
        plugin.adventure().player(player).sendMessage(
                LegacyComponentSerializer.legacyAmpersand().deserialize(getMessageWithPrefix(path)));
    }

    public void sendComponent(Player player, String path, Map<String, String> placeholders) {
        plugin.adventure().player(player).sendMessage(
                LegacyComponentSerializer.legacyAmpersand().deserialize(getMessageWithPrefix(path, placeholders)));
    }

    // === UTILITIES ===

    public String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * Helper to quickly create placeholder maps.
     */
    public static Map<String, String> createPlaceholders(String... pairs) {
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i + 1 < pairs.length) {
                placeholders.put(pairs[i], pairs[i + 1]);
            }
        }
        return placeholders;
    }
}
