package xyz.qincai.manhunt.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.command.Subcommand;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class ShuffleSubcommand implements Subcommand {
    @Override public String getName() { return "shuffle"; }
    @Override public String getPermission() { return "manhunt.admin"; }
    @Override public boolean requirePlayer() { return false; }
    @Override public boolean requireAdmin() { return true; }

    @Override
    public boolean execute(CommandSender sender, ManhuntNG plugin, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(cfg(plugin).getMessageComponent("usage.shuffle"));
            return true;
        }

        int runnerCount;
        try {
            runnerCount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.invalid-number"));
            return true;
        }

        if (runnerCount < 1) {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.invalid-number"));
            return true;
        }

        if (plugin.getGameManager().isGameActive()) {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.cannot-change-roles"));
            return true;
        }

        Match match = plugin.getGameManager().getMatch();

        Set<UUID> participantSet = new HashSet<>();
        participantSet.addAll(match.getRunnerUuids());
        participantSet.addAll(match.getHunterUuids());
        participantSet.addAll(match.getSpectatorUuids());

        List<UUID> onlineParticipants = new ArrayList<>();
        for (UUID uuid : participantSet) {
            if (Bukkit.getPlayer(uuid) != null) {
                onlineParticipants.add(uuid);
            }
        }

        if (runnerCount >= onlineParticipants.size() || onlineParticipants.size() < (long) runnerCount + 1) {
            sender.sendMessage(cfg(plugin).getMessageComponent("error.not-enough-players",
                    "{needed}", String.valueOf(runnerCount + 1)));
            return true;
        }

        for (UUID uuid : participantSet) {
            match.getRunnerUuids().remove(uuid);
            match.getHunterUuids().remove(uuid);
            match.addSpectator(uuid);
            plugin.getPlayerManager().setRole(uuid, PlayerRole.SPECTATOR);
        }

        Collections.shuffle(onlineParticipants, new Random());

        int assignedRunners = 0;
        int assignedHunters = 0;
        for (int i = 0; i < onlineParticipants.size(); i++) {
            UUID uuid = onlineParticipants.get(i);
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            if (i < runnerCount) {
                plugin.getPlayerManager().applyRoleToPlayer(player, PlayerRole.RUNNER);
                assignedRunners++;
            } else {
                plugin.getPlayerManager().applyRoleToPlayer(player, PlayerRole.HUNTER);
                assignedHunters++;
            }
        }

        sender.sendMessage(cfg(plugin).getMessageComponent("role.shuffle-result",
                "{runners}", String.valueOf(assignedRunners),
                "{hunters}", String.valueOf(assignedHunters)));
        return true;
    }
}
