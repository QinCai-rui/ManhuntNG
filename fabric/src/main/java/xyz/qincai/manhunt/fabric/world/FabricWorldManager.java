package xyz.qincai.manhunt.fabric.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import xyz.qincai.manhunt.fabric.game.FabricGameManager;

import java.util.*;

public class FabricWorldManager {
    private final FabricGameManager gameManager;
    private final List<ServerLevel> gameWorlds = new ArrayList<>();

    public FabricWorldManager(FabricGameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void prepareWorlds() {
        MinecraftServer server = gameManager.getServer();
        gameWorlds.clear();

        ServerLevel overworld = server.overworld();
        ServerLevel nether = server.getLevel(Level.NETHER);
        ServerLevel end = server.getLevel(Level.END);

        if (overworld != null) gameWorlds.add(overworld);
        if (nether != null) gameWorlds.add(nether);
        if (end != null) gameWorlds.add(end);
    }

    public BlockPos getRunnerSpawn() {
        ServerLevel overworld = gameManager.getServer().overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        return spawn.offset(50, 0, 0);
    }

    public BlockPos getHunterSpawn() {
        ServerLevel overworld = gameManager.getServer().overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        return spawn.offset(-50, 0, 0);
    }

    public void cleanup() {
        gameWorlds.clear();
    }
}
