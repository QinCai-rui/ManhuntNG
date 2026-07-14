package xyz.qincai.manhunt.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.sub.ChatSubcommand;
import xyz.qincai.manhunt.command.sub.ForceStartSubcommand;
import xyz.qincai.manhunt.command.sub.HunterSubcommand;
import xyz.qincai.manhunt.command.sub.JoinSubcommand;
import xyz.qincai.manhunt.command.sub.KickSubcommand;
import xyz.qincai.manhunt.command.sub.LeaveSubcommand;
import xyz.qincai.manhunt.command.sub.ModeSubcommand;
import xyz.qincai.manhunt.command.sub.OwnerSubcommand;
import xyz.qincai.manhunt.command.sub.PauseSubcommand;
import xyz.qincai.manhunt.command.sub.ReloadSubcommand;
import xyz.qincai.manhunt.command.sub.RemoveSubcommand;
import xyz.qincai.manhunt.command.sub.ResumeSubcommand;
import xyz.qincai.manhunt.command.sub.RunnerSubcommand;
import xyz.qincai.manhunt.command.sub.SeedSubcommand;
import xyz.qincai.manhunt.command.sub.ShuffleSubcommand;
import xyz.qincai.manhunt.command.sub.StartSubcommand;
import xyz.qincai.manhunt.command.sub.StopSubcommand;
import xyz.qincai.manhunt.command.sub.WorldSubcommand;
import xyz.qincai.manhunt.config.ConfigManager;
import xyz.qincai.manhunt.game.GameState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ManhuntCommand implements CommandExecutor, TabCompleter {
    private final ManhuntNG plugin;
    private final Map<String, Subcommand> subcommands = new LinkedHashMap<>();
    private final ManhuntDebugCommand debugCommand;

    public ManhuntCommand(ManhuntNG plugin) {
        this.plugin = plugin;
        this.debugCommand = new ManhuntDebugCommand(plugin);

        register(new JoinSubcommand());
        register(new LeaveSubcommand());
        register(new StartSubcommand());
        register(new StopSubcommand());
        register(new ReloadSubcommand());
        register(new RunnerSubcommand());
        register(new HunterSubcommand());
        register(new RemoveSubcommand());
        register(new KickSubcommand());
        register(new ForceStartSubcommand());
        register(new ShuffleSubcommand());
        register(new ModeSubcommand());
        register(new PauseSubcommand());
        register(new ResumeSubcommand());
        register(new OwnerSubcommand());
        register(new SeedSubcommand());
        register(new WorldSubcommand());
        register(new ChatSubcommand());
    }

    private void register(Subcommand subcommand) {
        subcommands.put(subcommand.getName(), subcommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subName = args[0].toLowerCase();

        if (subName.equals("debug")) {
            String[] debugArgs = new String[args.length - 1];
            System.arraycopy(args, 1, debugArgs, 0, debugArgs.length);
            return debugCommand.onCommand(sender, null, null, debugArgs);
        }

        Subcommand sub = subcommands.get(subName);
        if (sub == null) {
            sendHelp(sender);
            return true;
        }

        if (!sub.validatePreconditions(sender, plugin, args)) {
            return true;
        }

        return sub.execute(sender, plugin, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (Subcommand sub : subcommands.values()) {
                if (sub.getPermission() == null || sender.hasPermission(sub.getPermission())) {
                    completions.add(sub.getName());
                }
            }
            completions.add("debug");
            return filterPartial(args[0], completions);
        }

        String subName = args[0].toLowerCase();
        if (subName.equals("debug")) {
            if (args.length == 2) {
                List<String> completions = new ArrayList<>();
                completions.add("lastknown");
                return filterPartial(args[1], completions);
            }
            return new ArrayList<>();
        }

        Subcommand sub = subcommands.get(subName);
        if (sub != null) {
            return filterPartial(args.length > 1 ? args[args.length - 1] : "", sub.tabComplete(sender, plugin, args));
        }

        return new ArrayList<>();
    }

    private void sendHelp(CommandSender sender) {
        ConfigManager config = plugin.getConfigManager();
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
