package xyz.qincai.manhunt.command.sub;

import org.bukkit.command.CommandSender;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;

public class ReloadSubcommand implements Subcommand {
    @Override public String getName() { return "reload"; }
    @Override public String getPermission() { return "manhunt.admin"; }
    @Override public boolean requirePlayer() { return false; }
    @Override public boolean requireAdmin() { return true; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        plugin.getConfigManager().reloadConfigs();
        sender.sendMessage(cfg(plugin).getMessageComponent("admin.config-reloaded"));
        return true;
    }
}
