package xyz.qincai.manhunt.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatisticsManager {
    private final Map<UUID, MutablePlayerStats> playerStats = new HashMap<>();
    private int runnerWins = 0;
    private int hunterWins = 0;

    public static class MutablePlayerStats {
        private int kills = 0;
        private int deaths = 0;
        private long timePlayed = 0;

        public int getKills() { return kills; }
        public int getDeaths() { return deaths; }
        public long getTimePlayed() { return timePlayed; }
        public void incrementKills() { kills++; }
        public void incrementDeaths() { deaths++; }
        public void addTimePlayed(long time) { timePlayed += time; }
    }

    public void reset() {
        playerStats.clear();
        runnerWins = 0;
        hunterWins = 0;
    }

    public void recordKill(UUID killer, UUID victim) {
        playerStats.computeIfAbsent(killer, k -> new MutablePlayerStats()).incrementKills();
        playerStats.computeIfAbsent(victim, k -> new MutablePlayerStats()).incrementDeaths();
    }

    public void recordDeath(UUID uuid) {
        playerStats.computeIfAbsent(uuid, k -> new MutablePlayerStats()).incrementDeaths();
    }

    public void recordWin(boolean runnerWon) {
        if (runnerWon) runnerWins++;
        else hunterWins++;
    }

    public int getRunnerWins() { return runnerWins; }
    public int getHunterWins() { return hunterWins; }
    public Map<UUID, MutablePlayerStats> getPlayerStats() { return playerStats; }
}
