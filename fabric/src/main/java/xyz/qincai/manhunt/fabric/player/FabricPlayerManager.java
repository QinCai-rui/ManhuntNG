package xyz.qincai.manhunt.fabric.player;

import net.minecraft.server.level.ServerPlayer;
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

    public PlayerRole getRole(ServerPlayer player) {
        return gameManager.getPlayerRoles().get(player.getUUID());
    }

    public boolean isRunner(ServerPlayer player) {
        return getRole(player) == PlayerRole.RUNNER;
    }

    public boolean isHunter(ServerPlayer player) {
        return getRole(player) == PlayerRole.HUNTER;
    }

    public boolean isSpectator(ServerPlayer player) {
        return getRole(player) == PlayerRole.SPECTATOR;
    }

    public void recordKill(ServerPlayer killer) {
        killCount.merge(killer.getUUID(), 1, Integer::sum);
    }

    public void recordDeath(ServerPlayer victim) {
        deathCount.merge(victim.getUUID(), 1, Integer::sum);
    }

    public void recordWin(ServerPlayer player) {
        winCount.merge(player.getUUID(), 1, Integer::sum);
    }

    public int getKills(ServerPlayer player) {
        return killCount.getOrDefault(player.getUUID(), 0);
    }

    public int getDeaths(ServerPlayer player) {
        return deathCount.getOrDefault(player.getUUID(), 0);
    }

    public int getWins(ServerPlayer player) {
        return winCount.getOrDefault(player.getUUID(), 0);
    }
}
