package xyz.qincai.manhunt.command.sub;

import org.bukkit.command.CommandSender;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;

public class SeedSubcommand implements Subcommand {
    @Override public String getName() { return "seed"; }
    @Override public String getPermission() { return "manhunt.admin"; }
    @Override public boolean requirePlayer() { return false; }
    @Override public boolean requireAdmin() { return true; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.cannot-change-seed"));
            return true;
        }

        if (args.length < 2) {
            Long currentSeed = plugin.getGameManager().getMatch().getSeed();
            if (currentSeed != null) {
                sender.sendMessage(cfg(plugin).getMessageComponent("admin.seed-show", "{seed}", String.valueOf(currentSeed)));
            } else {
                sender.sendMessage(cfg(plugin).getMessageComponent("admin.seed-none"));
            }
            return true;
        }

        String seedArg = args[1];
        try {
            long seed;
            if (seedArg.matches("-?\\d+")) {
                seed = Long.parseLong(seedArg);
            } else {
                seed = seedArg.hashCode();
            }

            plugin.getGameManager().getMatch().setSeed(seed);
            sender.sendMessage(cfg(plugin).getMessageComponent("admin.seed-set",
                    "{seed}", String.valueOf(seed),
                    "{input}", seedArg));
        } catch (NumberFormatException e) {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.invalid-seed"));
        }
        return true;
    }
}
