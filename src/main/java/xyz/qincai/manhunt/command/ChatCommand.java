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
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("manhunt.play")) {
            player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /" + label + " <message>", NamedTextColor.RED));
            return true;
        }

        if (!plugin.getGameManager().isGameActive()) {
            player.sendMessage(Component.text("No active game!", NamedTextColor.RED));
            return true;
        }

        String message = String.join(" ", args);
        if (message.trim().isEmpty()) {
            player.sendMessage(Component.text("Message cannot be empty!", NamedTextColor.RED));
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
