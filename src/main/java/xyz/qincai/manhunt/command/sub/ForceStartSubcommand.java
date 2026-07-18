package xyz.qincai.manhunt.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;

import java.util.UUID;

public class ForceStartSubcommand implements Subcommand {
    @Override public String getName() { return "forcestart"; }
    @Override public String getPermission() { return "manhunt.admin"; }
    @Override public boolean requirePlayer() { return false; }
    @Override public boolean requireAdmin() { return true; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        UUID ownerUuid = sender instanceof Player player ? player.getUniqueId() : null;
        boolean success = plugin.getGameManager().startGameForce(ownerUuid);
        if (success) {
            sender.sendMessage(cfg(plugin).getMessageComponent("admin.force-started"));
        }
        return true;
    }
}
