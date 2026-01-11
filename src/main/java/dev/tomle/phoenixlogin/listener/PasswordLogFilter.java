package dev.tomle.phoenixlogin.listener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.util.regex.Pattern;

/**
 * Filtro que oculta las contraseñas de los comandos de autenticación en la
 * consola.
 * Previene que /login, /register, /changepassword, /unregister muestren las
 * contraseñas en logs.
 */
public class PasswordLogFilter extends AbstractFilter {

    // Patrones para detectar comandos con contraseña
    private static final Pattern LOGIN_PATTERN = Pattern.compile(
            ".*issued server command: /(login|l|loguear)\\s+.+", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGISTER_PATTERN = Pattern.compile(
            ".*issued server command: /(register|reg|registrar)\\s+.+", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHANGEPASS_PATTERN = Pattern.compile(
            ".*issued server command: /changepassword\\s+.+", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNREGISTER_PATTERN = Pattern.compile(
            ".*issued server command: /unregister\\s+.+", Pattern.CASE_INSENSITIVE);

    /**
     * Registra el filtro en el logger del servidor
     */
    public static void register() {
        try {
            Logger rootLogger = (Logger) LogManager.getRootLogger();
            rootLogger.addFilter(new PasswordLogFilter());
        } catch (Exception e) {
            // Si falla, no pasa nada crítico - solo no se filtrará
        }
    }

    @Override
    public Result filter(LogEvent event) {
        if (event == null || event.getMessage() == null) {
            return Result.NEUTRAL;
        }

        String message = event.getMessage().getFormattedMessage();
        if (message == null) {
            return Result.NEUTRAL;
        }

        // Si el mensaje contiene un comando de autenticación con contraseña, denegarlo
        if (containsPasswordCommand(message)) {
            return Result.DENY;
        }

        return Result.NEUTRAL;
    }

    private boolean containsPasswordCommand(String message) {
        return LOGIN_PATTERN.matcher(message).matches() ||
                REGISTER_PATTERN.matcher(message).matches() ||
                CHANGEPASS_PATTERN.matcher(message).matches() ||
                UNREGISTER_PATTERN.matcher(message).matches();
    }
}
