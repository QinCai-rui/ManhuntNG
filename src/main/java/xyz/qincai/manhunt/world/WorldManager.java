package xyz.qincai.manhunt.world;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

import java.io.File;
import java.util.UUID;

public class WorldManager {
    private static final String LOBBY_WORLD_NAME = "manhunt_lobby";
    private static final String OVERWORLD_NAME = "world";
    private static final String NETHER_NAME = "world_nether";
    private static final String END_NAME = "world_the_end";
    private final ManhuntNG plugin;

    public WorldManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    // ---- Lobby ----

    public World getLobbyWorld() {
        return Bukkit.getWorld(LOBBY_WORLD_NAME);
    }

    public World createLobbyWorld() {
        World lobby = Bukkit.getWorld(LOBBY_WORLD_NAME);
        if (lobby != null) return lobby;

        plugin.getLogger().info("Creating manhunt_lobby world...");
        WorldCreator creator = new WorldCreator(LOBBY_WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        lobby = creator.createWorld();
        if (lobby != null) {
            lobby.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
            lobby.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
            lobby.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK, false);
            lobby.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
            lobby.setGameRule(org.bukkit.GameRule.DO_TILE_DROPS, false);
            lobby.setGameRule(org.bukkit.GameRule.DO_ENTITY_DROPS, false);
            lobby.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, false);
            lobby.setGameRule(org.bukkit.GameRule.DO_IMMEDIATE_RESPAWN, true);
            lobby.setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, false);
            plugin.getLogger().info("manhunt_lobby world created.");
        } else {
            plugin.getLogger().severe("Failed to create manhunt_lobby world!");
        }
        return lobby;
    }

    public void teleportToLobby(Player player) {
        World lobby = getLobbyWorld();
        if (lobby == null) {
            lobby = createLobbyWorld();
        }
        if (lobby != null) {
            player.teleport(lobby.getSpawnLocation());
            player.setGameMode(GameMode.ADVENTURE);
        }
    }

    public void teleportToLobby(Match match) {
        World lobby = getLobbyWorld();
        if (lobby == null) {
            lobby = createLobbyWorld();
        }
        if (lobby == null) return;

        org.bukkit.Location spawn = lobby.getSpawnLocation();

        for (UUID uuid : match.getRunnerUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(spawn);
                player.setGameMode(GameMode.ADVENTURE);
            }
        }
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(spawn);
                player.setGameMode(GameMode.ADVENTURE);
            }
        }
        for (UUID uuid : match.getSpectatorUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(spawn);
                player.setGameMode(GameMode.ADVENTURE);
            }
        }
    }

    // ---- Game worlds (vanilla names) ----

    public void deleteAndGenerateWorlds(Match match) {
        Long seed = match.getSeed();

        deleteWorldFolder(OVERWORLD_NAME);
        deleteWorldFolder(NETHER_NAME);
        deleteWorldFolder(END_NAME);

        World overworld = generateWorld(OVERWORLD_NAME, World.Environment.NORMAL, seed);
        if (overworld == null) {
            plugin.getLogger().severe("Failed to create overworld!");
            return;
        }
        overworld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, true);
        overworld.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, true);
        overworld.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK, true);

        World nether = generateWorld(NETHER_NAME, World.Environment.NETHER, seed);
        if (nether == null) {
            plugin.getLogger().severe("Failed to create nether world!");
            return;
        }

        World end = generateWorld(END_NAME, World.Environment.THE_END, seed);
        if (end == null) {
            plugin.getLogger().severe("Failed to create end world!");
            return;
        }

        match.setGameWorld(overworld);
        match.setNetherWorld(nether);
        match.setEndWorld(end);
    }

    private World generateWorld(String name, World.Environment environment, Long seed) {
        plugin.getLogger().info("Generating world: " + name);
        WorldCreator creator = new WorldCreator(name);
        creator.environment(environment);
        creator.generateStructures(true);
        if (seed != null) {
            creator.seed(seed);
        }
        return creator.createWorld();
    }

    private void deleteWorldFolder(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            // Teleport players to the configured lobby world before unloading
            World lobby = getLobbyWorld();
            if (lobby == null) {
                lobby = createLobbyWorld();
            }
            if (lobby != null) {
                org.bukkit.Location lobbySpawn = lobby.getSpawnLocation();
                for (Player player : world.getPlayers()) {
                    player.teleport(lobbySpawn);
                }
            }

            // Attempt to unload the world and return early if it fails
            boolean unloaded = Bukkit.unloadWorld(world, false);
            if (!unloaded) {
                plugin.getLogger().warning("Failed to unload world: " + worldName);
                return;
            }
        }
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            plugin.getLogger().info("Deleting world folder: " + worldName);
            deleteFolder(worldFolder);
        }
    }

    private void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }
        folder.delete();
    }
}
