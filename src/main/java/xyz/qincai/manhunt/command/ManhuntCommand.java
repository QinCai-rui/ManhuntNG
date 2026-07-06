package xyz.qincai.manhunt.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("manhunt.play")) {
            player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            player.sendMessage(Component.text("A game is already in progress!", NamedTextColor.RED));
            return true;
        }

        plugin.getPlayerManager().setRole(player.getUniqueId(), PlayerRole.SPECTATOR);
        plugin.getGameManager().getMatch().addSpectator(player.getUniqueId());
        player.sendMessage(Component.text("You joined the manhunt lobby!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleLeave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            player.sendMessage(Component.text("Cannot leave during an active game!", NamedTextColor.RED));
            return true;
        }

        plugin.getPlayerManager().removePlayerFromGame(player.getUniqueId());
        player.sendMessage(Component.text("You left the manhunt lobby.", NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        plugin.getGameManager().startGame();
        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        plugin.getGameManager().stopGame();
        sender.sendMessage(Component.text("Game stopped.", NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        plugin.getConfigManager().reloadConfigs();
        sender.sendMessage(Component.text("Configuration reloaded!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleRunner(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                    .append(Component.text("/manhunt runner <player>", NamedTextColor.WHITE)));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(Component.text("Cannot change roles during an active game!", NamedTextColor.RED));
            return true;
        }

        plugin.getPlayerManager().removePlayerFromGame(target.getUniqueId());
        plugin.getPlayerManager().setRole(target.getUniqueId(), PlayerRole.RUNNER);
        plugin.getGameManager().getMatch().setRunnerUuid(target.getUniqueId());
        sender.sendMessage(Component.text(target.getName(), NamedTextColor.AQUA)
                .append(Component.text(" is now the Runner!", NamedTextColor.GREEN)));
        target.sendMessage(Component.text("You are now the Runner!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleHunter(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                    .append(Component.text("/manhunt hunter <player>", NamedTextColor.WHITE)));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(Component.text("Cannot change roles during an active game!", NamedTextColor.RED));
            return true;
        }

        plugin.getPlayerManager().removePlayerFromGame(target.getUniqueId());
        plugin.getPlayerManager().setRole(target.getUniqueId(), PlayerRole.HUNTER);
        plugin.getGameManager().getMatch().addHunter(target.getUniqueId());
        sender.sendMessage(Component.text(target.getName(), NamedTextColor.AQUA)
                .append(Component.text(" is now a Hunter!", NamedTextColor.GREEN)));
        target.sendMessage(Component.text("You are now a Hunter!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleForceStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getGameManager().getMatch().getState() != GameState.WAITING) {
            sender.sendMessage(Component.text("Game is not in waiting state!", NamedTextColor.RED));
            return true;
        }

        plugin.getGameManager().startGame();
        return true;
    }

    private void sendHelp(CommandSender sender) {
        GameState state = plugin.getGameManager().getMatch().getState();
        String stateName = switch (state) {
            case WAITING -> "Waiting";
            case COUNTDOWN -> "Countdown";
            case PRE_HUNT -> "Pre-Hunt";
            case RUNNING -> "Running";
            case FINISHED -> "Finished";
        };

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("ManhuntNG", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(" \u2014 ", NamedTextColor.DARK_GRAY))
                .append(Component.text(stateName, getTextColor(state), TextDecoration.BOLD)));
        sender.sendMessage(Component.text("  \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500", NamedTextColor.DARK_GRAY));

        Component playerHeader = Component.text("  Player", NamedTextColor.YELLOW, TextDecoration.BOLD);
        sender.sendMessage(playerHeader);

        helpEntry(sender, "/manhunt join", "Join the lobby",
                "Click to join", "manhunt.play");
        helpEntry(sender, "/manhunt leave", "Leave the lobby",
                "Click to leave", "manhunt.play");

        if (sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("  Admin", NamedTextColor.RED, TextDecoration.BOLD));

            helpEntry(sender, "/manhunt start", "Start the match",
                    "Click to start", "manhunt.admin");
            helpEntry(sender, "/manhunt stop", "Force stop the match",
                    "Click to stop", "manhunt.admin");
            helpEntry(sender, "/manhunt runner <player>", "Set the Runner",
                    "Click to set runner", "manhunt.admin");
            helpEntry(sender, "/manhunt hunter <player>", "Add a Hunter",
                    "Click to set hunter", "manhunt.admin");
            helpEntry(sender, "/manhunt forcestart", "Skip validation & start",
                    "Click to force start", "manhunt.admin");
            helpEntry(sender, "/manhunt reload", "Reload configuration",
                    "Click to reload", "manhunt.admin");
        }

        sender.sendMessage(Component.empty());
    }

    private void helpEntry(CommandSender sender, String command, String description,
                           String hoverText, String permission) {
        Component cmd = Component.text("  /" + command, NamedTextColor.AQUA)
                .hoverEvent(HoverEvent.showText(
                        Component.text(hoverText + "\n", NamedTextColor.GRAY)
                                .append(Component.text("Permission: ", NamedTextColor.DARK_GRAY))
                                .append(Component.text(permission, NamedTextColor.YELLOW))
                ))
                .clickEvent(ClickEvent.runCommand(command));
        Component desc = Component.text(" \u2014 ", NamedTextColor.DARK_GRAY)
                .append(Component.text(description, NamedTextColor.GRAY));
        sender.sendMessage(cmd.append(desc));
    }

    private NamedTextColor getTextColor(GameState state) {
        return switch (state) {
            case WAITING -> NamedTextColor.GREEN;
            case COUNTDOWN -> NamedTextColor.YELLOW;
            case PRE_HUNT -> NamedTextColor.GOLD;
            case RUNNING -> NamedTextColor.RED;
            case FINISHED -> NamedTextColor.DARK_GRAY;
        };
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
