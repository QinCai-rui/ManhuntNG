package xyz.qincai.manhunt.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.ui.GamePhase;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class GameManager {
    private final ManhuntNG plugin;
    private final Match match;
    private int countdownTaskId = -1;

    public GameManager(ManhuntNG plugin) {
        this.plugin = plugin;
        this.match = new Match();
    }

    public Match getMatch() {
        return match;
    }

    public void startGame() {
        startGame(null);
    }

    public void startGame(UUID ownerUuid) {
        if (match.getState() != GameState.WAITING) return;
        if (match.getRunnerUuid() == null) {
            plugin.getUiManager().broadcastMessage("\u00a7cNo runner selected!");
            return;
        }
        if (match.getHunterUuids().isEmpty()) {
            plugin.getUiManager().broadcastMessage("\u00a7cNo hunters selected!");
            return;
        }

        match.setOwnerUuid(ownerUuid);
        match.setState(GameState.COUNTDOWN);
        startCountdown();
    }

    private void startCountdown() {
        int initialCountdown = plugin.getConfigManager().getPreHuntCountdown();
        match.setState(GameState.COUNTDOWN);

        freezeAllPlayers();

        AtomicInteger countdown = new AtomicInteger(initialCountdown);

        countdownTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (match.getState() != GameState.COUNTDOWN) {
                Bukkit.getScheduler().cancelTask(countdownTaskId);
                return;
            }

            int current = countdown.get();
            if (current <= 0) {
                Bukkit.getScheduler().cancelTask(countdownTaskId);
                finishCountdown();
                return;
            }

            for (UUID uuid : match.getHunterUuids()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendTitle("\u00a7e" + current, "\u00a77Game starting in...", 0, 25, 0);
                }
            }
            if (match.getRunnerUuid() != null) {
                Player runner = Bukkit.getPlayer(match.getRunnerUuid());
                if (runner != null) {
                    runner.sendTitle("\u00a7e" + current, "\u00a77Game starting in...", 0, 25, 0);
                }
            }

            countdown.decrementAndGet();
        }, 0L, 20L).getTaskId();
    }

    private void finishCountdown() {
        plugin.getWorldManager().createGameWorlds();

        World gameWorld = match.getGameWorld();
        if (gameWorld == null) {
            plugin.getUiManager().broadcastMessage("\u00a7cFailed to create game world!");
            match.setState(GameState.WAITING);
            return;
        }

        teleportPlayersToWorlds();
        plugin.getFormationManager().teleportToFormation();

        match.setState(GameState.PRE_HUNT);
        freezeAllPlayers();

        plugin.getUiManager().sendTitle("\u00a76The Hunt Begins!", "\u00a77Damage a hunter to start");
        plugin.getUiManager().sendToAll("\u00a7ePre-Hunt phase! Runner must damage a hunter to start the hunt.");
    }

    private void teleportPlayersToWorlds() {
        World gameWorld = match.getGameWorld();
        if (gameWorld == null) return;

        Location spawn = gameWorld.getSpawnLocation();

        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) {
                runner.teleport(spawn);
            }
        }

        for (UUID uuid : match.getHunterUuids()) {
            Player hunter = Bukkit.getPlayer(uuid);
            if (hunter != null) {
                hunter.teleport(spawn);
            }
        }
    }

    public void startHunt() {
        if (match.getState() != GameState.PRE_HUNT) return;

        match.setState(GameState.RUNNING);
        match.setStartTime(System.currentTimeMillis());

        unfreezeAllPlayers();

        plugin.getTrackerManager().giveCompassToAll();
        plugin.getTrackerManager().startTracking();
        plugin.getUiManager().startUIUpdates();

        plugin.getUiManager().sendTitle("\u00a7cThe Hunt Has Begun!", "\u00a77Runner is on the loose!");
        plugin.getUiManager().sendToAll("\u00a7aThe hunt has started!");
    }

