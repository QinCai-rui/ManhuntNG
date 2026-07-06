package xyz.qincai.manhunt.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.ArrayList;
import java.util.List;

public class ManhuntCommand implements CommandExecutor, TabCompleter {
    private final ManhuntNG plugin;

    public ManhuntCommand(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "join" -> handleJoin(sender, args);
            case "leave" -> handleLeave(sender, args);
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender, args);
            case "reload" -> handleReload(sender, args);
            case "runner" -> handleRunner(sender, args);
            case "hunter" -> handleHunter(sender, args);
            case "forcestart" -> handleForceStart(sender, args);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return true;
        }

        if (!player.hasPermission("manhunt.play")) {
            player.sendMessage("\u00a7cYou don't have permission!");
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            player.sendMessage("\u00a7cA game is already in progress!");
            return true;
        }

        plugin.getPlayerManager().setRole(player.getUniqueId(), PlayerRole.SPECTATOR);
        plugin.getGameManager().getMatch().addSpectator(player.getUniqueId());
        player.sendMessage("\u00a7aYou joined the manhunt lobby!");
        return true;
    }

    private boolean handleLeave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            player.sendMessage("\u00a7cCannot leave during an active game!");
            return true;
        }

        plugin.getPlayerManager().removePlayerFromGame(player.getUniqueId());
        player.sendMessage("\u00a7eYou left the manhunt lobby.");
        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage("\u00a7cYou don't have permission!");
            return true;
        }

        plugin.getGameManager().startGame();
        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage("\u00a7cYou don't have permission!");
            return true;
        }

        plugin.getGameManager().stopGame();
        sender.sendMessage("\u00a7eGame stopped.");
        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage("\u00a7cYou don't have permission!");
            return true;
        }

        plugin.getConfigManager().reloadConfigs();
        sender.sendMessage("\u00a7aConfiguration reloaded!");
        return true;
    }

    private boolean handleRunner(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage("\u00a7cYou don't have permission!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /manhunt runner <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("\u00a7cPlayer not found!");
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage("\u00a7cCannot change roles during an active game!");
            return true;
        }

        plugin.getPlayerManager().removePlayerFromGame(target.getUniqueId());
        plugin.getPlayerManager().setRole(target.getUniqueId(), PlayerRole.RUNNER);
        plugin.getGameManager().getMatch().setRunnerUuid(target.getUniqueId());
        sender.sendMessage("\u00a7a" + target.getName() + " is now the Runner!");
        target.sendMessage("\u00a7aYou are now the Runner!");
        return true;
    }

    private boolean handleHunter(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage("\u00a7cYou don't have permission!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /manhunt hunter <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("\u00a7cPlayer not found!");
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage("\u00a7cCannot change roles during an active game!");
            return true;
        }

        plugin.getPlayerManager().removePlayerFromGame(target.getUniqueId());
        plugin.getPlayerManager().setRole(target.getUniqueId(), PlayerRole.HUNTER);
        plugin.getGameManager().getMatch().addHunter(target.getUniqueId());
        sender.sendMessage("\u00a7a" + target.getName() + " is now a Hunter!");
        target.sendMessage("\u00a7aYou are now a Hunter!");
        return true;
    }

    private boolean handleForceStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage("\u00a7cYou don't have permission!");
            return true;
        }

        if (plugin.getGameManager().getMatch().getState() != GameState.WAITING) {
            sender.sendMessage("\u00a7cGame is not in waiting state!");
            return true;
        }

        plugin.getGameManager().startGame();
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("\u00a76\u00a7l=== ManhuntNG ===");
        sender.sendMessage("\u00a7e/manhunt join \u00a77- Join the lobby");
        sender.sendMessage("\u00a7e/manhunt leave \u00a77- Leave the lobby");
        if (sender.hasPermission("manhunt.admin")) {
            sender.sendMessage("\u00a7e/manhunt start \u00a77- Start the game");
            sender.sendMessage("\u00a7e/manhunt stop \u00a77- Stop the game");
            sender.sendMessage("\u00a7e/manhunt reload \u00a77- Reload config");
            sender.sendMessage("\u00a7e/manhunt runner <player> \u00a77- Set runner");
            sender.sendMessage("\u00a7e/manhunt hunter <player> \u00a77- Set hunter");
            sender.sendMessage("\u00a7e/manhunt forcestart \u00a77- Force start");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("join");
            completions.add("leave");
            if (sender.hasPermission("manhunt.admin")) {
                completions.add("start");
                completions.add("stop");
                completions.add("reload");
                completions.add("runner");
                completions.add("hunter");
                completions.add("forcestart");
            }
            return filterPartial(args[0], completions);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("runner") || sub.equals("hunter")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
                return filterPartial(args[1], completions);
            }
        }

        return completions;
    }

    private List<String> filterPartial(String partial, List<String> options) {
        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(partial.toLowerCase())) {
                filtered.add(option);
            }
        }
        return filtered;
    }
}
