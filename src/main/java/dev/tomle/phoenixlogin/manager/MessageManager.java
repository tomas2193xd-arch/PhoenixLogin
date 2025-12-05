package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final PhoenixLogin plugin;
    private FileConfiguration messages;
    private String prefix;

    public MessageManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        String language = plugin.getConfigManager().getLanguage();
        File messagesFile = new File(plugin.getDataFolder(), "messages_" + language + ".yml");

        if (!messagesFile.exists()) {
            plugin.getLogger()
                    .warning("Messages file for language '" + language + "' not found! Using Spanish as default.");
            messagesFile = new File(plugin.getDataFolder(), "messages_es.yml");
        }

        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
        this.prefix = colorize(messages.getString("prefix", "&6&lPhoenixLogin &8»&r"));

        plugin.getLogger().info("Loaded messages for language: " + language);
    }

    public void reload() {
        loadMessages();
    }

    public String getMessage(String path) {
        String message = messages.getString(path);
        if (message == null) {
            plugin.getLogger().warning("Message not found: " + path);
            return "&cMessage not found: " + path;
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

    // Adventure API methods
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

    // Utility methods
    public String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getPrefix() {
        return prefix;
    }

    // Helper para crear mapas de placeholders rápidamente
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
