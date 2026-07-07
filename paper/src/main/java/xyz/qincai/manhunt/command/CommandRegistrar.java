package xyz.qincai.manhunt.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import xyz.qincai.manhunt.ManhuntNG;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandRegistrar {
    private final ManhuntNG plugin;
    private final Map<String, Command> registeredCommands = new HashMap<>();

    public CommandRegistrar(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void register(String name, String description, CommandExecutor executor, TabCompleter tabCompleter) {
        Command command = new Command(name) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                if (executor != null) {
                    return executor.onCommand(sender, this, commandLabel, args);
                }
                return false;
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String commandLabel, String[] args) {
                if (tabCompleter != null) {
                    return tabCompleter.onTabComplete(sender, this, commandLabel, args);
                }
                return super.tabComplete(sender, commandLabel, args);
            }
        };
        command.setDescription(description);

        CommandMap commandMap = getCommandMap();
        if (commandMap != null) {
            commandMap.register(plugin.getName().toLowerCase(), command);
            registeredCommands.put(name, command);
        }
    }

    public void unregisterAll() {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) return;

        for (Map.Entry<String, Command> entry : registeredCommands.entrySet()) {
            entry.getValue().unregister(commandMap);
        }
        registeredCommands.clear();
    }

    private CommandMap getCommandMap() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to access CommandMap: " + e.getMessage());
            return null;
        }
    }
}
