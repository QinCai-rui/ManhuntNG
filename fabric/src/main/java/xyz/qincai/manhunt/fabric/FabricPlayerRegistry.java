package xyz.qincai.manhunt.fabric;

import xyz.qincai.manhunt.platform.PlayerRegistry;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FabricPlayerRegistry implements PlayerRegistry {
    private final Map<UUID, PlayerRole> playerRoles = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> hunterRespawns = new ConcurrentHashMap<>();

    @Override
    public void reset() { playerRoles.clear(); hunterRespawns.clear(); }

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

    @Override public void addHunterRespawn(UUID uuid) { hunterRespawns.merge(uuid, 1, Integer::sum); }
    @Override public int getHunterRespawnCount(UUID uuid) { return hunterRespawns.getOrDefault(uuid, 0); }
    @Override public void clearHunterRespawns() { hunterRespawns.clear(); }

    @Override
    public void applyRoleToPlayer(UUID uuid, PlayerRole role) {
        setRole(uuid, role);
    }

    @Override
    public void removePlayerFromGame(UUID uuid) {
        playerRoles.remove(uuid);
        hunterRespawns.remove(uuid);
    }

    @Override public void eliminateRunner(UUID uuid) {}
    @Override public void eliminateHunter(UUID uuid) {}
    @Override public int getAliveHunterCount() { return 0; }
}
