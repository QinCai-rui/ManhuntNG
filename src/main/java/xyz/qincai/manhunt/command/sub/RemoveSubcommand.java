package xyz.qincai.manhunt.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.player.PlayerRole;

public class RemoveSubcommand implements Subcommand {
    @Override public String getName() { return "remove"; }
    @Override public String getPermission() { return "manhunt.admin"; }
    @Override public boolean requirePlayer() { return false; }
    @Override public boolean requireAdmin() { return true; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(cfg(plugin).getMessageComponent("usage.remove"));
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

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.cannot-change-roles"));
            return true;
        }

        plugin.getPlayerManager().applyRoleToPlayer(target, PlayerRole.SPECTATOR);
        sender.sendMessage(cfg(plugin).getMessageComponent("role.removed-sender", "{player}", target.getName()));
        target.sendMessage(cfg(plugin).getMessageComponent("role.removed-target"));
        return true;
    }
}