public void stopGame() {
        match.accumulatePausedTime();
        match.setState(GameState.FINISHED);
        match.setEndTime(System.currentTimeMillis());

        if (countdownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = -1;
        }

        plugin.getTrackerManager().stopTracking();
        plugin.getUiManager().stopUIUpdates();
        unfreezeAllPlayers();

        if (plugin.getConfigManager().isWorldResetAfterMatch()) {
            plugin.getWorldManager().deleteGameWorlds();
        }

        match.setState(GameState.WAITING);
        plugin.getPlayerManager().reset();
        plugin.getStatsManager().reset();
    }

    public void runnerWins() {
        match.setState(GameState.FINISHED);
        match.setEndTime(System.currentTimeMillis());
        match.setRunnerWins(true);
        plugin.getStatsManager().recordWin(true);

        plugin.getTrackerManager().stopTracking();
        plugin.getUiManager().stopUIUpdates();

        plugin.getUiManager().sendTitle("\u00a76Runner Wins!", "\u00a77The Ender Dragon has been defeated!");
        plugin.getUiManager().broadcastMessage("\u00a76\u00a7lRunner has won the game!");

        unfreezeAllPlayers();
        endGame();
    }

    public void huntersWin() {
        match.setState(GameState.FINISHED);
        match.setEndTime(System.currentTimeMillis());
        match.setRunnerWins(false);
        plugin.getStatsManager().recordWin(false);

        plugin.getTrackerManager().stopTracking();
        plugin.getUiManager().stopUIUpdates();

        plugin.getUiManager().sendTitle("\u00a7cHunters Win!", "\u00a77The Runner has been eliminated!");
        plugin.getUiManager().broadcastMessage("\u00a7c\u00a7lHunters have won the game!");

        unfreezeAllPlayers();
        endGame();
    }

    private void endGame() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getConfigManager().isWorldResetAfterMatch()) {
                plugin.getWorldManager().deleteGameWorlds();
            }
            match.setState(GameState.WAITING);
            plugin.getPlayerManager().reset();
        }, 200L);
    }

    public void freezeAllPlayers() {
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) {
                runner.setWalkSpeed(0f);
                runner.setFlySpeed(0f);
            }
        }

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setWalkSpeed(0f);
                player.setFlySpeed(0f);
            }
        }
    }

    public void unfreezeAllPlayers() {
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) {
                runner.setWalkSpeed(0.2f);
                runner.setFlySpeed(0.1f);
            }
        }

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.1f);
            }
        }
    }

    private void setAllPlayersInvulnerable(boolean invulnerable) {
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) runner.setInvulnerable(invulnerable);
        }
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.setInvulnerable(invulnerable);
        }
    }

    private void clearMobTargets() {
        World world = match.getGameWorld();
        if (world == null) return;
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (entity instanceof Mob mob) {
                if (mob.getTarget() instanceof Player) {
                    mob.setTarget(null);
                }
            }
        }
    }

    public boolean isGameActive() {
        return match.getState() == GameState.RUNNING || match.getState() == GameState.PRE_HUNT || match.getState() == GameState.PAUSED;
    }

    public boolean pauseGame(UUID ownerUuid) {
        if (match.getState() != GameState.RUNNING && match.getState() != GameState.PRE_HUNT) return false;
        if (!match.isOwner(ownerUuid)) {
            Player player = Bukkit.getPlayer(ownerUuid);
            if (player == null || !player.hasPermission("manhunt.admin")) return false;
        }

        match.setPrePauseState(match.getState());
        match.setState(GameState.PAUSED);
        match.setPausedAt(System.currentTimeMillis());

        freezeAllPlayers();
        plugin.getTrackerManager().stopTracking();

        if (match.getGameWorld() != null) {
            match.getGameWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }

        setAllPlayersInvulnerable(true);
        clearMobTargets();

        plugin.getUiManager().showPauseTitle();
        plugin.getUiManager().sendToAll("\u00a7eGame has been paused!");
        return true;
    }

    public boolean resumeGame(UUID ownerUuid) {
        if (match.getState() != GameState.PAUSED) return false;
        if (!match.isOwner(ownerUuid)) {
            Player player = Bukkit.getPlayer(ownerUuid);
            if (player == null || !player.hasPermission("manhunt.admin")) return false;
        }

        match.accumulatePausedTime();

        GameState previousState = match.getPrePauseState();
        match.setState(previousState);

        if (match.getGameWorld() != null) {
            match.getGameWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        }

        setAllPlayersInvulnerable(false);

        if (previousState == GameState.RUNNING) {
            unfreezeAllPlayers();
            plugin.getTrackerManager().giveCompassToAll();
            plugin.getTrackerManager().startTracking();
            plugin.getUiManager().startUIUpdates();
        }

        plugin.getUiManager().hidePauseTitle();
        plugin.getUiManager().sendTitle("\u00a7aGame Resumed", "\u00a77The game has been resumed");
        plugin.getUiManager().sendToAll("\u00a7aGame has been resumed!");
        return true;
    }

    public boolean isGamePaused() {
        return match.getState() == GameState.PAUSED;
    }
}
