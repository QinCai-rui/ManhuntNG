package xyz.qincai.manhunt.fabric.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.player.PlayerRole;
import xyz.qincai.manhunt.fabric.ManhuntMod;
import xyz.qincai.manhunt.fabric.player.FabricPlayerManager;
import xyz.qincai.manhunt.fabric.ui.FabricUIManager;
import xyz.qincai.manhunt.fabric.world.FabricWorldManager;

import java.util.*;

public class FabricGameManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ManhuntGame");

    private final MinecraftServer server;
    private final FabricPlayerManager playerManager;
    private final FabricWorldManager worldManager;
    private final FabricUIManager uiManager;

    private GameState state = GameState.WAITING;
    private int countdownSeconds = 30;
    private int preHuntSeconds = 60;
    private int gameTicks = 0;
    private int elapsedSeconds = 0;

    private final Set<ServerPlayer> runners = new HashSet<>();
    private final Set<ServerPlayer> hunters = new HashSet<>();
    private final Set<ServerPlayer> spectators = new HashSet<>();
    private final Map<UUID, PlayerRole> playerRoles = new HashMap<>();

    private boolean paused = false;
    private boolean trackingEnabled = true;

    public FabricGameManager(MinecraftServer server) {
        this.server = server;
        this.playerManager = new FabricPlayerManager(this);
        this.worldManager = new FabricWorldManager(this);
        this.uiManager = new FabricUIManager(this);
    }

    public void tick() {
        if (paused || state == GameState.WAITING || state == GameState.FINISHED) return;

        gameTicks++;
        if (gameTicks % 20 != 0) return;

        elapsedSeconds++;

        switch (state) {
            case COUNTDOWN -> tickCountdown();
            case PRE_HUNT -> tickPreHunt();
            case RUNNING -> tickRunning();
        }
    }

    private void tickCountdown() {
        countdownSeconds--;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal("\u23f3 " + countdownSeconds + " seconds until game starts!").withStyle(ChatFormatting.YELLOW));
        }
        if (countdownSeconds <= 0) {
            startPreHunt();
        }
    }

    private void tickPreHunt() {
        preHuntSeconds--;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (preHuntSeconds % 10 == 0 || preHuntSeconds <= 5) {
                player.sendSystemMessage(Component.literal("\u23f3 " + preHuntSeconds + " seconds until hunters release!").withStyle(ChatFormatting.RED));
            }
        }
        if (preHuntSeconds <= 0) {
            startRunning();
        }
    }

    private void tickRunning() {
        uiManager.update();
        updateTracking();
    }

    private void updateTracking() {
        if (!trackingEnabled) return;
        for (ServerPlayer hunter : hunters) {
            Optional<ServerPlayer> nearest = findNearestRunner(hunter);
            nearest.ifPresent(runner -> {
                updateCompass(hunter, runner);
            });
        }
    }

    private Optional<ServerPlayer> findNearestRunner(ServerPlayer hunter) {
        return runners.stream()
                .filter(r -> r.level() == hunter.level())
                .min(Comparator.comparingDouble(r -> r.distanceToSqr(hunter)));
    }

    private void updateCompass(ServerPlayer hunter, ServerPlayer target) {
        var stack = hunter.getInventory().selected == 0
                ? hunter.getInventory().getItem(0)
                : null;
        if (stack != null && stack.getItem() == Items.COMPASS) {
            hunter.setRespawnPosition(
                    Level.OVERWORLD,
                    target.blockPosition(),
                    0,
                    false
            );
        }
    }

    public void startGame(ServerPlayer starter) {
        if (state != GameState.WAITING) return;

        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        players.removeIf(p -> p.getUUID().equals(starter.getUUID()) || hasPermission(p, "manhunt.spectate"));

        if (players.isEmpty()) {
            starter.sendSystemMessage(Component.literal("Not enough players to start!").withStyle(ChatFormatting.RED));
            return;
        }

        assignRoles(starter, players);
        state = GameState.COUNTDOWN;
        countdownSeconds = 30;

        broadcast(Component.literal("Manhunt game starting!").withStyle(ChatFormatting.GREEN));
    }

    private void assignRoles(ServerPlayer starter, List<ServerPlayer> players) {
        runners.clear();
        hunters.clear();
        spectators.clear();
        playerRoles.clear();

        List<ServerPlayer> available = new ArrayList<>(players);
        Collections.shuffle(available);

        ServerPlayer runner = available.remove(0);
        runners.add(runner);
        playerRoles.put(runner.getUUID(), PlayerRole.RUNNER);

        for (ServerPlayer p : available) {
            hunters.add(p);
            playerRoles.put(p.getUUID(), PlayerRole.HUNTER);
        }

        starter.sendSystemMessage(Component.literal("Runner: " + runner.getName().getString()).withStyle(ChatFormatting.AQUA));
        runner.sendSystemMessage(Component.literal("You are the RUNNER! Run!").withStyle(ChatFormatting.GOLD));
        for (ServerPlayer hunter : hunters) {
            hunter.sendSystemMessage(Component.literal("You are a HUNTER! Hunt the runner!").withStyle(ChatFormatting.RED));
        }

        prepareWorlds();
    }

    private void prepareWorlds() {
        worldManager.prepareWorlds();
    }

    private void startPreHunt() {
        state = GameState.PRE_HUNT;
        preHuntSeconds = 60;
        broadcast(Component.literal("Runner has been released! Hunters will follow shortly.").withStyle(ChatFormatting.YELLOW));
    }

    private void startRunning() {
        state = GameState.RUNNING;
        broadcast(Component.literal("Hunters released! The hunt is on!").withStyle(ChatFormatting.RED));
    }

    public void pauseGame() {
        paused = true;
        broadcast(Component.literal("Game paused.").withStyle(ChatFormatting.YELLOW));
    }

    public void resumeGame() {
        paused = false;
        broadcast(Component.literal("Game resumed.").withStyle(ChatFormatting.GREEN));
    }

    public void endGame() {
        state = GameState.FINISHED;
        broadcast(Component.literal("Game over!").withStyle(ChatFormatting.GRAY));
        worldManager.cleanup();
    }

    public void onPlayerDeath(ServerPlayer player) {
        PlayerRole role = playerRoles.get(player.getUUID());
        if (role == PlayerRole.RUNNER) {
            broadcast(Component.literal("Runner " + player.getName().getString() + " died! Hunters win!").withStyle(ChatFormatting.GREEN));
            state = GameState.FINISHED;
        } else if (role == PlayerRole.HUNTER) {
            hunters.remove(player);
            spectators.add(player);
            broadcast(Component.literal("Hunter " + player.getName().getString() + " eliminated!").withStyle(ChatFormatting.GRAY));
        }
    }

    public void onPlayerRespawn(ServerPlayer player) {
        PlayerRole role = playerRoles.get(player.getUUID());
        if (role == PlayerRole.RUNNER) {
            player.teleportTo(worldManager.getRunnerSpawn(), 0, 0);
        } else if (role == PlayerRole.HUNTER) {
            player.teleportTo(worldManager.getHunterSpawn(), 0, 0);
        }
    }

    public void onPlayerJoin(ServerPlayer player) {
        if (state == GameState.RUNNING) {
            player.sendSystemMessage(Component.literal("A Manhunt game is in progress!").withStyle(ChatFormatting.YELLOW));
        }
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        PlayerRole role = playerRoles.get(player.getUUID());
        if (role == PlayerRole.RUNNER && state == GameState.RUNNING) {
            broadcast(Component.literal("Runner disconnected! Hunters win!").withStyle(ChatFormatting.GREEN));
            state = GameState.FINISHED;
        }
    }

    public void broadcast(Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }

    public boolean hasPermission(ServerPlayer player, String permission) {
        return server.getPlayerList().isOp(player.getGameProfile());
    }

    public GameState getState() { return state; }
    public FabricPlayerManager getPlayerManager() { return playerManager; }
    public FabricWorldManager getWorldManager() { return worldManager; }
    public FabricUIManager getUiManager() { return uiManager; }
    public Set<ServerPlayer> getRunners() { return runners; }
    public Set<ServerPlayer> getHunters() { return hunters; }
    public Map<UUID, PlayerRole> getPlayerRoles() { return playerRoles; }
    public MinecraftServer getServer() { return server; }
    public void shutdown() {
        if (state == GameState.RUNNING) endGame();
    }
}
