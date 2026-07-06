package xyz.qincai.manhunt.game;

import org.bukkit.World;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.Map;
import java.util.UUID;

public class Match {
    private GameState state = GameState.WAITING;
    private UUID runnerUuid;
    private final java.util.Set<UUID> hunterUuids = new java.util.concurrent.ConcurrentHashMap<UUID, Boolean>().newKeySet();
    private final java.util.Set<UUID> spectatorUuids = new java.util.concurrent.ConcurrentHashMap<UUID, Boolean>().newKeySet();
    private final java.util.Map<UUID, PlayerRole> previousRoles = new java.util.concurrent.ConcurrentHashMap<>();
    private long startTime;
    private long endTime;
    private World gameWorld;
    private World netherWorld;
    private World endWorld;
    private UUID winnerUuid;
    private boolean runnerWins;

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
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
        return (end - startTime) / 1000;
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

    public boolean isPlayer(UUID uuid) {
        return (runnerUuid != null && runnerUuid.equals(uuid)) || hunterUuids.contains(uuid);
    }

    public boolean isParticipant(UUID uuid) {
        return isPlayer(uuid) || spectatorUuids.contains(uuid);
    }
}
