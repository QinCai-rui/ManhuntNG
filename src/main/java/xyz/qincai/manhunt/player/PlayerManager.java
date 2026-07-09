package xyz.qincai.manhunt.player;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    private final ManhuntNG plugin;
    private final Map<UUID, PlayerRole> playerRoles = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> hunterRespawns = new ConcurrentHashMap<>();

    public PlayerManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void reset() {
        playerRoles.clear();
        hunterRespawns.clear();
        plugin.getNameTagManager().clearAll();
    }

    public void setRole(UUID uuid, PlayerRole role) {
        playerRoles.put(uuid, role);
        plugin.getNameTagManager().applyTag(uuid, role);
    }

    public PlayerRole getRole(UUID uuid) {
        return playerRoles.getOrDefault(uuid, PlayerRole.SPECTATOR);
    }

    public boolean isRunner(UUID uuid) {
        PlayerRole role = playerRoles.get(uuid);
        return role == PlayerRole.RUNNER;
    }

    public boolean isHunter(UUID uuid) {
        PlayerRole role = playerRoles.get(uuid);
        return role == PlayerRole.HUNTER;
    }

    public UUID getRunnerUuid() {
        for (Map.Entry<UUID, PlayerRole> entry : playerRoles.entrySet()) {
            if (entry.getValue() == PlayerRole.RUNNER) {
                return entry.getKey();
            }
        }
        return null;
    }

    public java.util.Set<UUID> getRunnerUuids() {
        java.util.Set<UUID> runners = new java.util.HashSet<>();
        for (Map.Entry<UUID, PlayerRole> entry : playerRoles.entrySet()) {
            if (entry.getValue() == PlayerRole.RUNNER) {
                runners.add(entry.getKey());
            }
        }
        return runners;
    }

    public void addHunterRespawn(UUID uuid) {
        hunterRespawns.merge(uuid, 1, Integer::sum);
    }

    public int getHunterRespawnCount(UUID uuid) {
        return hunterRespawns.getOrDefault(uuid, 0);
    }

    public void clearHunterRespawns() {
        hunterRespawns.clear();
    }

    public void applyRoleToPlayer(Player player, PlayerRole role) {
        Match match = plugin.getGameManager().getMatch();
        switch (role) {
            case RUNNER -> {
                match.addRunner(player.getUniqueId());
                match.removeSpectator(player.getUniqueId());
                setRole(player.getUniqueId(), PlayerRole.RUNNER);
            }
            case HUNTER -> {
                match.addHunter(player.getUniqueId());
                match.removeSpectator(player.getUniqueId());
                setRole(player.getUniqueId(), PlayerRole.HUNTER);
            }
            case SPECTATOR -> {
                match.addSpectator(player.getUniqueId());
                setRole(player.getUniqueId(), PlayerRole.SPECTATOR);
            }
        }
    }

    public void infectRunnerToHunter(UUID runnerUuid) {
        Match match = plugin.getGameManager().getMatch();
        Player player = Bukkit.getPlayer(runnerUuid);
        if (player == null) return;

        match.removeRunner(runnerUuid);
        match.addHunter(runnerUuid);
        setRole(runnerUuid, PlayerRole.HUNTER);
        player.setGameMode(GameMode.SURVIVAL);

        // Clear runner potion effects before applying hunter effects
        for (org.bukkit.potion.PotionEffect effect : plugin.getConfigManager().getRunnerPotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        plugin.getTrackerManager().giveCompassToPlayer(player);
        plugin.getPotionEffectManager().applyHunterEffects(runnerUuid);
    }

    public void removePlayerFromGame(UUID uuid) {
        Match match = plugin.getGameManager().getMatch();
        match.getHunterUuids().remove(uuid);
        match.getSpectatorUuids().remove(uuid);
        match.removeRunner(uuid);
        playerRoles.remove(uuid);
        hunterRespawns.remove(uuid);
        plugin.getNameTagManager().clearTag(Bukkit.getPlayer(uuid));
    }

    public void eliminateRunner(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.setGameMode(GameMode.SPECTATOR);
            Match match = plugin.getGameManager().getMatch();
            match.addSpectator(uuid);
        }
    }

    public void eliminateHunter(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    public int getAliveHunterCount() {
        Match match = plugin.getGameManager().getMatch();
        int count = 0;
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.getGameMode() != GameMode.SPECTATOR) {
                count++;
            }
        }
        return count;
    }
}
