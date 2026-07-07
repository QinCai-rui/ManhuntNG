package xyz.qincai.manhunt.game;

import xyz.qincai.manhunt.platform.ManhuntPlatform;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class GameManager {
    private final ManhuntPlatform platform;
    private final Match match;
    private int countdownTaskId = -1;
    private boolean forceStart;

    public GameManager(ManhuntPlatform platform) {
        this.platform = platform;
        this.match = new Match();
    }

    public Match getMatch() { return match; }

    public ManhuntPlatform getPlatform() { return platform; }

    private void resetMatchTiming() {
        match.setEndTime(0);
        match.setPausedAt(0);
        match.setTotalPausedDuration(0);
        match.setStartTime(0);
    }

    public void startGame() { startGame(null); }

    public void startGame(UUID ownerUuid) {
        if (match.getState() != GameState.WAITING) return;
        if (match.getRunnerUuid() == null) {
            platform.getUIFacade().broadcastMessage("\u00a7cNo runner selected!");
            return;
        }
        if (match.getHunterUuids().isEmpty()) {
            platform.getUIFacade().broadcastMessage("\u00a7cNo hunters selected!");
            return;
        }
        match.setOwnerUuid(ownerUuid);
        match.setState(GameState.COUNTDOWN);
        startCountdown();
    }

    public void startGameForce(UUID ownerUuid) {
        if (match.getState() != GameState.WAITING) return;
        match.setOwnerUuid(ownerUuid);
        forceStart = true;
        match.setState(GameState.COUNTDOWN);
        startCountdown();
    }

    private void startCountdown() {
        int initialCountdown = platform.getConfigProvider().getPreHuntCountdown();
        match.setState(GameState.COUNTDOWN);
        platform.getWorldProvider().freezeAllPlayers(match);
        AtomicInteger countdown = new AtomicInteger(initialCountdown);

        countdownTaskId = platform.getScheduler().runTaskTimer(() -> {
            if (match.getState() != GameState.COUNTDOWN) {
                platform.getScheduler().cancelTask(countdownTaskId);
                return;
            }
            int current = countdown.get();
            if (current <= 0) {
                platform.getScheduler().cancelTask(countdownTaskId);
                finishCountdown();
                return;
            }
            String title = "\u00a7e" + current;
            String subtitle = "\u00a77Game starting in...";
            for (UUID uuid : match.getHunterUuids()) {
                platform.getWorldProvider().sendTitle(uuid, title, subtitle);
            }
            if (match.getRunnerUuid() != null) {
                platform.getWorldProvider().sendTitle(match.getRunnerUuid(), title, subtitle);
            }
            countdown.decrementAndGet();
        }, 0L, 20L);
    }

    private void finishCountdown() {
        platform.getWorldProvider().createGameWorlds(match);
        String gameWorldName = match.getGameWorldName();
        if (gameWorldName == null) {
            platform.getUIFacade().broadcastMessage("\u00a7cFailed to create game world!");
            match.setState(GameState.WAITING);
            return;
        }
        platform.getWorldProvider().clearPlayerState(match);
        platform.getWorldProvider().teleportToFormation(match);
        platform.getWorldProvider().setAllPlayersSurvival(match);
        platform.getWorldProvider().healAllPlayers(match);

        match.setStrongholdDiscovered(false);
        match.setFortressDiscovered(false);
        match.setBlazeRodObtained(false);
        match.setBastionDiscovered(false);
        resetMatchTiming();

        if (forceStart) {
            forceStart = false;
            match.setState(GameState.RUNNING);
            match.setStartTime(System.currentTimeMillis());
            platform.getWorldProvider().unfreezeAllPlayers(match);
            if (match.getRunnerUuid() != null) {
                platform.getWorldProvider().setInvulnerable(match.getRunnerUuid(), false);
            }
            platform.getUIFacade().sendTitle("\u00a7cThe Hunt Has Begun!", "\u00a77Runner is on the loose!");
            platform.getUIFacade().sendToAll("\u00a7aThe hunt has started! (Force started)");
        } else {
            match.setState(GameState.PRE_HUNT);
            platform.getWorldProvider().unfreezeHorizontalAllPlayers(match);
            platform.getUIFacade().sendTitle("\u00a76Pre-Hunt", "\u00a77Damage a hunter to start the hunt");
            platform.getUIFacade().sendToAll("\u00a7ePre-Hunt phase! Runner must damage a hunter to start the hunt.");
        }
    }

    public void startHunt() {
        if (match.getState() != GameState.PRE_HUNT) return;
        match.setState(GameState.RUNNING);
        resetMatchTiming();
        match.setStartTime(System.currentTimeMillis());
        match.setStrongholdDiscovered(false);
        match.setFortressDiscovered(false);
        match.setBlazeRodObtained(false);
        match.setBastionDiscovered(false);
        platform.getWorldProvider().unfreezeAllPlayers(match);
        if (match.getRunnerUuid() != null) {
            platform.getWorldProvider().setInvulnerable(match.getRunnerUuid(), false);
        }
        platform.getUIFacade().sendTitle("\u00a7cThe Hunt Has Begun!", "\u00a77Runner is on the loose!");
        platform.getUIFacade().sendToAll("\u00a7aThe hunt has started!");
    }

    public void stopGame() {
        match.accumulatePausedTime();
        match.setState(GameState.FINISHED);
        match.setEndTime(System.currentTimeMillis());
        if (countdownTaskId != -1) {
            platform.getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = -1;
        }
        platform.getUIFacade().stopUIUpdates();
        platform.getWorldProvider().clearEffects(match);
        platform.getWorldProvider().unfreezeAllPlayers(match);
        platform.getWorldProvider().teleportToMainWorld(match);
        match.setState(GameState.WAITING);
        match.setSeed(null);
        match.setWorldName(null);
        platform.getPlayerRegistry().reset();
    }

    public void runnerWins() {
        match.setState(GameState.FINISHED);
        match.setEndTime(System.currentTimeMillis());
        match.setRunnerWins(true);
        platform.getUIFacade().stopUIUpdates();
        platform.getWorldProvider().clearEffects(match);
        platform.getUIFacade().sendTitle("\u00a76Runner Wins!", "\u00a77The Ender Dragon has been defeated!");
        platform.getUIFacade().broadcastMessage("\u00a76\u00a7lRunner has won the game!");
        platform.getWorldProvider().unfreezeAllPlayers(match);
        platform.getWorldProvider().teleportToMainWorld(match);
        platform.getScheduler().runTaskTimer(() -> {
            match.setState(GameState.WAITING);
            match.setSeed(null);
            match.setWorldName(null);
            platform.getPlayerRegistry().reset();
        }, 200L, 0L);
        platform.getScheduler().cancelTask(countdownTaskId);
    }

    public void huntersWin() {
        match.setState(GameState.FINISHED);
        match.setEndTime(System.currentTimeMillis());
        match.setRunnerWins(false);
        platform.getUIFacade().stopUIUpdates();
        platform.getWorldProvider().clearEffects(match);
        platform.getUIFacade().sendTitle("\u00a7cHunters Win!", "\u00a77The Runner has been eliminated!");
        platform.getUIFacade().broadcastMessage("\u00a7c\u00a7lHunters have won the game!");
        platform.getWorldProvider().unfreezeAllPlayers(match);
        platform.getWorldProvider().teleportToMainWorld(match);
        platform.getScheduler().runTaskTimer(() -> {
            match.setState(GameState.WAITING);
            match.setSeed(null);
            match.setWorldName(null);
            platform.getPlayerRegistry().reset();
        }, 200L, 0L);
        platform.getScheduler().cancelTask(countdownTaskId);
    }

    public boolean isGameActive() {
        GameState s = match.getState();
        return s == GameState.RUNNING || s == GameState.PRE_HUNT || s == GameState.PAUSED;
    }

    public boolean pauseGame() {
        if (match.getState() != GameState.RUNNING && match.getState() != GameState.PRE_HUNT) return false;
        pauseInternal();
        return true;
    }

    public boolean pauseGame(UUID ownerUuid) {
        if (match.getState() != GameState.RUNNING && match.getState() != GameState.PRE_HUNT) return false;
        if (!match.isOwner(ownerUuid)) return false;
        pauseInternal();
        return true;
    }

    private void pauseInternal() {
        match.setPrePauseState(match.getState());
        match.setState(GameState.PAUSED);
        match.setPausedAt(System.currentTimeMillis());
        platform.getWorldProvider().freezeAllPlayers(match);
        platform.getWorldProvider().setGameRule(match, "doDaylightCycle", false);
        platform.getWorldProvider().setAllPlayersInvulnerable(match, true);
        platform.getWorldProvider().clearMobTargets(match);
        platform.getUIFacade().showPauseTitle();
        platform.getUIFacade().sendToAll("\u00a7eGame has been paused!");
    }

    public boolean resumeGame(UUID ownerUuid) {
        if (match.getState() != GameState.PAUSED) return false;
        if (!match.isOwner(ownerUuid)) return false;
        match.accumulatePausedTime();
        GameState previousState = match.getPrePauseState();
        match.setState(previousState);
        platform.getWorldProvider().setGameRule(match, "doDaylightCycle", true);
        platform.getWorldProvider().setAllPlayersInvulnerable(match, false);
        if (previousState == GameState.RUNNING) {
            platform.getWorldProvider().unfreezeAllPlayers(match);
        } else if (previousState == GameState.PRE_HUNT) {
            platform.getWorldProvider().unfreezeHorizontalAllPlayers(match);
        }
        platform.getUIFacade().hidePauseTitle();
        platform.getUIFacade().sendTitle("\u00a7aGame Resumed", "\u00a77The game has been resumed");
        platform.getUIFacade().sendToAll("\u00a7aGame has been resumed!");
        return true;
    }

    public boolean isGamePaused() { return match.getState() == GameState.PAUSED; }
}
