package dev.tomle.phoenixlogin.util;

import org.bukkit.Bukkit;

/**
 * Professional logging system with ANSI colors and ASCII art
 * Making plugin logs look stunning in the console.
 */
public class ConsoleLogger {

    // ANSI Color Codes
    private static final String RESET = "\u001B[0m";
    // private static final String BLACK = "\u001B[30m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    // private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";

    // Bright Colors (High Intensity)
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";
    private static final String BRIGHT_BLUE = "\u001B[94m";
    private static final String BRIGHT_PURPLE = "\u001B[95m";
    private static final String BRIGHT_CYAN = "\u001B[96m";
    private static final String BRIGHT_WHITE = "\u001B[97m";

    // Styles
    private static final String BOLD = "\u001B[1m";
    // private static final String UNDERLINE = "\u001B[4m";

    // Plugin Prefix with Gradient-like effect
    private static final String PREFIX = BRIGHT_RED + "P" + BRIGHT_YELLOW + "hoenix" + BRIGHT_RED + "L" + BRIGHT_YELLOW
            + "ogin" + RESET;
    private static final String PREFIX_FORMAT = BRIGHT_RED + "[" + BRIGHT_YELLOW + "PhoenixLogin" + BRIGHT_RED + "] "
            + RESET;

    /**
     * Shows the EPIC banner on startup.
     * Designed to be extremely flashy and noticeable.
     */
    public static void showBanner(String version) {
        String[] banner = {
                "",
                BRIGHT_RED + " ██████╗ ██╗  ██╗ ██████╗ ███████╗███╗   ██╗██╗██╗  ██╗",
                BRIGHT_RED + " ██╔══██╗██║  ██║██╔═══██╗██╔════╝████╗  ██║██║╚██╗██╔╝",
                BRIGHT_YELLOW + " ██████╔╝███████║██║   ██║█████╗  ██╔██╗ ██║██║ ╚███╔╝ ",
                BRIGHT_YELLOW + " ██╔═══╝ ██╔══██║██║   ██║██╔══╝  ██║╚██╗██║██║ ██╔██╗ ",
                BRIGHT_RED + " ██║     ██║  ██║╚██████╔╝███████╗██║ ╚████║██║██╔╝ ██╗",
                BRIGHT_RED + " ╚═╝     ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═══╝╚═╝╚═╝  ╚═╝",
                BRIGHT_YELLOW + "             L   O   G   I   N       S   Y   S   T   E   M   ",
                "",
                BRIGHT_RED + " ╔════════════════════════════════════════════════════════════════════╗",
                BRIGHT_RED + " ║ " + BRIGHT_YELLOW + "⚠  ULTIMATE SERVER PROTECTION SYSTEM ACTIVATED  ⚠ " + BRIGHT_RED
                        + "                 ║",
                BRIGHT_RED + " ║                                                                    ║",
                BRIGHT_RED + " ║ " + BRIGHT_WHITE + "  ▸ Version: " + BRIGHT_GREEN + String.format("%-15s", version)
                        + BRIGHT_RED + "                                  ║",
                BRIGHT_RED + " ║ " + BRIGHT_WHITE + "  ▸ Author:  " + BRIGHT_PURPLE + String.format("%-15s", "TomLe")
                        + BRIGHT_RED + "                                  ║",
                BRIGHT_RED + " ║ " + BRIGHT_WHITE + "  ▸ Status:  " + BRIGHT_CYAN + "INITIALIZING..." + BRIGHT_RED
                        + "                                  ║",
                BRIGHT_RED + " ║                                                                    ║",
                BRIGHT_RED + " ╚════════════════════════════════════════════════════════════════════╝" + RESET,
                ""
        };

        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }

    /**
     * General info log
     */
    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX_FORMAT + BRIGHT_WHITE + message + RESET);
    }

    /**
     * Success log with checkmark
     */
    public static void success(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX_FORMAT + BRIGHT_GREEN + "✔ " + message + RESET);
    }

    /**
     * Warning log
     */
    public static void warn(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX_FORMAT + BRIGHT_YELLOW + "⚠ " + message + RESET);
    }

    /**
     * Error log
     */
    public static void error(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX_FORMAT + BRIGHT_RED + "✖ " + message + RESET);
    }

    /**
     * Debug log
     */
    public static void debug(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX_FORMAT + BRIGHT_PURPLE + "◆ [DEBUG] " + message + RESET);
    }

    /**
     * Shows a beautiful separator line
     */
    public static void separator() {
        Bukkit.getConsoleSender().sendMessage(
                BRIGHT_RED + " ══════════════════════════════════════════════════════════════════════" + RESET);
    }

    /**
     * Shows startup statistics (Dashboard style)
     */
    public static void showStartupStats(int players, String database, String language) {
        separator();
        Bukkit.getConsoleSender().sendMessage(BRIGHT_YELLOW + "   [ STATISTICS DASHBOARD ]");
        Bukkit.getConsoleSender().sendMessage("");

        info(BRIGHT_WHITE + "Database:      " + BRIGHT_CYAN + database.toUpperCase());
        info(BRIGHT_WHITE + "Language:      " + BRIGHT_CYAN + language.toUpperCase());
        info(BRIGHT_WHITE + "Total Users:   " + BRIGHT_GREEN + players);
        info(BRIGHT_WHITE + "Security:      " + BRIGHT_RED + BOLD + "MAXIMUM");
        separator();
    }

    /**
     * Shows loaded features list
     */
    public static void showFeatures() {
        Bukkit.getConsoleSender().sendMessage("");
        info(BRIGHT_YELLOW + "⚡ ENABLED MODULES:");
        success("BCrypt Encryption       " + BRIGHT_GREEN + "[ACTIVE]");
        success("Anti-Bot System         " + BRIGHT_GREEN + "[ACTIVE]");
        success("Void Authentication     " + BRIGHT_GREEN + "[ACTIVE]");
        success("Session Manager         " + BRIGHT_GREEN + "[ACTIVE]");
        success("Inventory Protection    " + BRIGHT_GREEN + "[ACTIVE]");
        Bukkit.getConsoleSender().sendMessage("");
    }

    /**
     * Message when loading is complete
     */
    public static void loaded(long loadTime) {
        separator();
        Bukkit.getConsoleSender().sendMessage(
                BRIGHT_RED + " ║ " + BRIGHT_GREEN + BOLD + "✔ PLUGIN SUCCESSFULLY ENABLED" + RESET
                        + BRIGHT_WHITE + " (" + loadTime + "ms)" + RESET);
        Bukkit.getConsoleSender().sendMessage(
                BRIGHT_RED + " ║ " + BRIGHT_YELLOW + "READY TO PROTECT YOUR SERVER." + RESET);
        separator();
        Bukkit.getConsoleSender().sendMessage("");
    }

    /**
     * Shutdown message
     */
    public static void shutdown() {
        Bukkit.getConsoleSender().sendMessage("");
        separator();
        Bukkit.getConsoleSender().sendMessage(
                BRIGHT_RED + " ║ " + BRIGHT_YELLOW + "⚠ " + BRIGHT_WHITE + "PhoenixLogin is shutting down..." + RESET);
        Bukkit.getConsoleSender().sendMessage(
                BRIGHT_RED + " ║ " + BRIGHT_WHITE + "   Good bye!" + RESET);
        separator();
        Bukkit.getConsoleSender().sendMessage("");
    }
}
