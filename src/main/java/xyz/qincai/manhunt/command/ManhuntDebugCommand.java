package xyz.qincai.manhunt.command;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.tracker.TrackerManager;

public class ManhuntDebugCommand implements CommandExecutor {

    private final ManhuntNG plugin;

    public ManhuntDebugCommand(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Only players can use this debug command
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        // subcommand: lastknown
        if (args.length == 0 || !args[0].equalsIgnoreCase("lastknown")) {
            player.sendMessage("§eUsage: §f/manhunt debug lastknown");
            return true;
        }

        TrackerManager tracker = plugin.getTrackerManager();
        player.sendMessage("§6§lLast Known Runner Locations:");

        for (World.Environment env : World.Environment.values()) {

            Location loc = tracker.getLastRunnerLocation(env);
            String envName = env.toString();

            if (loc == null) {
                player.sendMessage("§c" + envName + ": No data yet (runner has not entered this dimension)");
            } else {
                player.sendMessage(String.format(
                        "§a%s: §f(%.1f, %.1f, %.1f) in world '%s'",
                        envName,
                        loc.getX(), loc.getY(), loc.getZ(),
                        loc.getWorld().getName()
                ));
            }
        }

        return true;
    }
}
