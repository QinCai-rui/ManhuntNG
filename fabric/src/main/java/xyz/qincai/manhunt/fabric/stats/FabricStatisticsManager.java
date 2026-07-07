package xyz.qincai.manhunt.fabric.stats;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FabricStatisticsManager {
    private final Map<UUID, PlayerStats> stats = new ConcurrentHashMap<>();

    public record PlayerStats(
            int kills,
            int deaths,
            int wins,
            int gamesPlayed
    ) {}

    public void recordKill(UUID playerId) {
        stats.merge(playerId, new PlayerStats(1, 0, 0, 0), (old, val) ->
                new PlayerStats(old.kills + 1, old.deaths, old.wins, old.gamesPlayed));
    }

    public void recordDeath(UUID playerId) {
        stats.merge(playerId, new PlayerStats(0, 1, 0, 0), (old, val) ->
                new PlayerStats(old.kills, old.deaths + 1, old.wins, old.gamesPlayed));
    }

    public void recordWin(UUID playerId) {
        stats.merge(playerId, new PlayerStats(0, 0, 1, 0), (old, val) ->
                new PlayerStats(old.kills, old.deaths, old.wins + 1, old.gamesPlayed));
    }

    public void recordGamePlayed(UUID playerId) {
        stats.merge(playerId, new PlayerStats(0, 0, 0, 1), (old, val) ->
                new PlayerStats(old.kills, old.deaths, old.wins, old.gamesPlayed + 1));
    }

    public PlayerStats getStats(UUID playerId) {
        return stats.getOrDefault(playerId, new PlayerStats(0, 0, 0, 0));
    }
}
