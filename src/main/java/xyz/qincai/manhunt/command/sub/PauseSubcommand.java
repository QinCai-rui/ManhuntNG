package xyz.qincai.manhunt.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;
import xyz.qincai.manhunt.game.GameState;

public class PauseSubcommand implements Subcommand {
    @Override public String getName() { return "pause"; }
    @Override public String getPermission() { return null; }
    @Override public boolean requirePlayer() { return true; }
    @Override public boolean requireAdmin() { return false; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        Player player = (Player) sender;

        GameState state = plugin.getGameManager().getMatch().getState();
        if (state != GameState.RUNNING && state != GameState.PRE_HUNT && state != GameState.HEADSTART) {
            player.sendMessage(cfg(plugin).getMessageComponent("error.no-active-game-to-pause"));
            return true;
        }

        if (!plugin.getGameManager().getMatch().isOwner(player.getUniqueId()) && !player.hasPermission("manhunt.admin")) {
            player.sendMessage(cfg(plugin).getMessageComponent("error.only-owner-can-pause"));
            return true;
        }

        if (!plugin.getGameManager().pauseGame(player.getUniqueId())) {
            player.sendMessage(cfg(plugin).getMessageComponent("error.failed-to-pause"));
        }
        return true;
    }
}
