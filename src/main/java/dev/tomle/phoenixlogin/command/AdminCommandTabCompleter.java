package dev.tomle.phoenixlogin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TabCompleter para el comando /phoenixlogin
 */
public class AdminCommandTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "reload",
            "info",
            "unregister",
            "stats",
            "help");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("phoenixlogin.admin")) {
            return completions;
        }

        if (args.length == 1) {
            // Sugerir subcomandos
            String partial = args[0].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            // Para comandos que requieren nombre de jugador
            if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("unregister")) {
                // Sugerir jugadores online
                String partial = args[1].toLowerCase();
                sender.getServer().getOnlinePlayers().forEach(player -> {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                });
            }
        }

        return completions;
    }
}
