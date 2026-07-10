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
import xyz.qincai.manhunt.config.ConfigManager;
import xyz.qincai.manhunt.game.ManhuntGameMode;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.game.StartMode;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ManhuntCommand implements CommandExecutor, TabCompleter {
    private final ManhuntNG plugin;

    public ManhuntCommand(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    private ConfigManager cfg() {
        return plugin.getConfigManager();
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
            case "shuffle" -> handleShuffle(sender, args);
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
            sender.sendMessage(cfg().getMessageComponent("error.only-players"));
            return true;
        }

        if (!player.hasPermission("manhunt.play")) {
            player.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        if (match.isParticipant(player.getUniqueId())) {
            player.sendMessage(cfg().getMessageComponent("error.already-joined"));
            return true;
        }

        plugin.getPlayerManager().setRole(player.getUniqueId(), PlayerRole.SPECTATOR);
        match.addSpectator(player.getUniqueId());

        if (plugin.getGameManager().isGameActive()) {
            org.bukkit.World gameWorld = match.getGameWorld();
            if (gameWorld != null) {
                player.teleport(gameWorld.getSpawnLocation());
            }
            plugin.getUiManager().sendToAll(cfg().getMessage("join.broadcast", "{player}", player.getName()));
            player.sendMessage(cfg().getMessageComponent("join.spectator"));
        } else {
            player.sendMessage(cfg().getMessageComponent("join.lobby"));
        }
        return true;
    }

    private boolean handleLeave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(cfg().getMessageComponent("error.only-players"));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        if (!match.isParticipant(player.getUniqueId())) {
            player.sendMessage(cfg().getMessageComponent("error.not-in-game", "{player}", player.getName()));
            return true;
        }

        plugin.getPlayerManager().removePlayerFromGame(player.getUniqueId());

        if (plugin.getGameManager().isGameActive()) {
            org.bukkit.World mainWorld = org.bukkit.Bukkit.getWorlds().get(0);
            player.teleport(mainWorld.getSpawnLocation());
            plugin.getUiManager().sendToAll(cfg().getMessage("leave.broadcast", "{player}", player.getName()));
        }

        player.sendMessage(cfg().getMessageComponent("leave.lobby"));
        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        UUID ownerUuid = sender instanceof Player player ? player.getUniqueId() : null;
        plugin.getGameManager().startGame(ownerUuid);
        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        plugin.getGameManager().stopGame();
        sender.sendMessage(cfg().getMessageComponent("admin.game-stopped"));
        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        plugin.getConfigManager().reloadConfigs();
        sender.sendMessage(cfg().getMessageComponent("admin.config-reloaded"));
        return true;
    }

    private boolean handleRunner(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(cfg().getMessageComponent("usage.runner"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(cfg().getMessageComponent("error.player-not-found"));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        if (!match.isParticipant(target.getUniqueId())) {
            sender.sendMessage(cfg().getMessageComponent("role.not-joined", "{player}", target.getName()));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(cfg().getMessageComponent("error.cannot-change-roles"));
            return true;
        }

        plugin.getPlayerManager().applyRoleToPlayer(target, PlayerRole.RUNNER);
        sender.sendMessage(cfg().getMessageComponent("role.set-runner-sender", "{player}", target.getName()));
        target.sendMessage(cfg().getMessageComponent("role.set-runner-target"));
        return true;
    }

    private boolean handleHunter(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(cfg().getMessageComponent("usage.hunter"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(cfg().getMessageComponent("error.player-not-found"));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        if (!match.isParticipant(target.getUniqueId())) {
            sender.sendMessage(cfg().getMessageComponent("role.not-joined", "{player}", target.getName()));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(cfg().getMessageComponent("error.cannot-change-roles"));
            return true;
        }

        plugin.getPlayerManager().applyRoleToPlayer(target, PlayerRole.HUNTER);
        sender.sendMessage(cfg().getMessageComponent("role.set-hunter-sender", "{player}", target.getName()));
        target.sendMessage(cfg().getMessageComponent("role.set-hunter-target"));
        return true;
    }

    private boolean handleForceStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        UUID ownerUuid = sender instanceof Player player ? player.getUniqueId() : null;
        plugin.getGameManager().startGameForce(ownerUuid);
        sender.sendMessage(cfg().getMessageComponent("admin.force-started"));
        return true;
    }

    private boolean handleShuffle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(cfg().getMessageComponent("usage.shuffle"));
            return true;
        }

        int runnerCount;
        try {
            runnerCount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(cfg().getMessageComponent("error.invalid-number"));
            return true;
        }

        if (runnerCount < 1) {
            sender.sendMessage(cfg().getMessageComponent("error.invalid-number"));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(cfg().getMessageComponent("error.cannot-change-roles"));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        List<UUID> participants = new ArrayList<>();
        participants.addAll(match.getRunnerUuids());
        participants.addAll(match.getHunterUuids());
        participants.addAll(match.getSpectatorUuids());

        participants.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);

        if (participants.size() < runnerCount + 1) {
            sender.sendMessage(cfg().getMessageComponent("error.not-enough-players",
                    "{needed}", String.valueOf(runnerCount + 1)));
            return true;
        }

        Collections.shuffle(participants, new Random());

        for (UUID uuid : participants) {
            match.getRunnerUuids().remove(uuid);
            match.getHunterUuids().remove(uuid);
            match.addSpectator(uuid);
            plugin.getPlayerManager().setRole(uuid, PlayerRole.SPECTATOR);
        }

        for (int i = 0; i < participants.size(); i++) {
            UUID uuid = participants.get(i);
            Player player = Bukkit.getPlayer(uuid);
            PlayerRole role = i < runnerCount ? PlayerRole.RUNNER : PlayerRole.HUNTER;
            plugin.getPlayerManager().applyRoleToPlayer(player, role);
        }

        sender.sendMessage(cfg().getMessageComponent("role.shuffle-result",
                "{runners}", String.valueOf(runnerCount),
                "{hunters}", String.valueOf(participants.size() - runnerCount)));
        return true;
    }

    private boolean handleMode(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(cfg().getMessageComponent("error.cannot-change-mode"));
            return true;
        }

        if (args.length == 1) {
            ManhuntGameMode currentGameMode = plugin.getGameManager().getMatch().getGameMode();
            StartMode currentStartMode = plugin.getGameManager().getMatch().getStartMode();
            sender.sendMessage(cfg().getMessageComponent("mode.current-game", "{mode}", currentGameMode.getDisplayName(cfg())));
            sender.sendMessage(cfg().getMessageComponent("mode.current-start", "{mode}", currentStartMode.getDisplayName(cfg())));
            sender.sendMessage(cfg().getMessageComponent("usage.mode"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(cfg().getMessageComponent("usage.mode"));
            return true;
        }

        ManhuntGameMode gameMode;
        if (args[1].toLowerCase().equals("infection")) {
            gameMode = ManhuntGameMode.INFECTION;
        } else if (args[1].toLowerCase().equals("normal")) {
            gameMode = ManhuntGameMode.NORMAL;
        } else {
            sender.sendMessage(cfg().getMessageComponent("error.invalid-game-mode"));
            return true;
        }

        StartMode startMode;
        if (args[2].toLowerCase().equals("headstart")) {
            startMode = StartMode.HEADSTART;
        } else if (args[2].toLowerCase().equals("dreamstart")) {
            startMode = StartMode.DREAMSTART;
        } else {
            sender.sendMessage(cfg().getMessageComponent("error.invalid-start-mode"));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        match.setGameMode(gameMode);
        match.setStartMode(startMode);

        sender.sendMessage(cfg().getMessageComponent("mode.game-set", "{mode}", gameMode.getDisplayName(cfg())));
        sender.sendMessage(cfg().getMessageComponent("mode.start-set", "{mode}", startMode.getDisplayName(cfg())));

        if (gameMode == ManhuntGameMode.INFECTION) {
            sender.sendMessage(cfg().getMessageComponent("mode.infection-info"));
        }
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(cfg().getMessageComponent("usage.remove"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(cfg().getMessageComponent("error.player-not-found"));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        if (!match.isParticipant(target.getUniqueId())) {
            sender.sendMessage(cfg().getMessageComponent("error.not-in-game", "{player}", target.getName()));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(cfg().getMessageComponent("error.cannot-change-roles"));
            return true;
        }

        plugin.getPlayerManager().applyRoleToPlayer(target, PlayerRole.SPECTATOR);
        sender.sendMessage(cfg().getMessageComponent("role.removed-sender", "{player}", target.getName()));
        target.sendMessage(cfg().getMessageComponent("role.removed-target"));
        return true;
    }

    private boolean handleKick(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(cfg().getMessageComponent("usage.kick"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(cfg().getMessageComponent("error.player-not-found"));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        if (!match.isParticipant(target.getUniqueId())) {
            sender.sendMessage(cfg().getMessageComponent("error.not-in-game", "{player}", target.getName()));
            return true;
        }

        plugin.getPlayerManager().removePlayerFromGame(target.getUniqueId());

        if (plugin.getGameManager().isGameActive()) {
            org.bukkit.World mainWorld = org.bukkit.Bukkit.getWorlds().get(0);
            target.teleport(mainWorld.getSpawnLocation());
            plugin.getUiManager().sendToAll(cfg().getMessage("admin.kick-broadcast", "{player}", target.getName()));
        }

        sender.sendMessage(cfg().getMessageComponent("admin.kick-sender", "{player}", target.getName()));
        target.sendMessage(cfg().getMessageComponent("admin.kick-target"));
        return true;
    }

    private boolean handlePause(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(cfg().getMessageComponent("error.only-players"));
            return true;
        }

        if (plugin.getGameManager().getMatch().getState() != GameState.RUNNING &&
                plugin.getGameManager().getMatch().getState() != GameState.PRE_HUNT &&
                plugin.getGameManager().getMatch().getState() != GameState.HEADSTART) {
            player.sendMessage(cfg().getMessageComponent("error.no-active-game-to-pause"));
            return true;
        }

        if (!plugin.getGameManager().getMatch().isOwner(player.getUniqueId()) && !player.hasPermission("manhunt.admin")) {
            player.sendMessage(cfg().getMessageComponent("error.only-owner-can-pause"));
            return true;
        }

        if (!plugin.getGameManager().pauseGame(player.getUniqueId())) {
            player.sendMessage(cfg().getMessageComponent("error.failed-to-pause"));
        }
        return true;
    }

    private boolean handleResume(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(cfg().getMessageComponent("error.only-players"));
            return true;
        }

        if (plugin.getGameManager().getMatch().getState() != GameState.PAUSED) {
            player.sendMessage(cfg().getMessageComponent("error.no-paused-game"));
            return true;
        }

        if (!plugin.getGameManager().getMatch().isOwner(player.getUniqueId()) && !player.hasPermission("manhunt.admin")) {
            player.sendMessage(cfg().getMessageComponent("error.only-owner-can-resume"));
            return true;
        }

        if (!plugin.getGameManager().resumeGame(player.getUniqueId())) {
            player.sendMessage(cfg().getMessageComponent("error.failed-to-resume"));
        }
        return true;
    }

    private boolean handleOwner(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        if (args.length < 2) {
            UUID ownerUuid = plugin.getGameManager().getMatch().getOwnerUuid();
            if (ownerUuid != null) {
                Player owner = Bukkit.getPlayer(ownerUuid);
                String ownerName = owner != null ? owner.getName() : "Unknown";
                sender.sendMessage(cfg().getMessageComponent("admin.owner-show", "{player}", ownerName));
            } else {
                sender.sendMessage(cfg().getMessageComponent("admin.owner-none"));
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(cfg().getMessageComponent("error.player-not-found"));
            return true;
        }

        plugin.getGameManager().getMatch().setOwnerUuid(target.getUniqueId());
        sender.sendMessage(cfg().getMessageComponent("admin.owner-set-sender", "{player}", target.getName()));
        target.sendMessage(cfg().getMessageComponent("admin.owner-set-target"));
        return true;
    }

    private boolean handleSeed(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(cfg().getMessageComponent("error.cannot-change-seed"));
            return true;
        }

        if (args.length < 2) {
            Long currentSeed = plugin.getGameManager().getMatch().getSeed();
            if (currentSeed != null) {
                sender.sendMessage(cfg().getMessageComponent("admin.seed-show", "{seed}", String.valueOf(currentSeed)));
            } else {
                sender.sendMessage(cfg().getMessageComponent("admin.seed-none"));
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
            sender.sendMessage(cfg().getMessageComponent("admin.seed-set",
                    "{seed}", String.valueOf(seed),
                    "{input}", seedArg));
        } catch (NumberFormatException e) {
            sender.sendMessage(cfg().getMessageComponent("error.invalid-seed"));
        }
        return true;
    }

    private boolean handleWorld(CommandSender sender, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(cfg().getMessageComponent("error.no-permission"));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(cfg().getMessageComponent("error.cannot-change-world"));
            return true;
        }

        if (args.length < 2) {
            String currentWorld = plugin.getGameManager().getMatch().getWorldName();
            if (currentWorld != null) {
                sender.sendMessage(cfg().getMessageComponent("admin.world-show", "{world}", currentWorld));
            } else {
                sender.sendMessage(cfg().getMessageComponent("admin.world-none"));
            }
            return true;
        }

        String worldName = args[1];

        if (worldName.equalsIgnoreCase("clear") || worldName.equalsIgnoreCase("reset")) {
            plugin.getGameManager().getMatch().setWorldName(null);
            sender.sendMessage(cfg().getMessageComponent("admin.world-cleared"));
            return true;
        }

        plugin.getGameManager().getMatch().setWorldName(worldName);
        sender.sendMessage(cfg().getMessageComponent("admin.world-set", "{world}", worldName));
        return true;
    }

    private boolean handleChat(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(cfg().getMessageComponent("error.only-players"));
            return true;
        }

        if (args.length < 2) {
            ChatMode mode = plugin.getChatManager().getChatMode(player.getUniqueId());
            player.sendMessage(cfg().getMessageComponent("chat.mode-show")
                    .append(Component.text(mode.name(),
                            mode == ChatMode.GLOBAL ? NamedTextColor.GOLD : NamedTextColor.GREEN)));
            player.sendMessage(cfg().getMessageComponent("usage.chat-mode"));
            return true;
        }

        PlayerRole role = plugin.getPlayerManager().getRole(player.getUniqueId());
        if (role == PlayerRole.SPECTATOR) {
            player.sendMessage(cfg().getMessageComponent("error.spectator-global-only"));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "global", "g" -> {
                plugin.getChatManager().setChatMode(player.getUniqueId(), ChatMode.GLOBAL);
                player.sendMessage(cfg().getMessageComponent("chat.mode-set-global"));
            }
            case "team", "t" -> {
                if (plugin.getChatManager().isTeamSinglePlayer(player.getUniqueId())) {
                    plugin.getChatManager().setChatMode(player.getUniqueId(), ChatMode.GLOBAL);
                    player.sendMessage(cfg().getMessageComponent("chat.mode-set-global"));
                    player.sendMessage(cfg().getMessageComponent("error.cannot-team-single"));
                } else {
                    plugin.getChatManager().setChatMode(player.getUniqueId(), ChatMode.TEAM);
                    player.sendMessage(cfg().getMessageComponent("chat.mode-set-team"));
                }
            }
            default -> player.sendMessage(cfg().getMessageComponent("usage.chat-mode"));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        ConfigManager config = cfg();
        GameState state = plugin.getGameManager().getMatch().getState();
        String stateName = switch (state) {
            case WAITING -> config.getMessage("state.waiting");
            case COUNTDOWN -> config.getMessage("state.countdown");
            case HEADSTART -> config.getMessage("state.headstart");
            case PRE_HUNT -> config.getMessage("state.prehunt");
            case RUNNING -> config.getMessage("state.running");
            case PAUSED -> config.getMessage("state.paused");
            case FINISHED -> config.getMessage("state.finished");
        };

        sender.sendMessage(Component.empty());
        sender.sendMessage(config.getMessageComponent("help.title")
                .append(config.getMessageComponent("help.separator"))
                .append(Component.text(stateName, getTextColor(state), TextDecoration.BOLD)));
        sender.sendMessage(config.getMessageComponent("help.divider"));

        Component playerHeader = Component.text("  ", NamedTextColor.WHITE)
                .append(config.getMessageComponent("help.section-player")
                        .decoration(TextDecoration.BOLD, true));
        sender.sendMessage(playerHeader);

        helpEntry(sender, config, "help.join");
        helpEntry(sender, config, "help.leave");
        helpEntry(sender, config, "help.pause");
        helpEntry(sender, config, "help.resume");
        helpEntry(sender, config, "help.chat");
        helpEntry(sender, config, "help.g");
        helpEntry(sender, config, "help.t");

        if (sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("  ", NamedTextColor.WHITE)
                    .append(config.getMessageComponent("help.section-admin")
                            .decoration(TextDecoration.BOLD, true)));

            helpEntry(sender, config, "help.start");
            helpEntry(sender, config, "help.stop");
            helpEntry(sender, config, "help.runner");
            helpEntry(sender, config, "help.hunter");
            helpEntry(sender, config, "help.remove");
            helpEntry(sender, config, "help.kick");
            helpEntry(sender, config, "help.owner");
            helpEntry(sender, config, "help.seed");
            helpEntry(sender, config, "help.world");
            helpEntry(sender, config, "help.mode");
            helpEntry(sender, config, "help.forcestart");
            helpEntry(sender, config, "help.shuffle");
            helpEntry(sender, config, "help.reload");
            helpEntry(sender, config, "help.debug");
        }

        sender.sendMessage(Component.empty());
    }

    private void helpEntry(CommandSender sender, ConfigManager config, String prefix) {
        String command = config.getMessage(prefix + ".cmd");
        String description = config.getMessage(prefix + ".desc");
        String hoverText = config.getMessage(prefix + ".hover");
        String permissionLabel = config.getMessage("help.permission");
        String permission = config.getMessage(prefix + ".permission");

        Component cmd = Component.text("  /" + command, NamedTextColor.AQUA)
                .hoverEvent(HoverEvent.showText(
                        Component.text(hoverText + "\n", NamedTextColor.GRAY)
                                .append(Component.text(permissionLabel, NamedTextColor.DARK_GRAY))
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
                completions.add("shuffle");
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
