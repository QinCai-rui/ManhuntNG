package xyz.qincai.manhunt.fabric.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

    private final Set<ServerPlayerEntity> runners = new HashSet<>();
    private final Set<ServerPlayerEntity> hunters = new HashSet<>();
    private final Set<ServerPlayerEntity> spectators = new HashSet<>();
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
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.literal("\u23f3 " + countdownSeconds + " seconds until game starts!").formatted(Formatting.YELLOW), false);
        }
        if (countdownSeconds <= 0) {
            startPreHunt();
        }
    }

    private void tickPreHunt() {
        preHuntSeconds--;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (preHuntSeconds % 10 == 0 || preHuntSeconds <= 5) {
                player.sendMessage(Text.literal("\u23f3 " + preHuntSeconds + " seconds until hunters release!").formatted(Formatting.RED), false);
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
        for (ServerPlayerEntity hunter : hunters) {
            Optional<ServerPlayerEntity> nearest = findNearestRunner(hunter);
            nearest.ifPresent(runner -> {
                updateCompass(hunter, runner);
            });
        }
    }

    private Optional<ServerPlayerEntity> findNearestRunner(ServerPlayerEntity hunter) {
        return runners.stream()
                .filter(r -> r.getWorld() == hunter.getWorld())
                .min(Comparator.comparingDouble(r -> r.squaredDistanceTo(hunter)));
    }

    private void updateCompass(ServerPlayerEntity hunter, ServerPlayerEntity target) {
        var stack = hunter.getInventory().selectedSlot == 0
                ? hunter.getInventory().getStack(0)
                : null;
        if (stack != null && stack.getItem() == net.minecraft.item.Items.COMPASS) {
            hunter.setSpawnPoint(
                    net.minecraft.world.World.OVERWORLD,
                    target.getBlockPos(),
                    0,
                    false
            );
        }
    }

    public void startGame(ServerPlayerEntity starter) {
        if (state != GameState.WAITING) return;

        List<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
        players.removeIf(p -> p.getUuid().equals(starter.getUuid()) || hasPermission(p, "manhunt.spectate"));

        if (players.isEmpty()) {
            starter.sendMessage(Text.literal("Not enough players to start!").formatted(Formatting.RED));
            return;
        }

        assignRoles(starter, players);
        state = GameState.COUNTDOWN;
        countdownSeconds = 30;

        broadcast(Text.literal("Manhunt game starting!").formatted(Formatting.GREEN));
    }

    private void assignRoles(ServerPlayerEntity starter, List<ServerPlayerEntity> players) {
        runners.clear();
        hunters.clear();
        spectators.clear();
        playerRoles.clear();

        List<ServerPlayerEntity> available = new ArrayList<>(players);
        Collections.shuffle(available);

        ServerPlayerEntity runner = available.remove(0);
        runners.add(runner);
        playerRoles.put(runner.getUuid(), PlayerRole.RUNNER);

        for (ServerPlayerEntity p : available) {
            hunters.add(p);
            playerRoles.put(p.getUuid(), PlayerRole.HUNTER);
        }

        starter.sendMessage(Text.literal("Runner: " + runner.getName().getString()).formatted(Formatting.AQUA));
        runner.sendMessage(Text.literal("You are the RUNNER! Run!").formatted(Formatting.GOLD));
        for (ServerPlayerEntity hunter : hunters) {
            hunter.sendMessage(Text.literal("You are a HUNTER! Hunt the runner!").formatted(Formatting.RED));
        }

        prepareWorlds();
    }

    private void prepareWorlds() {
        worldManager.prepareWorlds();
    }

    private void startPreHunt() {
        state = GameState.PRE_HUNT;
        preHuntSeconds = 60;
        broadcast(Text.literal("Runner has been released! Hunters will follow shortly.").formatted(Formatting.YELLOW));
    }

    private void startRunning() {
        state = GameState.RUNNING;
        broadcast(Text.literal("Hunters released! The hunt is on!").formatted(Formatting.RED));
    }

    public void pauseGame() {
        paused = true;
        broadcast(Text.literal("Game paused.").formatted(Formatting.YELLOW));
    }

    public void resumeGame() {
        paused = false;
        broadcast(Text.literal("Game resumed.").formatted(Formatting.GREEN));
    }

    public void endGame() {
        state = GameState.FINISHED;
        broadcast(Text.literal("Game over!").formatted(Formatting.GRAY));
        worldManager.cleanup();
    }

    public void onPlayerDeath(ServerPlayerEntity player) {
        PlayerRole role = playerRoles.get(player.getUuid());
        if (role == PlayerRole.RUNNER) {
            broadcast(Text.literal("Runner " + player.getName().getString() + " died! Hunters win!").formatted(Formatting.GREEN));
            state = GameState.FINISHED;
        } else if (role == PlayerRole.HUNTER) {
            hunters.remove(player);
            spectators.add(player);
            broadcast(Text.literal("Hunter " + player.getName().getString() + " eliminated!").formatted(Formatting.GRAY));
        }
    }

    public void onPlayerRespawn(ServerPlayerEntity player) {
        PlayerRole role = playerRoles.get(player.getUuid());
        if (role == PlayerRole.RUNNER) {
            player.teleport(worldManager.getRunnerSpawn(), 0, 0);
        } else if (role == PlayerRole.HUNTER) {
            player.teleport(worldManager.getHunterSpawn(), 0, 0);
        }
    }

    public void onPlayerJoin(ServerPlayerEntity player) {
        if (state == GameState.RUNNING) {
            player.sendMessage(Text.literal("A Manhunt game is in progress!").formatted(Formatting.YELLOW));
        }
    }

    public void onPlayerDisconnect(ServerPlayerEntity player) {
        PlayerRole role = playerRoles.get(player.getUuid());
        if (role == PlayerRole.RUNNER && state == GameState.RUNNING) {
            broadcast(Text.literal("Runner disconnected! Hunters win!").formatted(Formatting.GREEN));
            state = GameState.FINISHED;
        }
    }

    public void broadcast(Text message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(message, false);
        }
    }

    public boolean hasPermission(ServerPlayerEntity player, String permission) {
        return server.getPlayerManager().isOperator(player.getGameProfile());
    }

    public GameState getState() { return state; }
    public FabricPlayerManager getPlayerManager() { return playerManager; }
    public FabricWorldManager getWorldManager() { return worldManager; }
    public FabricUIManager getUiManager() { return uiManager; }
    public Set<ServerPlayerEntity> getRunners() { return runners; }
    public Set<ServerPlayerEntity> getHunters() { return hunters; }
    public Map<UUID, PlayerRole> getPlayerRoles() { return playerRoles; }
    public void shutdown() {
        if (state == GameState.RUNNING) endGame();
    }
}
