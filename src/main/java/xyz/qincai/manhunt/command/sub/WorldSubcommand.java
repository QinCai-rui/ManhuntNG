package xyz.qincai.manhunt.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;

import java.util.ArrayList;
import java.util.List;

public class WorldSubcommand implements Subcommand {
    @Override public String getName() { return "world"; }
    @Override public String getPermission() { return "manhunt.admin"; }
    @Override public boolean requirePlayer() { return false; }
    @Override public boolean requireAdmin() { return true; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.cannot-change-world"));
            return true;
        }

        if (args.length < 2) {
            String currentWorld = plugin.getGameManager().getMatch().getWorldName();
            if (currentWorld != null) {
                sender.sendMessage(cfg(plugin).getMessageComponent("admin.world-show", "{world}", currentWorld));
            } else {
                sender.sendMessage(cfg(plugin).getMessageComponent("admin.world-none"));
            }
            return true;
        }

        String worldName = args[1];

        if (worldName.equalsIgnoreCase("clear") || worldName.equalsIgnoreCase("reset")) {
            plugin.getGameManager().getMatch().setWorldName(null);
            sender.sendMessage(cfg(plugin).getMessageComponent("admin.world-cleared"));
            return true;
        }

        plugin.getGameManager().getMatch().setWorldName(worldName);
        sender.sendMessage(cfg(plugin).getMessageComponent("admin.world-set", "{world}", worldName));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, ManhuntNG plugin, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            for (World world : Bukkit.getWorlds()) {
                completions.add(world.getName());
            }
            completions.add("clear");
        }
        return completions;
    }
}
