package xyz.qincai.manhunt.world;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

public class WorldManager {
    private final ManhuntNG plugin;

    public WorldManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void createGameWorlds() {
        String worldName = "manhunt_" + System.currentTimeMillis();

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(true);
        World overworld = creator.createWorld();
        if (overworld == null) {
            plugin.getLogger().severe("Failed to create overworld!");
            return;
        }
        overworld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, true);
        overworld.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, true);
        overworld.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK, true);

        WorldCreator netherCreator = new WorldCreator(worldName + "_nether");
        netherCreator.environment(World.Environment.NETHER);
        World nether = netherCreator.createWorld();
        if (nether == null) {
            plugin.getLogger().severe("Failed to create nether world!");
            return;
        }

        WorldCreator endCreator = new WorldCreator(worldName + "_the_end");
        endCreator.environment(World.Environment.THE_END);
        World end = endCreator.createWorld();
        if (end == null) {
            plugin.getLogger().severe("Failed to create end world!");
            return;
        }

        Match match = plugin.getGameManager().getMatch();
        match.setGameWorld(overworld);
        match.setNetherWorld(nether);
        match.setEndWorld(end);

        overworld.setSpawnLocation(overworld.getSpawnLocation());
    }
}
