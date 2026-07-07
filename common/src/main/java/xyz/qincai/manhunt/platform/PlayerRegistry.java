package xyz.qincai.manhunt.platform;

import xyz.qincai.manhunt.player.PlayerRole;

import java.util.UUID;

public interface PlayerRegistry {
    void setRole(UUID uuid, PlayerRole role);
    PlayerRole getRole(UUID uuid);
    boolean isRunner(UUID uuid);
    boolean isHunter(UUID uuid);
    UUID getRunnerUuid();
    void addHunterRespawn(UUID uuid);
    int getHunterRespawnCount(UUID uuid);
    void clearHunterRespawns();
    void applyRoleToPlayer(UUID uuid, PlayerRole role);
    void removePlayerFromGame(UUID uuid);
    void eliminateRunner(UUID uuid);
    void eliminateHunter(UUID uuid);
    int getAliveHunterCount();
    void reset();
}
