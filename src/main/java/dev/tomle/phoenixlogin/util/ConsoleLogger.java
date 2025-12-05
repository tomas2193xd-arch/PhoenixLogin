package dev.tomle.phoenixlogin.util;

import org.bukkit.Bukkit;

/**
 * Sistema de logging profesional con colores ANSI y ASCII art
 * Hace que los logs del plugin se vean hermosos en la consola
 */
public class ConsoleLogger {

    // Códigos de color ANSI
    private static final String RESET = "\u001B[0m";
    private static final String BLACK = "\u001B[30m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";

    // Colores brillantes
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";
    private static final String BRIGHT_BLUE = "\u001B[94m";
    private static final String BRIGHT_PURPLE = "\u001B[95m";
    private static final String BRIGHT_CYAN = "\u001B[96m";
    private static final String BRIGHT_WHITE = "\u001B[97m";

    // Estilos
    private static final String BOLD = "\u001B[1m";
    private static final String UNDERLINE = "\u001B[4m";

    // Prefijo del plugin
    private static final String PREFIX = BRIGHT_CYAN + "[" + BRIGHT_YELLOW + "PhoenixLogin" + BRIGHT_CYAN + "]" + RESET;

    /**
     * Muestra el banner épico del plugin al iniciar
     */
    public static void showBanner(String version) {
        String[] banner = {
                "",
                BRIGHT_YELLOW + "    ____  __                   _       __                _       ",
                BRIGHT_YELLOW + "   / __ \\/ /_  ____  ___  ____(_)  __ / /   ____  ____ _(_)___   ",
                BRIGHT_RED + "  / /_/ / __ \\/ __ \\/ _ \\/ __ / / |/_// /   / __ \\/ __ `/ / __ \\  ",
                BRIGHT_RED + " / ____/ / / / /_/ /  __/ / / / />  < / /___/ /_/ / /_/ / / / / /  ",
                BRIGHT_RED + "/_/   /_/ /_/\\____/\\___/_/ /_/_/_/|_|/_____/\\____/\\__, /_/_/ /_/   ",
                BRIGHT_YELLOW + "                                                /____/            " + RESET,
                "",
                BRIGHT_CYAN + "    ╔═══════════════════════════════════════════════════════════╗",
                BRIGHT_CYAN + "    ║  " + BRIGHT_WHITE + BOLD + "Advanced Authentication System for Minecraft" + RESET
                        + BRIGHT_CYAN + "          ║",
                BRIGHT_CYAN + "    ║  " + BRIGHT_GREEN + "Version: " + BRIGHT_WHITE + version + RESET + BRIGHT_CYAN
                        + "                                          ║",
                BRIGHT_CYAN + "    ║  " + BRIGHT_GREEN + "Author: " + BRIGHT_WHITE + "TomLe (Tomas2193)" + RESET
                        + BRIGHT_CYAN + "                        ║",
                BRIGHT_CYAN + "    ║  " + BRIGHT_GREEN + "GitHub: " + BRIGHT_WHITE
                        + "github.com/tomas2193xd-arch/PhoenixLogin" + RESET + BRIGHT_CYAN + " ║",
                BRIGHT_CYAN + "    ╚═══════════════════════════════════════════════════════════╝" + RESET,
                ""
        };

        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(line + RESET);
        }
    }

    /**
     * Muestra el logo alternativo (más compacto) del phoenix
     */
    public static void showCompactLogo() {
        String[] logo = {
                "",
                BRIGHT_RED + "         ▄▀▀▀▀▀▀▀▀▀▀▄",
                BRIGHT_RED + "        █" + BRIGHT_YELLOW + "  ◣     ◢  " + BRIGHT_RED + "█",
                BRIGHT_YELLOW + "        █" + BRIGHT_RED + "   ▀▄ ▄▀   " + BRIGHT_YELLOW + "█",
                BRIGHT_YELLOW + "         █" + BRIGHT_RED + "   ███   " + BRIGHT_YELLOW + "█",
                BRIGHT_YELLOW + "          ▀▄" + BRIGHT_RED + " ███ " + BRIGHT_YELLOW + "▄▀     " + BRIGHT_CYAN
                        + "PhoenixLogin",
                BRIGHT_RED + "            ▀███▀      " + BRIGHT_WHITE + "Your server, your rules",
                RESET
        };

        for (String line : logo) {
            Bukkit.getConsoleSender().sendMessage(line + RESET);
        }
    }

    /**
     * Log de información general
     */
    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + " " + BRIGHT_WHITE + message + RESET);
    }

    /**
     * Log de éxito
     */
    public static void success(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + " " + BRIGHT_GREEN + "✓ " + message + RESET);
    }

    /**
     * Log de advertencia
     */
    public static void warn(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + " " + BRIGHT_YELLOW + "⚠ " + message + RESET);
    }

    /**
     * Log de error
     */
    public static void error(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + " " + BRIGHT_RED + "✖ " + message + RESET);
    }

    /**
     * Log de debug (con símbolo especial)
     */
    public static void debug(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + " " + BRIGHT_PURPLE + "◆ [DEBUG] " + message + RESET);
    }

    /**
     * Log de seguridad (eventos importantes)
     */
    public static void security(String message) {
        Bukkit.getConsoleSender()
                .sendMessage(PREFIX + " " + BRIGHT_RED + "🛡 [SECURITY] " + BRIGHT_WHITE + message + RESET);
    }

    /**
     * Muestra una línea separadora bonita
     */
    public static void separator() {
        Bukkit.getConsoleSender()
                .sendMessage(BRIGHT_CYAN + "    ═══════════════════════════════════════════════════════════" + RESET);
    }

    /**
     * Muestra estadísticas de inicio
     */
    public static void showStartupStats(int players, String database, String language) {
        separator();
        info(BRIGHT_CYAN + "▸ " + BRIGHT_WHITE + "Database Type: " + BRIGHT_GREEN + database);
        info(BRIGHT_CYAN + "▸ " + BRIGHT_WHITE + "Default Language: " + BRIGHT_GREEN + language);
        info(BRIGHT_CYAN + "▸ " + BRIGHT_WHITE + "Registered Players: " + BRIGHT_GREEN + players);
        info(BRIGHT_CYAN + "▸ " + BRIGHT_WHITE + "Security Level: " + BRIGHT_GREEN + "Maximum");
        separator();
    }

    /**
     * Muestra las características cargadas
     */
    public static void showFeatures() {
        info(BRIGHT_PURPLE + "Loading features:" + RESET);
        success("BCrypt Password Encryption");
        success("Anti-Bot Captcha System");
        success("Void Authentication World");
        success("Session Management");
        success("Brute-Force Protection");
        success("Multi-Language Support");
    }

    /**
     * Mensaje de carga completada
     */
    public static void loaded(long loadTime) {
        separator();
        Bukkit.getConsoleSender().sendMessage(
                BRIGHT_CYAN + "    ║ " + BRIGHT_GREEN + BOLD + "✓ Plugin loaded successfully in " + loadTime + "ms"
                        + RESET + BRIGHT_CYAN + "");
        Bukkit.getConsoleSender().sendMessage(
                BRIGHT_CYAN + "    ║ " + BRIGHT_YELLOW + "Ready to protect your server!" + RESET);
        separator();
        Bukkit.getConsoleSender().sendMessage("");
    }

    /**
     * Mensaje de desactivación
     */
    public static void shutdown() {
        Bukkit.getConsoleSender().sendMessage("");
        separator();
        Bukkit.getConsoleSender().sendMessage(
                BRIGHT_CYAN + "    ║ " + BRIGHT_YELLOW + "◈ " + BRIGHT_WHITE + "PhoenixLogin is shutting down..."
                        + RESET);
        separator();
        Bukkit.getConsoleSender().sendMessage("");
    }

    /**
     * Muestra información de un jugador (login/register)
     */
    public static void playerAction(String action, String player, String ip) {
        Bukkit.getConsoleSender().sendMessage(
                PREFIX + " " + BRIGHT_CYAN + action + " " + BRIGHT_WHITE + player +
                        BRIGHT_CYAN + " from " + BRIGHT_YELLOW + ip + RESET);
    }
}
