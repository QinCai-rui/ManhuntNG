package xyz.qincai.manhunt.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OwnerSubcommand implements Subcommand {
    @Override public String getName() { return "owner"; }
    @Override public String getPermission() { return "manhunt.admin"; }
    @Override public boolean requirePlayer() { return false; }
    @Override public boolean requireAdmin() { return true; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        if (args.length < 2) {
            UUID ownerUuid = plugin.getGameManager().getMatch().getOwnerUuid();
            if (ownerUuid != null) {
                Player owner = Bukkit.getPlayer(ownerUuid);
                String ownerName = owner != null ? owner.getName() : "Unknown";
                sender.sendMessage(cfg(plugin).getMessageComponent("admin.owner-show", "{player}", ownerName));
            } else {
                sender.sendMessage(cfg(plugin).getMessageComponent("admin.owner-none"));
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.player-not-found"));
            return true;
        }

        plugin.getGameManager().getMatch().setOwnerUuid(target.getUniqueId());
        sender.sendMessage(cfg(plugin).getMessageComponent("admin.owner-set-sender", "{player}", target.getName()));
        target.sendMessage(cfg(plugin).getMessageComponent("admin.owner-set-target"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, ManhuntNG plugin, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        return completions;
    }
}
