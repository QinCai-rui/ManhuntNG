package xyz.qincai.manhunt.command.sub;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;
import xyz.qincai.manhunt.game.Match;

public class WorldInfoSubcommand implements Subcommand {
    @Override public String getName() { return "world"; }
    @Override public String getPermission() { return "manhunt.play"; }
    @Override public boolean requirePlayer() { return true; }
    @Override public boolean requireAdmin() { return false; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        Player player = (Player) sender;
        World world = player.getWorld();
        String worldName = world.getName();
        String dimension = switch (world.getEnvironment()) {
            case NORMAL -> "Overworld";
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> "Unknown";
        };

        Match match = plugin.getGameManager().getMatch();
        if (plugin.getGameManager().isGameActive()) {
            if (match.getGameWorld() != null && world.equals(match.getGameWorld())) {
                sender.sendMessage(cfg(plugin).getMessageComponent("admin.world-info-game",
                        "{world}", worldName, "{dimension}", dimension));
            } else if (match.getNetherWorld() != null && world.equals(match.getNetherWorld())) {
                sender.sendMessage(cfg(plugin).getMessageComponent("admin.world-info-game",
                        "{world}", worldName, "{dimension}", dimension));
            } else if (match.getEndWorld() != null && world.equals(match.getEndWorld())) {
                sender.sendMessage(cfg(plugin).getMessageComponent("admin.world-info-game",
                        "{world}", worldName, "{dimension}", dimension));
            } else {
                sender.sendMessage(cfg(plugin).getMessageComponent("admin.world-info-lobby",
                        "{world}", worldName, "{dimension}", dimension));
            }
        } else {
            sender.sendMessage(cfg(plugin).getMessageComponent("admin.world-info-lobby",
                    "{world}", worldName, "{dimension}", dimension));
        }
        return true;
    }
}
