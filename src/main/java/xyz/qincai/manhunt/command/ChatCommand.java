package xyz.qincai.manhunt.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;

public class ChatCommand implements CommandExecutor {
    private final ManhuntNG plugin;
    private final boolean global;

    public ChatCommand(ManhuntNG plugin, boolean global) {
        this.plugin = plugin;
        this.global = global;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessageComponent("error.only-players"));
            return true;
        }

        if (!player.hasPermission("manhunt.play")) {
            player.sendMessage(plugin.getConfigManager().getMessageComponent("error.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getConfigManager().getMessageComponent("usage.chat-command",
                    "{label}", label));
            return true;
        }

        if (!plugin.getGameManager().isGameActive()) {
            player.sendMessage(plugin.getConfigManager().getMessageComponent("error.no-active-game"));
            return true;
        }

        String message = String.join(" ", args);
        if (message.trim().isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessageComponent("error.message-empty"));
            return true;
        }

        if (global) {
            plugin.getChatManager().sendGlobalMessage(player, message);
        } else {
            plugin.getChatManager().sendTeamMessage(player, message);
        }

        return true;
    }
}
