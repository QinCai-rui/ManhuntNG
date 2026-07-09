package xyz.qincai.manhunt.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
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
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    plugin.getConfigManager().getMessage("debug.only-in-game")));
            return true;
        }

        // subcommand: lastknown
        if (args.length == 0 || !args[0].equalsIgnoreCase("lastknown")) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    plugin.getConfigManager().getMessage("debug.usage")));
            return true;
        }

        TrackerManager tracker = plugin.getTrackerManager();
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                plugin.getConfigManager().getMessage("debug.header")));

        for (World.Environment env : World.Environment.values()) {

            Location loc = tracker.getLastRunnerLocation(env);
            String envName = env.toString();

            if (loc == null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        plugin.getConfigManager().getMessage("debug.no-data", "{dimension}", envName)));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        plugin.getConfigManager().getMessage("debug.location",
                                "{dimension}", envName,
                                "{x}", String.format("%.1f", loc.getX()),
                                "{y}", String.format("%.1f", loc.getY()),
                                "{z}", String.format("%.1f", loc.getZ()),
                                "{world}", loc.getWorld().getName())));
            }
        }

        return true;
    }
}
