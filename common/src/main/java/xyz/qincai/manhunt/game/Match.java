package xyz.qincai.manhunt.game;

import xyz.qincai.manhunt.player.PlayerRole;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Match {
    private GameState state = GameState.WAITING;
    private UUID runnerUuid;
    private final Set<UUID> hunterUuids = ConcurrentHashMap.newKeySet();
    private final Set<UUID> spectatorUuids = ConcurrentHashMap.newKeySet();
    private final Map<UUID, PlayerRole> previousRoles = new ConcurrentHashMap<>();
    private UUID ownerUuid;
    private long startTime;
    private long endTime;
    private long pausedAt;
    private long totalPausedDuration;
    private GameState prePauseState;
    private String gameWorldName;
    private String netherWorldName;
    private String endWorldName;
    private UUID winnerUuid;
    private boolean runnerWins;
    private boolean strongholdDiscovered;
    private boolean fortressDiscovered;
    private boolean blazeRodObtained;
    private boolean bastionDiscovered;
    private Long seed;
    private String worldName;

    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }

    public UUID getRunnerUuid() { return runnerUuid; }
    public void setRunnerUuid(UUID runnerUuid) { this.runnerUuid = runnerUuid; }

    public Set<UUID> getHunterUuids() { return hunterUuids; }
    public void addHunter(UUID uuid) { hunterUuids.add(uuid); }
    public void removeHunter(UUID uuid) { hunterUuids.remove(uuid); }
    public boolean isHunter(UUID uuid) { return hunterUuids.contains(uuid); }

    public Set<UUID> getSpectatorUuids() { return spectatorUuids; }
    public void addSpectator(UUID uuid) { spectatorUuids.add(uuid); }
    public void removeSpectator(UUID uuid) { spectatorUuids.remove(uuid); }
    public boolean isSpectator(UUID uuid) { return spectatorUuids.contains(uuid); }

    public Map<UUID, PlayerRole> getPreviousRoles() { return previousRoles; }
    public void storePreviousRole(UUID uuid, PlayerRole role) { previousRoles.put(uuid, role); }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public long getElapsedSeconds() {
        if (startTime == 0) return 0;
        long end = endTime != 0 ? endTime : System.currentTimeMillis();
        long paused = pausedAt != 0 ? System.currentTimeMillis() - pausedAt : 0;
        return (end - startTime - totalPausedDuration - paused) / 1000;
    }

    public UUID getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(UUID ownerUuid) { this.ownerUuid = ownerUuid; }

    public long getPausedAt() { return pausedAt; }
    public void setPausedAt(long pausedAt) { this.pausedAt = pausedAt; }

    public long getTotalPausedDuration() { return totalPausedDuration; }
    public void setTotalPausedDuration(long totalPausedDuration) { this.totalPausedDuration = totalPausedDuration; }

    public GameState getPrePauseState() { return prePauseState; }
    public void setPrePauseState(GameState prePauseState) { this.prePauseState = prePauseState; }

    public void accumulatePausedTime() {
        if (pausedAt != 0) {
            totalPausedDuration += System.currentTimeMillis() - pausedAt;
            pausedAt = 0;
        }
    }

    public boolean isOwner(UUID uuid) { return ownerUuid != null && ownerUuid.equals(uuid); }

    public String getGameWorldName() { return gameWorldName; }
    public void setGameWorldName(String worldName) { this.gameWorldName = worldName; }

    public String getNetherWorldName() { return netherWorldName; }
    public void setNetherWorldName(String worldName) { this.netherWorldName = worldName; }

    public String getEndWorldName() { return endWorldName; }
    public void setEndWorldName(String worldName) { this.endWorldName = worldName; }

    public UUID getWinnerUuid() { return winnerUuid; }
    public void setWinnerUuid(UUID winnerUuid) { this.winnerUuid = winnerUuid; }

    public boolean isRunnerWins() { return runnerWins; }
    public void setRunnerWins(boolean runnerWins) { this.runnerWins = runnerWins; }

    public boolean isStrongholdDiscovered() { return strongholdDiscovered; }
    public void setStrongholdDiscovered(boolean strongholdDiscovered) { this.strongholdDiscovered = strongholdDiscovered; }

    public boolean isFortressDiscovered() { return fortressDiscovered; }
    public void setFortressDiscovered(boolean fortressDiscovered) { this.fortressDiscovered = fortressDiscovered; }

    public boolean isBlazeRodObtained() { return blazeRodObtained; }
    public void setBlazeRodObtained(boolean blazeRodObtained) { this.blazeRodObtained = blazeRodObtained; }

    public boolean isBastionDiscovered() { return bastionDiscovered; }
    public void setBastionDiscovered(boolean bastionDiscovered) { this.bastionDiscovered = bastionDiscovered; }

    public boolean isPlayer(UUID uuid) {
        return (runnerUuid != null && runnerUuid.equals(uuid)) || hunterUuids.contains(uuid);
    }

    public boolean isParticipant(UUID uuid) {
        return isPlayer(uuid) || spectatorUuids.contains(uuid);
    }

    public Long getSeed() { return seed; }
    public void setSeed(Long seed) { this.seed = seed; }

    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    public boolean isUsingExistingWorld() { return worldName != null && !worldName.isEmpty(); }
}
