package xyz.qincai.manhunt.game;

import org.bukkit.World;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.Map;
import java.util.UUID;

public class Match {
    private GameState state = GameState.WAITING;
    private StartMode startMode = StartMode.DREAMSTART;
    private UUID runnerUuid;
    private final java.util.Set<UUID> hunterUuids = new java.util.concurrent.ConcurrentHashMap<UUID, Boolean>().newKeySet();
    private final java.util.Set<UUID> spectatorUuids = new java.util.concurrent.ConcurrentHashMap<UUID, Boolean>().newKeySet();
    private final java.util.Map<UUID, PlayerRole> previousRoles = new java.util.concurrent.ConcurrentHashMap<>();
    private UUID ownerUuid;
    private long startTime;
    private long endTime;
    private long pausedAt;
    private long totalPausedDuration;
    private GameState prePauseState;
    private World gameWorld;
    private World netherWorld;
    private World endWorld;
    private UUID winnerUuid;
    private boolean runnerWins;
    private boolean strongholdDiscovered;
    private boolean fortressDiscovered;
    private boolean blazeRodObtained;
    private boolean bastionDiscovered;
    private int headstartRemaining = -1; // remaining headstart seconds, -1 = not set
    private Long seed;
    private String worldName;

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public StartMode getStartMode() {
        return startMode;
    }

    public void setStartMode(StartMode startMode) {
        this.startMode = startMode;
    }

    public UUID getRunnerUuid() {
        return runnerUuid;
    }

    public void setRunnerUuid(UUID runnerUuid) {
        this.runnerUuid = runnerUuid;
    }

    public java.util.Set<UUID> getHunterUuids() {
        return hunterUuids;
    }

    public void addHunter(UUID uuid) {
        hunterUuids.add(uuid);
    }

    public void removeHunter(UUID uuid) {
        hunterUuids.remove(uuid);
    }

    public boolean isHunter(UUID uuid) {
        return hunterUuids.contains(uuid);
    }

    public java.util.Set<UUID> getSpectatorUuids() {
        return spectatorUuids;
    }

    public void addSpectator(UUID uuid) {
        spectatorUuids.add(uuid);
    }

    public void removeSpectator(UUID uuid) {
        spectatorUuids.remove(uuid);
    }

    public boolean isSpectator(UUID uuid) {
        return spectatorUuids.contains(uuid);
    }

    public java.util.Map<UUID, PlayerRole> getPreviousRoles() {
        return previousRoles;
    }

    public void storePreviousRole(UUID uuid, PlayerRole role) {
        previousRoles.put(uuid, role);
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getElapsedSeconds() {
        if (startTime == 0) return 0;
        long end = endTime != 0 ? endTime : System.currentTimeMillis();
        long paused = pausedAt != 0 ? System.currentTimeMillis() - pausedAt : 0;
        return (end - startTime - totalPausedDuration - paused) / 1000;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public long getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(long pausedAt) {
        this.pausedAt = pausedAt;
    }

    public long getTotalPausedDuration() {
        return totalPausedDuration;
    }

    public void setTotalPausedDuration(long totalPausedDuration) {
        this.totalPausedDuration = totalPausedDuration;
    }

    public GameState getPrePauseState() {
        return prePauseState;
    }

    public void setPrePauseState(GameState prePauseState) {
        this.prePauseState = prePauseState;
    }

    public void accumulatePausedTime() {
        if (pausedAt != 0) {
            totalPausedDuration += System.currentTimeMillis() - pausedAt;
            pausedAt = 0;
        }
    }

    public boolean isOwner(UUID uuid) {
        return ownerUuid != null && ownerUuid.equals(uuid);
    }

    public World getGameWorld() {
        return gameWorld;
    }

    public void setGameWorld(World gameWorld) {
        this.gameWorld = gameWorld;
    }

    public World getNetherWorld() {
        return netherWorld;
    }

    public void setNetherWorld(World netherWorld) {
        this.netherWorld = netherWorld;
    }

    public World getEndWorld() {
        return endWorld;
    }

    public void setEndWorld(World endWorld) {
        this.endWorld = endWorld;
    }

    public UUID getWinnerUuid() {
        return winnerUuid;
    }

    public void setWinnerUuid(UUID winnerUuid) {
        this.winnerUuid = winnerUuid;
    }

    public boolean isRunnerWins() {
        return runnerWins;
    }

    public void setRunnerWins(boolean runnerWins) {
        this.runnerWins = runnerWins;
    }

    public boolean isStrongholdDiscovered() {
        return strongholdDiscovered;
    }

    public void setStrongholdDiscovered(boolean strongholdDiscovered) {
        this.strongholdDiscovered = strongholdDiscovered;
    }

    public boolean isFortressDiscovered() {
        return fortressDiscovered;
    }

    public void setFortressDiscovered(boolean fortressDiscovered) {
        this.fortressDiscovered = fortressDiscovered;
    }

    public boolean isBlazeRodObtained() {
        return blazeRodObtained;
    }

    public void setBlazeRodObtained(boolean blazeRodObtained) {
        this.blazeRodObtained = blazeRodObtained;
    }

    public boolean isBastionDiscovered() {
        return bastionDiscovered;
    }

    public void setBastionDiscovered(boolean bastionDiscovered) {
        this.bastionDiscovered = bastionDiscovered;
    }

    public int getHeadstartRemaining() {
        return headstartRemaining;
    }

    public void setHeadstartRemaining(int headstartRemaining) {
        this.headstartRemaining = headstartRemaining;
    }

    public boolean isPlayer(UUID uuid) {
        return (runnerUuid != null && runnerUuid.equals(uuid)) || hunterUuids.contains(uuid);
    }

    public boolean isParticipant(UUID uuid) {
        return isPlayer(uuid) || spectatorUuids.contains(uuid);
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public boolean isUsingExistingWorld() {
        return worldName != null && !worldName.isEmpty();
    }
}
