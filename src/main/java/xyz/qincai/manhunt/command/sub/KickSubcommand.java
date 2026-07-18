package xyz.qincai.manhunt.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;
import xyz.qincai.manhunt.game.Match;

public class KickSubcommand implements Subcommand {
    @Override public String getName() { return "kick"; }
    @Override public String getPermission() { return "manhunt.admin"; }
    @Override public boolean requirePlayer() { return false; }
    @Override public boolean requireAdmin() { return true; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(cfg(plugin).getMessageComponent("usage.kick"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.player-not-found"));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        if (!match.isParticipant(target.getUniqueId())) {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.not-in-game", "{player}", target.getName()));
            return true;
        }

        plugin.getPlayerManager().removePlayerFromGame(target.getUniqueId());

        if (plugin.getGameManager().isGameActive()) {
            org.bukkit.World mainWorld = org.bukkit.Bukkit.getWorlds().get(0);
            target.teleport(mainWorld.getSpawnLocation());
            plugin.getUiManager().sendToAll(cfg(plugin).getMessage("admin.kick-broadcast", "{player}", target.getName()));
        }

        sender.sendMessage(cfg(plugin).getMessageComponent("admin.kick-sender", "{player}", target.getName()));
        target.sendMessage(cfg(plugin).getMessageComponent("admin.kick-target"));
        return true;
    }
}
