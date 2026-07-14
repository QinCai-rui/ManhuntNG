package xyz.qincai.manhunt.command.sub;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.chat.ChatMode;
import xyz.qincai.manhunt.command.Subcommand;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.ArrayList;
import java.util.List;

public class ChatSubcommand implements Subcommand {
    @Override public String getName() { return "chat"; }
    @Override public String getPermission() { return "manhunt.play"; }
    @Override public boolean requirePlayer() { return true; }
    @Override public boolean requireAdmin() { return false; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        Player player = (Player) sender;

        if (args.length < 2) {
            ChatMode mode = plugin.getChatManager().getChatMode(player.getUniqueId());
            player.sendMessage(cfg(plugin).getMessageComponent("chat.mode-show")
                    .append(Component.text(mode.name(),
                            mode == ChatMode.GLOBAL ? NamedTextColor.GOLD : NamedTextColor.GREEN)));
            player.sendMessage(cfg(plugin).getMessageComponent("usage.chat-mode"));
            return true;
        }

        PlayerRole role = plugin.getPlayerManager().getRole(player.getUniqueId());
        if (role == PlayerRole.SPECTATOR) {
            player.sendMessage(cfg(plugin).getMessageComponent("error.spectator-global-only"));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "global", "g" -> {
                plugin.getChatManager().setChatMode(player.getUniqueId(), ChatMode.GLOBAL);
                player.sendMessage(cfg(plugin).getMessageComponent("chat.mode-set-global"));
            }
            case "team", "t" -> {
                if (plugin.getChatManager().isTeamSinglePlayer(player.getUniqueId())) {
                    plugin.getChatManager().setChatMode(player.getUniqueId(), ChatMode.GLOBAL);
                    player.sendMessage(cfg(plugin).getMessageComponent("chat.mode-set-global"));
                    player.sendMessage(cfg(plugin).getMessageComponent("error.cannot-team-single"));
                } else {
                    plugin.getChatManager().setChatMode(player.getUniqueId(), ChatMode.TEAM);
                    player.sendMessage(cfg(plugin).getMessageComponent("chat.mode-set-team"));
                }
            }
            default -> player.sendMessage(cfg(plugin).getMessageComponent("usage.chat-mode"));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, ManhuntNG plugin, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.add("global");
            completions.add("team");
        }
        return completions;
    }
}
