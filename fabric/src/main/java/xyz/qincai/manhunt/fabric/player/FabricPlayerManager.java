package xyz.qincai.manhunt.fabric.player;

import net.minecraft.server.network.ServerPlayerEntity;
import xyz.qincai.manhunt.player.PlayerRole;
import xyz.qincai.manhunt.fabric.game.FabricGameManager;

import java.util.*;

public class FabricPlayerManager {
    private final FabricGameManager gameManager;
    private final Map<UUID, Integer> killCount = new HashMap<>();
    private final Map<UUID, Integer> deathCount = new HashMap<>();
    private final Map<UUID, Integer> winCount = new HashMap<>();

    public FabricPlayerManager(FabricGameManager gameManager) {
        this.gameManager = gameManager;
    }

    public PlayerRole getRole(ServerPlayerEntity player) {
        return gameManager.getPlayerRoles().get(player.getUuid());
    }

    public boolean isRunner(ServerPlayerEntity player) {
        return getRole(player) == PlayerRole.RUNNER;
    }

    public boolean isHunter(ServerPlayerEntity player) {
        return getRole(player) == PlayerRole.HUNTER;
    }

    public boolean isSpectator(ServerPlayerEntity player) {
        return getRole(player) == PlayerRole.SPECTATOR;
    }

    public void recordKill(ServerPlayerEntity killer) {
        killCount.merge(killer.getUuid(), 1, Integer::sum);
    }

    public void recordDeath(ServerPlayerEntity victim) {
        deathCount.merge(victim.getUuid(), 1, Integer::sum);
    }

    public void recordWin(ServerPlayerEntity player) {
        winCount.merge(player.getUuid(), 1, Integer::sum);
    }

    public int getKills(ServerPlayerEntity player) {
        return killCount.getOrDefault(player.getUuid(), 0);
    }

    public int getDeaths(ServerPlayerEntity player) {
        return deathCount.getOrDefault(player.getUuid(), 0);
    }

    public int getWins(ServerPlayerEntity player) {
        return winCount.getOrDefault(player.getUuid(), 0);
    }
}
