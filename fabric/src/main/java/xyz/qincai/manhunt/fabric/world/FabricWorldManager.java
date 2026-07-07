package xyz.qincai.manhunt.fabric.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.qincai.manhunt.fabric.game.FabricGameManager;

import java.util.*;

public class FabricWorldManager {
    private final FabricGameManager gameManager;
    private final List<ServerWorld> gameWorlds = new ArrayList<>();

    public FabricWorldManager(FabricGameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void prepareWorlds() {
        MinecraftServer server = gameManager.getServer();
        gameWorlds.clear();

        ServerWorld overworld = server.getOverworld();
        ServerWorld nether = server.getWorld(World.NETHER);
        ServerWorld end = server.getWorld(World.END);

        if (overworld != null) gameWorlds.add(overworld);
        if (nether != null) gameWorlds.add(nether);
        if (end != null) gameWorlds.add(end);
    }

    public BlockPos getRunnerSpawn() {
        ServerWorld overworld = gameManager.getServer().getOverworld();
        BlockPos spawn = overworld.getSpawnPos();
        return spawn.add(50, 0, 0);
    }

    public BlockPos getHunterSpawn() {
        ServerWorld overworld = gameManager.getServer().getOverworld();
        BlockPos spawn = overworld.getSpawnPos();
        return spawn.add(-50, 0, 0);
    }

    public void cleanup() {
        gameWorlds.clear();
    }
}
