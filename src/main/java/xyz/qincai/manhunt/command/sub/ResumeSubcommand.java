package xyz.qincai.manhunt.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;
import xyz.qincai.manhunt.game.GameState;

public class ResumeSubcommand implements Subcommand {
    @Override public String getName() { return "resume"; }
    @Override public String getPermission() { return null; }
    @Override public boolean requirePlayer() { return true; }
    @Override public boolean requireAdmin() { return false; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        Player player = (Player) sender;

        if (plugin.getGameManager().getMatch().getState() != GameState.PAUSED) {
            player.sendMessage(cfg(plugin).getMessageComponent("error.no-paused-game"));
            return true;
        }

        if (!plugin.getGameManager().getMatch().isOwner(player.getUniqueId()) && !player.hasPermission("manhunt.admin")) {
            player.sendMessage(cfg(plugin).getMessageComponent("error.only-owner-can-resume"));
            return true;
        }

        if (!plugin.getGameManager().resumeGame(player.getUniqueId())) {
            player.sendMessage(cfg(plugin).getMessageComponent("error.failed-to-resume"));
        }
        return true;
    }
}
