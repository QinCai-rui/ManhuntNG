package xyz.qincai.manhunt.command.sub;

import org.bukkit.command.CommandSender;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;
import xyz.qincai.manhunt.game.ManhuntGameMode;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.game.StartMode;

public class ModeSubcommand implements Subcommand {
    @Override public String getName() { return "mode"; }
    @Override public String getPermission() { return "manhunt.admin"; }
    @Override public boolean requirePlayer() { return false; }
    @Override public boolean requireAdmin() { return true; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.cannot-change-mode"));
            return true;
        }

        if (args.length == 1) {
            Match match = plugin.getGameManager().getMatch();
            ManhuntGameMode currentGameMode = match.getGameMode();
            StartMode currentStartMode = match.getStartMode();
            sender.sendMessage(cfg(plugin).getMessageComponent("mode.current-game", "{mode}", currentGameMode.getDisplayName(cfg(plugin))));
            sender.sendMessage(cfg(plugin).getMessageComponent("mode.current-start", "{mode}", currentStartMode.getDisplayName(cfg(plugin))));
            sender.sendMessage(cfg(plugin).getMessageComponent("usage.mode"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(cfg(plugin).getMessageComponent("usage.mode"));
            return true;
        }

        ManhuntGameMode gameMode;
        if (args[1].toLowerCase().equals("infection")) {
            gameMode = ManhuntGameMode.INFECTION;
        } else if (args[1].toLowerCase().equals("normal")) {
            gameMode = ManhuntGameMode.NORMAL;
        } else {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.invalid-game-mode"));
            return true;
        }

        StartMode startMode;
        if (args[2].toLowerCase().equals("headstart")) {
            startMode = StartMode.HEADSTART;
        } else if (args[2].toLowerCase().equals("dreamstart")) {
            startMode = StartMode.DREAMSTART;
        } else {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.invalid-start-mode"));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        match.setGameMode(gameMode);
        match.setStartMode(startMode);

        sender.sendMessage(cfg(plugin).getMessageComponent("mode.game-set", "{mode}", gameMode.getDisplayName(cfg(plugin))));
        sender.sendMessage(cfg(plugin).getMessageComponent("mode.start-set", "{mode}", startMode.getDisplayName(cfg(plugin))));

        if (gameMode == ManhuntGameMode.INFECTION) {
            sender.sendMessage(cfg(plugin).getMessageComponent("mode.infection-info"));
        }
        return true;
    }

    @Override
    public java.util.List<String> tabComplete(CommandSender sender, ManhuntNG plugin, String[] args) {
        java.util.List<String> completions = new java.util.ArrayList<>();
        if (args.length == 2) {
            completions.add("normal");
            completions.add("infection");
        } else if (args.length == 3) {
            String gameMode = args[1].toLowerCase();
            if (gameMode.equals("normal") || gameMode.equals("infection")) {
                completions.add("dreamstart");
                completions.add("headstart");
            }
        }
        return completions;
    }
}
