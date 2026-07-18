package xyz.qincai.manhunt.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;
import xyz.qincai.manhunt.game.Match;

public class LeaveSubcommand implements Subcommand {
    @Override public String getName() { return "leave"; }
    @Override public String getPermission() { return "manhunt.play"; }
    @Override public boolean requirePlayer() { return true; }
    @Override public boolean requireAdmin() { return false; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        Player player = (Player) sender;

        Match match = plugin.getGameManager().getMatch();

        if (!match.isParticipant(player.getUniqueId())) {
            player.sendMessage(cfg(plugin).getMessageComponent("error.not-in-game", "{player}", player.getName()));
            return true;
        }

        plugin.getPlayerManager().removePlayerFromGame(player.getUniqueId());

        if (plugin.getGameManager().isGameActive()) {
            plugin.getWorldManager().teleportToLobby(player);
            plugin.getUiManager().sendToAll(cfg(plugin).getMessage("leave.broadcast", "{player}", player.getName()));
        }

        player.sendMessage(cfg(plugin).getMessageComponent("leave.lobby"));
        return true;
    }
}
