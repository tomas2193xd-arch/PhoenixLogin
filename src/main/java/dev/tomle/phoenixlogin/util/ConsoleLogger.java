package dev.tomle.phoenixlogin.util;

import java.util.logging.Logger;

/**
 * Minimal, professional console logging using Java Logger (no System.out/err).
 */
public class ConsoleLogger {

    private static final String PREFIX = "[PhoenixLogin] ";
    private static Logger logger;

    public static void setLogger(Logger pluginLogger) {
        logger = pluginLogger;
    }

    private static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("PhoenixLogin");
        }
        return logger;
    }

    public static void showBanner(String version) {
        getLogger().info("PhoenixLogin v" + version + " by Tomas2193");
    }

    public static void info(String message) {
        getLogger().info(message);
    }

    public static void success(String message) {
        getLogger().info(message);
    }

    public static void warn(String message) {
        getLogger().warning(message);
    }

    public static void error(String message) {
        getLogger().severe(message);
    }

    public static void debug(String message) {
        getLogger().fine("[DEBUG] " + message);
    }

    public static void separator() {
        // no-op
    }

    public static void showStartupStats(int players, String database) {
        getLogger().info("Stats: " + players + " users | DB: " + database);
    }

    public static void showFeatures() {
        // no-op
    }

    public static void loaded(long loadTime) {
        getLogger().info("Ready (" + loadTime + "ms)");
    }

    public static void shutdown() {
        getLogger().info("Disabled.");
    }
}
