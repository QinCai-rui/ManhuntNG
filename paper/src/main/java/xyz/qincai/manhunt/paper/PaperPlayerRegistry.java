package xyz.qincai.manhunt.paper;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.PaperManhuntNG;
import xyz.qincai.manhunt.platform.PlayerRegistry;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PaperPlayerRegistry implements PlayerRegistry {
    private final Plugin plugin;
    private final Map<UUID, PlayerRole> playerRoles = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> hunterRespawns = new ConcurrentHashMap<>();

    public PaperPlayerRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reset() {
        playerRoles.clear();
        hunterRespawns.clear();
    }

    @Override
    public void setRole(UUID uuid, PlayerRole role) { playerRoles.put(uuid, role); }

    @Override
    public PlayerRole getRole(UUID uuid) { return playerRoles.getOrDefault(uuid, PlayerRole.SPECTATOR); }

    @Override
    public boolean isRunner(UUID uuid) { return playerRoles.get(uuid) == PlayerRole.RUNNER; }

    @Override
    public boolean isHunter(UUID uuid) { return playerRoles.get(uuid) == PlayerRole.HUNTER; }

    @Override
    public UUID getRunnerUuid() {
        for (Map.Entry<UUID, PlayerRole> e : playerRoles.entrySet()) {
            if (e.getValue() == PlayerRole.RUNNER) return e.getKey();
        }
        return null;
    }

    @Override
    public void addHunterRespawn(UUID uuid) { hunterRespawns.merge(uuid, 1, Integer::sum); }

    @Override
    public int getHunterRespawnCount(UUID uuid) { return hunterRespawns.getOrDefault(uuid, 0); }

    @Override
    public void clearHunterRespawns() { hunterRespawns.clear(); }

    @Override
    public void applyRoleToPlayer(UUID uuid, PlayerRole role) {
        PaperManhuntNG main = PaperManhuntNG.getInstance();
        Match match = main.getGameManager().getMatch();
        switch (role) {
            case RUNNER -> { match.setRunnerUuid(uuid); match.removeSpectator(uuid); setRole(uuid, PlayerRole.RUNNER); }
            case HUNTER -> { match.addHunter(uuid); match.removeSpectator(uuid); setRole(uuid, PlayerRole.HUNTER); }
            case SPECTATOR -> { match.addSpectator(uuid); setRole(uuid, PlayerRole.SPECTATOR); }
        }
    }

    @Override
    public void removePlayerFromGame(UUID uuid) {
        PaperManhuntNG main = PaperManhuntNG.getInstance();
        Match match = main.getGameManager().getMatch();
        match.getHunterUuids().remove(uuid);
        match.getSpectatorUuids().remove(uuid);
        if (match.getRunnerUuid() != null && match.getRunnerUuid().equals(uuid)) match.setRunnerUuid(null);
        playerRoles.remove(uuid);
        hunterRespawns.remove(uuid);
    }

    @Override
    public void eliminateRunner(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.setGameMode(GameMode.SPECTATOR);
            PaperManhuntNG.getInstance().getGameManager().getMatch().addSpectator(uuid);
        }
    }

    @Override
    public void eliminateHunter(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) player.setGameMode(GameMode.SPECTATOR);
    }

    @Override
    public int getAliveHunterCount() {
        PaperManhuntNG main = PaperManhuntNG.getInstance();
        Match match = main.getGameManager().getMatch();
        int count = 0;
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.getGameMode() != GameMode.SPECTATOR) count++;
        }
        return count;
    }
}
