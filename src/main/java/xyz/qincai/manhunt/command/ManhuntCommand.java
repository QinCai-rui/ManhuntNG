package xyz.qincai.manhunt.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.chat.ChatMode;
import xyz.qincai.manhunt.game.ManhuntGameMode;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.game.StartMode;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
            case "remove" -> handleRemove(sender, args);
            case "kick" -> handleKick(sender, args);
            case "forcestart" -> handleForceStart(sender, args);
            case "mode" -> handleMode(sender, args);
            case "pause" -> handlePause(sender, args);
            case "resume" -> handleResume(sender, args);
            case "owner" -> handleOwner(sender, args);
            case "seed" -> handleSeed(sender, args);
            case "world" -> handleWorld(sender, args);
            case "chat" -> handleChat(sender, args);
            case "debug" -> new ManhuntDebugCommand(plugin).onCommand(sender, null, null, args);
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

        Match match = plugin.getGameManager().getMatch();

        if (match.isParticipant(player.getUniqueId())) {
            player.sendMessage(Component.text("You have already joined!", NamedTextColor.RED));
            return true;
        }

        plugin.getPlayerManager().setRole(player.getUniqueId(), PlayerRole.SPECTATOR);
        match.addSpectator(player.getUniqueId());

        if (plugin.getGameManager().isGameActive()) {
            org.bukkit.World gameWorld = match.getGameWorld();
            if (gameWorld != null) {
                player.teleport(gameWorld.getSpawnLocation());
            }
            plugin.getUiManager().sendToAll("<green>" + player.getName() + " has joined the game!");
            player.sendMessage(Component.text("You joined as a spectator!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("You joined the manhunt lobby!", NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean handleLeave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        if (!match.isParticipant(player.getUniqueId())) {
            player.sendMessage(Component.text("You are not in the game!", NamedTextColor.RED));
            return true;
        }

        plugin.getPlayerManager().removePlayerFromGame(player.getUniqueId());

        if (plugin.getGameManager().isGameActive()) {
            org.bukkit.World mainWorld = org.bukkit.Bukkit.getWorlds().get(0);
            player.teleport(mainWorld.getSpawnLocation());
            plugin.getUiManager().sendToAll("<yellow>" + player.getName() + " has left the game!");
        }

        player.sendMessage(Component.text("You left the manhunt lobby.", NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        UUID ownerUuid = sender instanceof Player player ? player.getUniqueId() : null;
        plugin.getGameManager().startGame(ownerUuid);
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

        Match match = plugin.getGameManager().getMatch();

        if (!match.isParticipant(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " has not joined the game!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(Component.text("Cannot change roles during an active game!", NamedTextColor.RED));
            return true;
        }

        plugin.getPlayerManager().applyRoleToPlayer(target, PlayerRole.RUNNER);
        sender.sendMessage(Component.text(target.getName(), NamedTextColor.AQUA)
                .append(Component.text(" is now a Runner!", NamedTextColor.GREEN)));
        target.sendMessage(Component.text("You are now a Runner!", NamedTextColor.GREEN));
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

        Match match = plugin.getGameManager().getMatch();

        if (!match.isParticipant(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " has not joined the game!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(Component.text("Cannot change roles during an active game!", NamedTextColor.RED));
            return true;
        }

        plugin.getPlayerManager().applyRoleToPlayer(target, PlayerRole.HUNTER);
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

        UUID ownerUuid = sender instanceof Player player ? player.getUniqueId() : null;
        plugin.getGameManager().startGameForce(ownerUuid);
        sender.sendMessage(Component.text("Game force started!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleMode(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(Component.text("Cannot change mode during an active game!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 1) {
            ManhuntGameMode currentGameMode = plugin.getGameManager().getMatch().getGameMode();
            StartMode currentStartMode = plugin.getGameManager().getMatch().getStartMode();
            sender.sendMessage(Component.text("Current game mode: ", NamedTextColor.GRAY)
                    .append(Component.text(currentGameMode.getDisplayName(), NamedTextColor.AQUA)));
            sender.sendMessage(Component.text("Current start mode: ", NamedTextColor.GRAY)
                    .append(Component.text(currentStartMode.getDisplayName(), NamedTextColor.AQUA)));
            sender.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                    .append(Component.text("/manhunt mode <normal|infection> <dreamstart|headstart>", NamedTextColor.WHITE)));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                    .append(Component.text("/manhunt mode <normal|infection> <dreamstart|headstart>", NamedTextColor.WHITE)));
            return true;
        }

        ManhuntGameMode gameMode;
        if (args[1].toLowerCase().equals("infection")) {
            gameMode = ManhuntGameMode.INFECTION;
        } else if (args[1].toLowerCase().equals("normal")) {
            gameMode = ManhuntGameMode.NORMAL;
        } else {
            sender.sendMessage(Component.text("Invalid game mode! Must be 'normal' or 'infection'.", NamedTextColor.RED));
            return true;
        }

        StartMode startMode;
        if (args[2].toLowerCase().equals("headstart")) {
            startMode = StartMode.HEADSTART;
        } else if (args[2].toLowerCase().equals("dreamstart")) {
            startMode = StartMode.DREAMSTART;
        } else {
            sender.sendMessage(Component.text("Invalid start mode! Must be 'dreamstart' or 'headstart'.", NamedTextColor.RED));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        match.setGameMode(gameMode);
        match.setStartMode(startMode);

        sender.sendMessage(Component.text("Game mode set to ", NamedTextColor.GREEN)
                .append(Component.text(gameMode.getDisplayName(), NamedTextColor.AQUA)));
        sender.sendMessage(Component.text("Start mode set to ", NamedTextColor.GREEN)
                .append(Component.text(startMode.getDisplayName(), NamedTextColor.AQUA)));

        if (gameMode == ManhuntGameMode.INFECTION) {
            sender.sendMessage(Component.text("Infection mode: Runners are permanently converted to hunters when killed.", NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                    .append(Component.text("/manhunt remove <player>", NamedTextColor.WHITE)));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        if (!match.isParticipant(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " is not in the game!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(Component.text("Cannot change roles during an active game!", NamedTextColor.RED));
            return true;
        }

        plugin.getPlayerManager().applyRoleToPlayer(target, PlayerRole.SPECTATOR);
        sender.sendMessage(Component.text(target.getName(), NamedTextColor.AQUA)
                .append(Component.text("'s role has been removed!", NamedTextColor.YELLOW)));
        target.sendMessage(Component.text("Your role has been removed!", NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleKick(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                    .append(Component.text("/manhunt kick <player>", NamedTextColor.WHITE)));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        if (!match.isParticipant(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " is not in the game!", NamedTextColor.RED));
            return true;
        }

        plugin.getPlayerManager().removePlayerFromGame(target.getUniqueId());

        if (plugin.getGameManager().isGameActive()) {
            org.bukkit.World mainWorld = org.bukkit.Bukkit.getWorlds().get(0);
            target.teleport(mainWorld.getSpawnLocation());
            plugin.getUiManager().sendToAll("<red>" + target.getName() + " has been kicked from the game!");
        }

        sender.sendMessage(Component.text(target.getName(), NamedTextColor.AQUA)
                .append(Component.text(" has been kicked from the game!", NamedTextColor.RED)));
        target.sendMessage(Component.text("You have been kicked from the game!", NamedTextColor.RED));
        return true;
    }

    private boolean handlePause(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getGameManager().getMatch().getState() != GameState.RUNNING &&
                plugin.getGameManager().getMatch().getState() != GameState.PRE_HUNT &&
                plugin.getGameManager().getMatch().getState() != GameState.HEADSTART) {
            player.sendMessage(Component.text("No active game to pause!", NamedTextColor.RED));
            return true;
        }

        if (!plugin.getGameManager().getMatch().isOwner(player.getUniqueId()) && !player.hasPermission("manhunt.admin")) {
            player.sendMessage(Component.text("Only the game owner can pause the game!", NamedTextColor.RED));
            return true;
        }

        if (!plugin.getGameManager().pauseGame(player.getUniqueId())) {
            player.sendMessage(Component.text("Failed to pause the game!", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleResume(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getGameManager().getMatch().getState() != GameState.PAUSED) {
            player.sendMessage(Component.text("No paused game to resume!", NamedTextColor.RED));
            return true;
        }

        if (!plugin.getGameManager().getMatch().isOwner(player.getUniqueId()) && !player.hasPermission("manhunt.admin")) {
            player.sendMessage(Component.text("Only the game owner can resume the game!", NamedTextColor.RED));
            return true;
        }

        if (!plugin.getGameManager().resumeGame(player.getUniqueId())) {
            player.sendMessage(Component.text("Failed to resume the game!", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleOwner(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            UUID ownerUuid = plugin.getGameManager().getMatch().getOwnerUuid();
            if (ownerUuid != null) {
                Player owner = Bukkit.getPlayer(ownerUuid);
                String ownerName = owner != null ? owner.getName() : "Unknown";
                sender.sendMessage(Component.text("Game owner: ", NamedTextColor.GRAY)
                        .append(Component.text(ownerName, NamedTextColor.AQUA)));
            } else {
                sender.sendMessage(Component.text("No game owner set.", NamedTextColor.GRAY));
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }

        plugin.getGameManager().getMatch().setOwnerUuid(target.getUniqueId());
        sender.sendMessage(Component.text(target.getName(), NamedTextColor.AQUA)
                .append(Component.text(" is now the game owner!", NamedTextColor.GREEN)));
        target.sendMessage(Component.text("You are now the game owner!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSeed(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(Component.text("Cannot change seed during an active game!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            Long currentSeed = plugin.getGameManager().getMatch().getSeed();
            if (currentSeed != null) {
                sender.sendMessage(Component.text("Current seed: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(currentSeed), NamedTextColor.AQUA)));
            } else {
                sender.sendMessage(Component.text("No seed set (will use random).", NamedTextColor.GRAY));
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
            sender.sendMessage(Component.text("World seed set to ", NamedTextColor.GREEN)
                    .append(Component.text(String.valueOf(seed), NamedTextColor.AQUA))
                    .append(Component.text(" (\""))
                    .append(Component.text(seedArg, NamedTextColor.YELLOW))
                    .append(Component.text("\")", NamedTextColor.GREEN)));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid seed value!", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleWorld(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(Component.text("Cannot change world during an active game!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            String currentWorld = plugin.getGameManager().getMatch().getWorldName();
            if (currentWorld != null) {
                sender.sendMessage(Component.text("Current world: ", NamedTextColor.GRAY)
                        .append(Component.text(currentWorld, NamedTextColor.AQUA)));
            } else {
                sender.sendMessage(Component.text("No world set (will generate a new world).", NamedTextColor.GRAY));
            }
            return true;
        }

        String worldName = args[1];

        if (worldName.equalsIgnoreCase("clear") || worldName.equalsIgnoreCase("reset")) {
            plugin.getGameManager().getMatch().setWorldName(null);
            sender.sendMessage(Component.text("World cleared. Will generate a new world on start.", NamedTextColor.GREEN));
            return true;
        }

        plugin.getGameManager().getMatch().setWorldName(worldName);
        sender.sendMessage(Component.text("World set to ", NamedTextColor.GREEN)
                .append(Component.text(worldName, NamedTextColor.AQUA))
                .append(Component.text(". Will use this world on start.")));
        return true;
    }

    private boolean handleChat(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            ChatMode mode = plugin.getChatManager().getChatMode(player.getUniqueId());
            player.sendMessage(Component.text("Current chat mode: ", NamedTextColor.GRAY)
                    .append(Component.text(mode.name(),
                            mode == ChatMode.GLOBAL ? NamedTextColor.GOLD : NamedTextColor.GREEN)));
            player.sendMessage(Component.text("Usage: /manhunt chat <global|team>", NamedTextColor.GRAY));
            return true;
        }

        PlayerRole role = plugin.getPlayerManager().getRole(player.getUniqueId());
        if (role == PlayerRole.SPECTATOR) {
            player.sendMessage(Component.text("Spectators can only use global chat!", NamedTextColor.RED));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "global", "g" -> {
                plugin.getChatManager().setChatMode(player.getUniqueId(), ChatMode.GLOBAL);
                player.sendMessage(Component.text("Chat mode set to ", NamedTextColor.GRAY)
                        .append(Component.text("GLOBAL", NamedTextColor.GOLD)));
            }
            case "team", "t" -> {
                if (plugin.getChatManager().isTeamSinglePlayer(player.getUniqueId())) {
                    plugin.getChatManager().setChatMode(player.getUniqueId(), ChatMode.GLOBAL);
                    player.sendMessage(Component.text("Chat mode set to ", NamedTextColor.GRAY)
                            .append(Component.text("GLOBAL", NamedTextColor.GOLD)));
                    player.sendMessage(Component.text("Cannot use team chat - you're the only one on your team.", NamedTextColor.YELLOW));
                } else {
                    plugin.getChatManager().setChatMode(player.getUniqueId(), ChatMode.TEAM);
                    player.sendMessage(Component.text("Chat mode set to ", NamedTextColor.GRAY)
                            .append(Component.text("TEAM", NamedTextColor.GREEN)));
                }
            }
            default -> player.sendMessage(Component.text("Usage: /manhunt chat <global|team>", NamedTextColor.RED));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        GameState state = plugin.getGameManager().getMatch().getState();
        String stateName = switch (state) {
            case WAITING -> "Waiting";
            case COUNTDOWN -> "Countdown";
            case HEADSTART -> "Head Start";
            case PRE_HUNT -> "Pre-Hunt";
            case RUNNING -> "Running";
            case PAUSED -> "Paused";
            case FINISHED -> "Finished";
        };

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("ManhuntNG", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(" \u2014 ", NamedTextColor.DARK_GRAY))
                .append(Component.text(stateName, getTextColor(state), TextDecoration.BOLD)));
        sender.sendMessage(Component.text("  \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500", NamedTextColor.DARK_GRAY));

        Component playerHeader = Component.text("  Player", NamedTextColor.YELLOW, TextDecoration.BOLD);
        sender.sendMessage(playerHeader);

        helpEntry(sender, "manhunt join", "Join the lobby",
                "Click to join", "manhunt.play");
        helpEntry(sender, "manhunt leave", "Leave the lobby",
                "Click to leave", "manhunt.play");
        helpEntry(sender, "manhunt pause", "Pause the game (owner only)",
                "Click to pause", "manhunt.play");
        helpEntry(sender, "manhunt resume", "Resume the game (owner only)",
                "Click to resume", "manhunt.play");
        helpEntry(sender, "manhunt chat <global|team>", "Switch chat mode",
                "Click to switch chat mode", "manhunt.play");
        helpEntry(sender, "g <message>", "Send a global message",
                "Click to send global message", "manhunt.play");
        helpEntry(sender, "t <message>", "Send a team message",
                "Click to send team message", "manhunt.play");

        if (sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("  Admin", NamedTextColor.RED, TextDecoration.BOLD));

            helpEntry(sender, "manhunt start", "Start the match (uses selected mode)",
                    "Click to start", "manhunt.admin");
            helpEntry(sender, "manhunt stop", "Force stop the match",
                    "Click to stop", "manhunt.admin");
            helpEntry(sender, "manhunt runner <player>", "Set the Runner",
                    "Click to set runner", "manhunt.admin");
            helpEntry(sender, "manhunt hunter <player>", "Add a Hunter",
                    "Click to set hunter", "manhunt.admin");
            helpEntry(sender, "manhunt remove <player>", "Remove a player's role",
                    "Click to remove role", "manhunt.admin");
            helpEntry(sender, "manhunt kick <player>", "Kick a player from the game",
                    "Click to kick player", "manhunt.admin");
            helpEntry(sender, "manhunt owner [player]", "View/set game owner",
                    "Click to set owner", "manhunt.admin");
            helpEntry(sender, "manhunt seed [value]", "View/set world seed",
                    "Click to set seed", "manhunt.admin");
            helpEntry(sender, "manhunt world [name]", "View/set existing world to use",
                    "Click to set world", "manhunt.admin");
            helpEntry(sender, "manhunt mode <normal|infection> <dreamstart|headstart>", "Set game and start mode",
                    "Click to set mode", "manhunt.admin");
            helpEntry(sender, "manhunt forcestart", "Skip validation & start",
                    "Click to force start", "manhunt.admin");
            helpEntry(sender, "manhunt reload", "Reload configuration",
                    "Click to reload", "manhunt.admin");
            helpEntry(sender, "manhunt debug", "Show debug information",
                    "Click to show debug info", "manhunt.admin");
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

    private void updateEntryWithMode(CommandSender sender, String gameModeName) {
        String command = "manhunt mode " + gameModeName + " dreamstart";
        String description = "Set game mode to " + gameModeName + " with dreamstart";
        String hoverText = "Click to set " + gameModeName + " mode";
        String permission = "manhunt.admin";

        helpEntry(sender, command, description, hoverText, permission);
    }

    private void updateEntryWithModeAndHeadstart(CommandSender sender, String gameModeName) {
        String command = "manhunt mode " + gameModeName + " headstart";
        String description = "Set game mode to " + gameModeName + " with headstart";
        String hoverText = "Click to set " + gameModeName + " mode with headstart";
        String permission = "manhunt.admin";

        helpEntry(sender, command, description, hoverText, permission);
    }

    private NamedTextColor getTextColor(GameState state) {
        return switch (state) {
            case WAITING -> NamedTextColor.GREEN;
            case COUNTDOWN -> NamedTextColor.YELLOW;
            case HEADSTART -> NamedTextColor.LIGHT_PURPLE;
            case PRE_HUNT -> NamedTextColor.GOLD;
            case RUNNING -> NamedTextColor.RED;
            case PAUSED -> NamedTextColor.AQUA;
            case FINISHED -> NamedTextColor.DARK_GRAY;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("join");
            completions.add("leave");
            completions.add("chat");
            if (sender.hasPermission("manhunt.admin")) {
                completions.add("start");
                completions.add("stop");
                completions.add("reload");
                completions.add("runner");
                completions.add("hunter");
                completions.add("remove");
                completions.add("kick");
                completions.add("forcestart");
                completions.add("mode");
                completions.add("owner");
                completions.add("seed");
                completions.add("world");
                completions.add("debug");
            }
            completions.add("pause");
            completions.add("resume");
            return filterPartial(args[0], completions);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("chat")) {
                completions.add("global");
                completions.add("team");
                return filterPartial(args[1], completions);
            }
            if (sub.equals("runner") || sub.equals("hunter") || sub.equals("owner") || sub.equals("remove") || sub.equals("kick")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
                return filterPartial(args[1], completions);
            }
            if (sub.equals("world")) {
                for (World world : Bukkit.getWorlds()) {
                    completions.add(world.getName());
                }
                completions.add("clear");
                return filterPartial(args[1], completions);
            }
            if (sub.equals("debug")) {
                completions.add("lastknown");
                return filterPartial(args[1], completions);
            }
            if (sub.equals("mode")) {
                completions.add("normal");
                completions.add("infection");
                return filterPartial(args[1], completions);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("mode")) {
                String gameMode = args[1].toLowerCase();
                if (gameMode.equals("normal") || gameMode.equals("infection")) {
                    completions.add("dreamstart");
                    completions.add("headstart");
                }
                return filterPartial(args[2], completions);
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
