package xyz.qincai.manhunt.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.player.PlayerRole;

public class JoinSubcommand implements Subcommand {
    @Override public String getName() { return "join"; }
    @Override public String getPermission() { return "manhunt.play"; }
    @Override public boolean requirePlayer() { return true; }
    @Override public boolean requireAdmin() { return false; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        Player player = (Player) sender;

        Match match = plugin.getGameManager().getMatch();

        if (match.isParticipant(player.getUniqueId())) {
            player.sendMessage(cfg(plugin).getMessageComponent("error.already-joined"));
            return true;
        }

        plugin.getPlayerManager().setRole(player.getUniqueId(), PlayerRole.SPECTATOR);
        match.addSpectator(player.getUniqueId());

        if (plugin.getGameManager().isGameActive()) {
            org.bukkit.World gameWorld = match.getGameWorld();
            if (gameWorld != null) {
                player.teleport(gameWorld.getSpawnLocation());
            }
            plugin.getUiManager().sendToAll(cfg(plugin).getMessage("join.broadcast", "{player}", player.getName()));
            player.sendMessage(cfg(plugin).getMessageComponent("join.spectator"));
        } else {
            plugin.getWorldManager().teleportToLobby(player);
            player.sendMessage(cfg(plugin).getMessageComponent("join.lobby"));
        }
        return true;
    }
}
