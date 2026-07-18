package xyz.qincai.manhunt.command.sub;

import org.bukkit.command.CommandSender;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;

public class StopSubcommand implements Subcommand {
    @Override public String getName() { return "stop"; }
    @Override public String getPermission() { return "manhunt.admin"; }
    @Override public boolean requirePlayer() { return false; }
    @Override public boolean requireAdmin() { return true; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        plugin.getGameManager().stopGame();
        sender.sendMessage(cfg(plugin).getMessageComponent("admin.game-stopped"));
        return true;
    }
}
