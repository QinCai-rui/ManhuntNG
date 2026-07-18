package xyz.qincai.manhunt.command;

import org.bukkit.command.CommandSender;
import xyz.qincai.manhunt.config.ConfigManager;
import xyz.qincai.manhunt.ManhuntNG;

import java.util.ArrayList;
import java.util.List;

public interface Subcommand {
    String getName();
    String getPermission();
    boolean requirePlayer();
    boolean requireAdmin();

    default boolean validatePreconditions(CommandSender sender, ManhuntNG plugin, String[] args) {
        ConfigManager cfg = plugin.getConfigManager();

        if (requirePlayer() && !(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(cfg.getMessageComponent("error.only-players"));
            return false;
        }

        if (requireAdmin() && !sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg.getMessageComponent("error.no-permission"));
            return false;
        }

        if (getPermission() != null && !sender.hasPermission(getPermission())) {
            sender.sendMessage(cfg.getMessageComponent("error.no-permission"));
            return false;
        }

        return true;
    }

    boolean execute(CommandSender sender, ManhuntNG plugin, String[] args);

    default List<String> tabComplete(CommandSender sender, ManhuntNG plugin, String[] args) {
        return new ArrayList<>();
    }

    default ConfigManager cfg(ManhuntNG plugin) {
        return plugin.getConfigManager();
    }
}
