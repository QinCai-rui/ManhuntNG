package xyz.qincai.manhunt.game;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.config.ConfigManager;

import java.time.Duration;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class GameManager {
    private final ManhuntNG plugin;
    private final Match match;
    private int countdownTaskId = -1;
    private int warmupTaskId = -1;
    private int headstartTaskId = -1;
    private int pauseTimeoutTaskId = -1;
    private AtomicInteger headstartCounter;
    private boolean forceStart;

    public GameManager(ManhuntNG plugin) {
        this.plugin = plugin;
        this.match = new Match();
    }

    public Match getMatch() {
        return match;
    }

    private ConfigManager cfg() {
        return plugin.getConfigManager();
    }

    private void showCountdownTitle(Player player, String title, String subtitle, long stayMs) {
        showCountdownTitle(player, title, subtitle, stayMs, 0);
    }

    private void showCountdownTitle(Player player, String title, String subtitle, long stayMs, long fadeOutMs) {
        player.showTitle(Title.title(
            MiniMessage.miniMessage().deserialize(title),
            MiniMessage.miniMessage().deserialize(subtitle),
            Title.Times.times(Duration.ZERO, Duration.ofMillis(stayMs), Duration.ofMillis(fadeOutMs))
        ));
    }

    private void resetMatchTiming() {
        match.setEndTime(0);
        match.setPausedAt(0);
        match.setTotalPausedDuration(0);
        match.setStartTime(0);
        match.setHeadstartRemaining(-1);
    }

    public void startGame() {
        startGame(null);
    }

    public void startGame(UUID ownerUuid) {
        if (match.getState() != GameState.WAITING) return;

        if (match.getRunnerUuids().isEmpty()) {
            plugin.getUiManager().broadcastMessage(cfg().getMessage("game.no-runners"));
            return;
        }
        if (match.getHunterUuids().isEmpty()) {
            plugin.getUiManager().broadcastMessage(cfg().getMessage("game.no-hunters"));
            return;
        }

        match.setOwnerUuid(ownerUuid);
        match.setState(GameState.GENERATING);

        plugin.getUiManager().broadcastMessage(cfg().getMessage("game.generating-worlds"));

        Bukkit.getScheduler().runTask(plugin, () -> {
            // Guard against state change (e.g., stopGame() was called while queued)
            if (match.getState() != GameState.GENERATING) return;

            plugin.getWorldManager().teleportToLobby(match);
            plugin.getWorldManager().deleteAndGenerateWorlds(match);

            // Re-check state after world generation
            if (match.getState() != GameState.GENERATING) return;

            World gameWorld = match.getGameWorld();
            if (gameWorld == null) {
                plugin.getUiManager().broadcastMessage(cfg().getMessage("game.world-failed"));
                match.setState(GameState.WAITING);
                forceStart = false;
                return;
            }

            match.setState(GameState.COUNTDOWN);
            startCountdown();
        });
    }

    public boolean startGameForce(UUID ownerUuid) {
        if (match.getState() != GameState.WAITING) return false;

        match.setOwnerUuid(ownerUuid);
        forceStart = true;
        match.setState(GameState.GENERATING);

        plugin.getUiManager().broadcastMessage(cfg().getMessage("game.generating-worlds"));

        Bukkit.getScheduler().runTask(plugin, () -> {
            // Guard against state change (e.g., stopGame() was called while queued)
            if (match.getState() != GameState.GENERATING) return;

            plugin.getWorldManager().teleportToLobby(match);
            plugin.getWorldManager().deleteAndGenerateWorlds(match);

            // Re-check state after world generation
            if (match.getState() != GameState.GENERATING) return;

            World gameWorld = match.getGameWorld();
            if (gameWorld == null) {
                plugin.getUiManager().broadcastMessage(cfg().getMessage("game.world-failed"));
                match.setState(GameState.WAITING);
                forceStart = false;
                return;
            }

            match.setState(GameState.COUNTDOWN);
            startCountdown();
        });
        return true;
    }

    private void startCountdown() {
        int initialCountdown = cfg().getPreHuntCountdown();
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
                    showCountdownTitle(player,
                            cfg().getMessage("countdown.title", "{seconds}", String.valueOf(current)),
                            cfg().getMessage("countdown.subtitle"), 1250);
                }
            }

            for (UUID runnerUuid : match.getRunnerUuids()) {
                Player runner = Bukkit.getPlayer(runnerUuid);
                if (runner != null) {
                    showCountdownTitle(runner,
                            cfg().getMessage("countdown.title", "{seconds}", String.valueOf(current)),
                            cfg().getMessage("countdown.subtitle"), 1250);
                }
            }

            countdown.decrementAndGet();
        }, 0L, 20L).getTaskId();
    }

    private void finishCountdown() {
        World gameWorld = match.getGameWorld();
        if (gameWorld == null) {
            plugin.getUiManager().broadcastMessage(cfg().getMessage("game.world-failed"));
            match.setState(GameState.WAITING);
            return;
        }

        clearPlayerState();

        for (UUID uuid : match.getRunnerUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.teleport(gameWorld.getSpawnLocation());
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.teleport(gameWorld.getSpawnLocation());
                player.setGameMode(GameMode.SURVIVAL);
            }
        }

        healAllPlayers();

        match.setStrongholdDiscovered(false);
        match.setFortressDiscovered(false);
        match.setBlazeRodObtained(false);
        match.setBastionDiscovered(false);

        resetMatchTiming();

        int warmupDuration = cfg().getWarmupDuration();
        if (warmupDuration > 0) {
            startWarmup(warmupDuration);
        } else {
            finishWarmup();
        }
    }

    private void startWarmup(int duration) {
        match.setState(GameState.WARMUP);
        freezeAllPlayers();

        AtomicInteger counter = new AtomicInteger(duration);

        plugin.getUiManager().sendTitle(
                cfg().getMessage("warmup.title"),
                cfg().getMessage("warmup.subtitle", "{seconds}", String.valueOf(duration)));

        warmupTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (match.getState() != GameState.WARMUP) {
                Bukkit.getScheduler().cancelTask(warmupTaskId);
                return;
            }

            int current = counter.getAndDecrement();
            if (current <= 0) {
                Bukkit.getScheduler().cancelTask(warmupTaskId);
                warmupTaskId = -1;
                finishWarmup();
                return;
            }

            for (UUID uuid : match.getRunnerUuids()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    showCountdownTitle(player,
                            cfg().getMessage("warmup.countdown-title", "{seconds}", String.valueOf(current)),
                            cfg().getMessage("warmup.countdown-subtitle"), 1250);
                }
            }
            for (UUID uuid : match.getHunterUuids()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    showCountdownTitle(player,
                            cfg().getMessage("warmup.countdown-title", "{seconds}", String.valueOf(current)),
                            cfg().getMessage("warmup.countdown-subtitle"), 1250);
                }
            }
        }, 20L, 20L).getTaskId();
    }

    private void finishWarmup() {
        plugin.getFormationManager().teleportToFormation();

        setAllPlayersSurvival();
        healAllPlayers();

        // Capture and clear forceStart flag before branching on start mode
        boolean wasForceStart = forceStart;
        forceStart = false;

        if (match.getStartMode() == StartMode.HEADSTART) {
            match.setState(GameState.HEADSTART);
            applyHeadstartFreezeState();
            startHeadstartTimer(cfg().getHeadstartDuration());
        } else if (wasForceStart) {
            match.setState(GameState.RUNNING);
            match.setStartTime(System.currentTimeMillis());

            unfreezeAllPlayers();

            for (UUID runnerUuid : match.getRunnerUuids()) {
                Player runner = Bukkit.getPlayer(runnerUuid);
                if (runner != null) runner.setInvulnerable(false);
            }

            plugin.getTrackerManager().giveCompassToAll();
            plugin.getTrackerManager().startTracking();
            plugin.getUiManager().startUIUpdates();
            plugin.getPotionEffectManager().applyEffects();

            plugin.getUiManager().sendTitle(
                    cfg().getMessage("forcestart.title"),
                    cfg().getMessage("forcestart.subtitle"));
            plugin.getUiManager().sendToAll(cfg().getMessage("forcestart.broadcast"));
        } else {
            match.setState(GameState.PRE_HUNT);
            unfreezeHorizontalAllPlayers();

            plugin.getUiManager().sendTitle(
                    cfg().getMessage("prehunt.title"),
                    cfg().getMessage("prehunt.subtitle"));
            plugin.getUiManager().sendToAll(cfg().getMessage("prehunt.broadcast"));
        }
    }

    private void applyHeadstartFreezeState() {
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) {
                runner.setWalkSpeed(0.2f);
                runner.setFlySpeed(0.1f);
            }
        }
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setWalkSpeed(0f);
                player.setFlySpeed(0f);
                player.setInvulnerable(true);
            }
        }
    }

    private void startHeadstartTimer(int initialDuration) {
        headstartCounter = new AtomicInteger(initialDuration);
        match.setHeadstartRemaining(initialDuration);

        plugin.getUiManager().sendTitle(
            cfg().getMessage("headstart.title"),
            cfg().getMessage("headstart.subtitle", "{duration}", String.valueOf(initialDuration))
        );
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                showCountdownTitle(player,
                    cfg().getMessage("headstart.hunter-title"),
                    cfg().getMessage("headstart.hunter-subtitle", "{duration}", String.valueOf(initialDuration)),
                    2000, 500);
            }
        }
        plugin.getUiManager().sendToAll(cfg().getMessage("headstart.broadcast", "{duration}", String.valueOf(initialDuration)));

        headstartTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (match.getState() != GameState.HEADSTART) {
                Bukkit.getScheduler().cancelTask(headstartTaskId);
                headstartTaskId = -1;
                return;
            }

            int current = headstartCounter.get();
            match.setHeadstartRemaining(current);
            if (current <= 0) {
                Bukkit.getScheduler().cancelTask(headstartTaskId);
                headstartTaskId = -1;
                headstartCounter = null;
                finishHeadstart();
                return;
            }

            for (UUID uuid : match.getHunterUuids()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    showCountdownTitle(player,
                            cfg().getMessage("headstart.countdown-hunter-title", "{seconds}", String.valueOf(current)),
                            cfg().getMessage("headstart.countdown-hunter-subtitle"), 1250);
                }
            }

            for (UUID runnerUuid : match.getRunnerUuids()) {
                Player runner = Bukkit.getPlayer(runnerUuid);
                if (runner != null) {
                    showCountdownTitle(runner,
                            cfg().getMessage("headstart.countdown-runner-title", "{seconds}", String.valueOf(current)),
                            cfg().getMessage("headstart.countdown-runner-subtitle"), 1250);
                }
            }

            headstartCounter.decrementAndGet();
        }, 0L, 20L).getTaskId();
    }

    private void finishHeadstart() {
        match.setState(GameState.RUNNING);
        resetMatchTiming();
        match.setStartTime(System.currentTimeMillis());

        match.setStrongholdDiscovered(false);
        match.setFortressDiscovered(false);
        match.setBlazeRodObtained(false);
        match.setBastionDiscovered(false);

        unfreezeAllPlayers();

        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) runner.setInvulnerable(false);
        }

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.setInvulnerable(false);
        }

        if (cfg().isHeadstartCompassEnabled()) {
            plugin.getTrackerManager().giveCompassToAll();
        }
        plugin.getTrackerManager().startTracking();
        plugin.getUiManager().startUIUpdates();
        plugin.getPotionEffectManager().applyEffects();

        plugin.getUiManager().sendTitle(
                cfg().getMessage("prehunt.hunt-begun-title"),
                cfg().getMessage("prehunt.hunt-begun-subtitle"));
        plugin.getUiManager().sendToAll(cfg().getMessage("headstart.ended"));
    }

    private void setAllPlayersSurvival() {
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) runner.setGameMode(GameMode.SURVIVAL);
        }
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private void healAllPlayers() {
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) healPlayer(runner);
        }
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) healPlayer(player);
        }
    }

    private void healPlayer(Player player) {
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
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

        unfreezeAllPlayers();

        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) runner.setInvulnerable(false);
        }

        plugin.getTrackerManager().giveCompassToAll();
        plugin.getTrackerManager().startTracking();
        plugin.getUiManager().startUIUpdates();
        plugin.getPotionEffectManager().applyEffects();

        plugin.getUiManager().sendTitle(
                cfg().getMessage("prehunt.hunt-begun-title"),
                cfg().getMessage("prehunt.hunt-begun-subtitle"));
        plugin.getUiManager().sendToAll(cfg().getMessage("prehunt.hunt-begun-broadcast"));
    }

    public void stopGame() {
        match.accumulatePausedTime();
        match.setState(GameState.FINISHED);
        match.setEndTime(System.currentTimeMillis());

        if (countdownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = -1;
        }
        if (warmupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(warmupTaskId);
            warmupTaskId = -1;
        }
        if (headstartTaskId != -1) {
            Bukkit.getScheduler().cancelTask(headstartTaskId);
            headstartTaskId = -1;
        }
        headstartCounter = null;
        forceStart = false;
        stopPauseTimeout();

        plugin.getTrackerManager().stopTracking();
        plugin.getUiManager().stopUIUpdates();
        plugin.getPotionEffectManager().clearEffects();
        unfreezeAllPlayers();
        plugin.getWorldManager().teleportToLobby(match);

        match.setState(GameState.WAITING);
        match.setSeed(null);
        match.clearAllPlayers();
        plugin.getPlayerManager().reset();
        plugin.getStatsManager().reset();
        plugin.getGameListenerState().clearSavedItems();
        if (plugin.getLootListener() != null) {
            plugin.getLootListener().clearTracking();
        }
        plugin.getChatManager().resetDefaults();
    }

    public void runnerWins() {
        match.setState(GameState.FINISHED);
        match.setEndTime(System.currentTimeMillis());
        match.setRunnerWins(true);
        plugin.getStatsManager().recordWin(true);

        plugin.getTrackerManager().stopTracking();
        plugin.getUiManager().stopUIUpdates();
        plugin.getPotionEffectManager().clearEffects();

        plugin.getUiManager().sendTitle(
                cfg().getMessage("game.runner-wins-title"),
                cfg().getMessage("game.runner-wins-subtitle"));
        plugin.getUiManager().broadcastMessage(cfg().getMessage("game.runner-wins-broadcast"));

        unfreezeAllPlayers();
        plugin.getWorldManager().teleportToLobby(match);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            match.setState(GameState.WAITING);
            match.setSeed(null);
            match.clearAllPlayers();
            plugin.getPlayerManager().reset();
            plugin.getGameListenerState().clearSavedItems();
            plugin.getChatManager().resetDefaults();
        }, 200L);
    }

    public void huntersWin() {
        match.setState(GameState.FINISHED);
        match.setEndTime(System.currentTimeMillis());
        match.setRunnerWins(false);
        plugin.getStatsManager().recordWin(false);

        plugin.getTrackerManager().stopTracking();
        plugin.getUiManager().stopUIUpdates();
        plugin.getPotionEffectManager().clearEffects();

        plugin.getUiManager().sendTitle(
                cfg().getMessage("game.hunters-win-title"),
                cfg().getMessage("game.hunters-win-subtitle"));
        plugin.getUiManager().broadcastMessage(cfg().getMessage("game.hunters-win-broadcast"));

        unfreezeAllPlayers();
        plugin.getWorldManager().teleportToLobby(match);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            match.setState(GameState.WAITING);
            match.setSeed(null);
            match.clearAllPlayers();
            plugin.getPlayerManager().reset();
            plugin.getGameListenerState().clearSavedItems();
            plugin.getChatManager().resetDefaults();
        }, 200L);
    }

    public boolean isDragonAlive() {
        World endWorld = match.getEndWorld();
        if (endWorld == null) return false;
        return endWorld.getEntitiesByClass(EnderDragon.class)
                .stream()
                .anyMatch(dragon -> dragon.getHealth() > 0);
    }

    public void freezeAllPlayers() {
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
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

    private void clearPlayerState() {
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) clearPlayerState(runner);
        }
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) clearPlayerState(player);
        }
    }

    private void clearPlayerState(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[player.getInventory().getArmorContents().length]);
        player.getInventory().setItemInMainHand(null);
        player.getInventory().setItemInOffHand(null);

        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            AdvancementProgress progress = player.getAdvancementProgress(iterator.next());
            progress.getAwardedCriteria().forEach(progress::revokeCriteria);
        }
    }

    public void unfreezeHorizontalAllPlayers() {
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) {
                runner.setWalkSpeed(0f);
                runner.setFlySpeed(0f);
                runner.setInvulnerable(true);
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

    public void unfreezeAllPlayers() {
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
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

    public boolean isGameActive() {
        return match.getState() == GameState.RUNNING ||
               match.getState() == GameState.PRE_HUNT ||
               match.getState() == GameState.HEADSTART ||
               match.getState() == GameState.WARMUP ||
               match.getState() == GameState.GENERATING ||
               match.getState() == GameState.PAUSED;
    }

    public boolean pauseGame() {
        if (match.getState() != GameState.RUNNING && match.getState() != GameState.PRE_HUNT && match.getState() != GameState.HEADSTART) return false;
        pauseInternal();
        return true;
    }

    public boolean pauseGame(UUID ownerUuid) {
        if (match.getState() != GameState.RUNNING && match.getState() != GameState.PRE_HUNT && match.getState() != GameState.HEADSTART) return false;

        if (!match.isOwner(ownerUuid)) {
            Player player = Bukkit.getPlayer(ownerUuid);
            if (player == null || !player.hasPermission("manhunt.admin")) return false;
        }

        pauseInternal();
        return true;
    }

    public void infectPlayer(UUID runnerUuid) {
        if (match.getGameMode() != ManhuntGameMode.INFECTION) return;
        if (match.getState() != GameState.RUNNING) return;

        plugin.getPlayerManager().infectRunnerToHunter(runnerUuid);
        Player infectedPlayer = Bukkit.getPlayer(runnerUuid);
        String name = infectedPlayer != null ? infectedPlayer.getName() : "Unknown";
        plugin.getUiManager().sendToAll(cfg().getMessage("infection.broadcast", "{player}", name));

        if (match.getRunnerUuids().isEmpty()) {
            huntersWin();
        }
    }

    private void pauseInternal() {
        match.setPrePauseState(match.getState());
        match.setState(GameState.PAUSED);
        match.setPausedAt(System.currentTimeMillis());

        if (headstartTaskId != -1) {
            if (headstartCounter != null) {
                match.setHeadstartRemaining(headstartCounter.get());
            }
            Bukkit.getScheduler().cancelTask(headstartTaskId);
            headstartTaskId = -1;
            headstartCounter = null;
        }

        freezeAllPlayers();
        plugin.getTrackerManager().stopTracking();

        if (match.getGameWorld() != null) {
            match.getGameWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }

        setAllPlayersInvulnerable(true);
        clearMobTargets();

        plugin.getUiManager().showPauseTitle();
        plugin.getUiManager().sendToAll(cfg().getMessage("pause.broadcast"));

        boolean runnersOffline = isTeamFullyOffline(match.getRunnerUuids());
        boolean huntersOffline = isTeamFullyOffline(match.getHunterUuids());
        if (cfg().isPauseTimeoutEnabled() && (runnersOffline || huntersOffline)) {
            match.setPauseTimeoutHuntersWin(runnersOffline);
            startPauseTimeout();
        }
    }

    private boolean isTeamFullyOffline(java.util.Set<UUID> uuids) {
        if (uuids.isEmpty()) return false;
        for (UUID id : uuids) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) return false;
        }
        return true;
    }

    private void startPauseTimeout() {
        stopPauseTimeout();
        if (!cfg().isPauseTimeoutEnabled()) return;

        match.setPauseTimeoutRemaining(cfg().getPauseTimeoutDuration());

        pauseTimeoutTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (match.getState() != GameState.PAUSED) {
                stopPauseTimeout();
                return;
            }

            int remaining = match.getPauseTimeoutRemaining();
            if (remaining <= 0) {
                stopPauseTimeout();
                if (match.isPauseTimeoutHuntersWin()) {
                    plugin.getUiManager().broadcastMessage(cfg().getMessage("pause.timeout-hunters-win"));
                    huntersWin();
                } else {
                    plugin.getUiManager().broadcastMessage(cfg().getMessage("pause.timeout-runner-wins"));
                    runnerWins();
                }
                return;
            }
            match.setPauseTimeoutRemaining(remaining - 1);
        }, 20L, 20L).getTaskId();
    }

    private void stopPauseTimeout() {
        if (pauseTimeoutTaskId != -1) {
            Bukkit.getScheduler().cancelTask(pauseTimeoutTaskId);
            pauseTimeoutTaskId = -1;
        }
    }

    public boolean resumeGame(UUID ownerUuid) {
        if (match.getState() != GameState.PAUSED) return false;

        stopPauseTimeout();

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
        } else if (previousState == GameState.PRE_HUNT) {
            unfreezeHorizontalAllPlayers();
        } else if (previousState == GameState.HEADSTART) {
            applyHeadstartFreezeState();
            int remaining = match.getHeadstartRemaining();
            if (remaining <= 0) remaining = cfg().getHeadstartDuration();
            startHeadstartTimer(remaining);
        }

        plugin.getUiManager().hidePauseTitle();
        plugin.getUiManager().sendTitle(
                cfg().getMessage("resume.title"),
                cfg().getMessage("resume.subtitle"));
        plugin.getUiManager().sendToAll(cfg().getMessage("resume.broadcast"));

        return true;
    }

    public boolean isGamePaused() {
        return match.getState() == GameState.PAUSED;
    }

    private void setAllPlayersInvulnerable(boolean invulnerable) {
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
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
            if (entity instanceof org.bukkit.entity.Mob mob) {
                if (mob.getTarget() instanceof Player) {
                    mob.setTarget(null);
                }
            }
        }
    }
}
