package xyz.qincai.manhunt.game;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.config.ConfigManager;
import xyz.qincai.manhunt.player.PlayerRole;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class GameManager {
    private final ManhuntNG plugin;
    private final Match match;
    private int countdownTaskId = -1;
    private int headstartTaskId = -1;
    private int pauseTimeoutTaskId = -1;
    private java.util.concurrent.atomic.AtomicInteger headstartCounter;
    private boolean forceStart;

    public GameManager(ManhuntNG plugin) {
        this.plugin = plugin;
        this.match = new Match(); // Each GameManager owns a single Match instance
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

    /*
     * Resets all timing-related fields for a new match.
     * Called when starting or force-starting a game.
     */
    private void resetMatchTiming() {
        match.setEndTime(0);
        match.setPausedAt(0);
        match.setTotalPausedDuration(0);
        match.setStartTime(0);
        match.setHeadstartRemaining(-1);
    }

    /*
     * wrapper for startGame(null)
     */
    public void startGame() {
        startGame(null);
    }

    /*
     * Starts the game using the selected start mode (Dreamstart or Headstart).
     * Validates runner & hunters, then transitions to COUNTDOWN.
     */
    public void startGame(UUID ownerUuid) {
        if (match.getState() != GameState.WAITING) return; // Only allowed from WAITING state

        // Must have runners + hunters selected
        if (match.getRunnerUuids().isEmpty()) {
            plugin.getUiManager().broadcastMessage(cfg().getMessage("game.no-runners"));
            return;
        }
        if (match.getHunterUuids().isEmpty()) {
            plugin.getUiManager().broadcastMessage(cfg().getMessage("game.no-hunters"));
            return;
        }

        match.setOwnerUuid(ownerUuid);
        match.setState(GameState.COUNTDOWN);
        startCountdown();
    }

    /*
     * force-start bypasses runner/hunter checks.
     * Used by admins or debugging only. skips PRE_HUNT phase
     */
    public void startGameForce(UUID ownerUuid) {
        if (match.getState() != GameState.WAITING) return;

        match.setOwnerUuid(ownerUuid);
        forceStart = true; // Marks that countdown should skip PRE_HUNT phase
        match.setState(GameState.COUNTDOWN);
        startCountdown();
    }

    /*
     * Begins the countdown timer before the game starts.
     * Freezes all players, shows titles, and transitions to finishCountdown().
     */
    private void startCountdown() {
        int initialCountdown = cfg().getPreHuntCountdown();
        match.setState(GameState.COUNTDOWN);

        freezeAllPlayers(); // Prevent movement during countdown

        AtomicInteger countdown = new AtomicInteger(initialCountdown);

        // Schedule repeating countdown task
        countdownTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // If state changed externally, stop countdown
            if (match.getState() != GameState.COUNTDOWN) {
                Bukkit.getScheduler().cancelTask(countdownTaskId);
                return;
            }

            int current = countdown.get();
            if (current <= 0) {
                // Countdown finished -> start game
                Bukkit.getScheduler().cancelTask(countdownTaskId);
                finishCountdown();
                return;
            }

            // Send countdown titles to all hunters
            for (UUID uuid : match.getHunterUuids()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    showCountdownTitle(player,
                            cfg().getMessage("countdown.title", "{seconds}", String.valueOf(current)),
                            cfg().getMessage("countdown.subtitle"), 1250);
                }
            }

            // Send countdown title to all runners
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

    /*
     * Called when countdown reaches zero.
     * Creates worlds, teleports players, applies effects, and transitions into PRE_HUNT or RUNNING.
     */
    private void finishCountdown() {
        plugin.getWorldManager().createGameWorlds();

        World gameWorld = match.getGameWorld();
        if (gameWorld == null) {
            // World creation failed -> abort
            plugin.getUiManager().broadcastMessage(cfg().getMessage("game.world-failed"));
            match.setState(GameState.WAITING);
            return;
        }

        clearPlayerState(); // Reset inventories + advancements
        plugin.getFormationManager().teleportToFormation(); // Teleport runner & hunters into formation

        setAllPlayersSurvival(); // set to survival mode
        healAllPlayers(); // Full heal + hunger

        // Reset progression flags
        match.setStrongholdDiscovered(false);
        match.setFortressDiscovered(false);
        match.setBlazeRodObtained(false);
        match.setBastionDiscovered(false);

        resetMatchTiming();

        // HEADSTART -> runner gets a headstart, hunters frozen
        if (match.getStartMode() == StartMode.HEADSTART) {
            match.setState(GameState.HEADSTART);

            applyHeadstartFreezeState();

            startHeadstartTimer(cfg().getHeadstartDuration());
        } else if (forceStart) {
            forceStart = false;
            match.setState(GameState.RUNNING);
            match.setStartTime(System.currentTimeMillis());

            unfreezeAllPlayers(); // Allow movement

            // All runners become vulnerable immediately
            for (UUID runnerUuid : match.getRunnerUuids()) {
                Player runner = Bukkit.getPlayer(runnerUuid);
                if (runner != null) runner.setInvulnerable(false);
            }

            // Start tracking + UI + effects
            plugin.getTrackerManager().giveCompassToAll();
            plugin.getTrackerManager().startTracking();
            plugin.getUiManager().startUIUpdates();
            plugin.getPotionEffectManager().applyEffects();

            plugin.getUiManager().sendTitle(
                    cfg().getMessage("forcestart.title"),
                    cfg().getMessage("forcestart.subtitle"));
            plugin.getUiManager().sendToAll(cfg().getMessage("forcestart.broadcast"));
        } else {
            // Normal start -> PRE_HUNT phase
            match.setState(GameState.PRE_HUNT);
            unfreezeHorizontalAllPlayers(); // Runner & hunters frozen horizontally but invulnerable

            plugin.getUiManager().sendTitle(
                    cfg().getMessage("prehunt.title"),
                    cfg().getMessage("prehunt.subtitle"));
            plugin.getUiManager().sendToAll(cfg().getMessage("prehunt.broadcast"));
        }
    }

    /*
     * Applies the HEADSTART freeze state: runners are unfrozen, hunters are frozen + invulnerable.
     */
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

    /*
     * Runs the headstart countdown timer.
     * Hunters remain frozen while the runner gets a headstart.
     */
    private void startHeadstartTimer(int initialDuration) {
        headstartCounter = new java.util.concurrent.atomic.AtomicInteger(initialDuration);
        match.setHeadstartRemaining(initialDuration);

        // Show headstart title to everyone
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

            // Send countdown to hunters
            for (UUID uuid : match.getHunterUuids()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    showCountdownTitle(player,
                            cfg().getMessage("headstart.countdown-hunter-title", "{seconds}", String.valueOf(current)),
                            cfg().getMessage("headstart.countdown-hunter-subtitle"), 1250);
                }
            }

            // Send remaining time to all runners
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

    /*
     * Called when headstart timer reaches zero.
     * Unfreezes hunters, transitions to RUNNING, starts tracking.
     */
    private void finishHeadstart() {
        match.setState(GameState.RUNNING);
        resetMatchTiming();
        match.setStartTime(System.currentTimeMillis());

        // Reset progression flags
        match.setStrongholdDiscovered(false);
        match.setFortressDiscovered(false);
        match.setBlazeRodObtained(false);
        match.setBastionDiscovered(false);

        unfreezeAllPlayers();

        // All runners become vulnerable
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) runner.setInvulnerable(false);
        }

        // Hunters no longer invulnerable
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.setInvulnerable(false);
        }

        // Start tracking + UI + effects
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

    /*
     * Sets all runners and hunters to survival mode.
     */
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

    /*
     * Fully heals all players. (health, hunger, saturation)
     */
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
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    /*
     * Called when runner damages a hunter during PRE_HUNT.
     * Transitions into RUNNING state.
     */
    public void startHunt() {
        if (match.getState() != GameState.PRE_HUNT) return;

        match.setState(GameState.RUNNING);
        resetMatchTiming();
        match.setStartTime(System.currentTimeMillis());

        // Reset progression flags
        match.setStrongholdDiscovered(false);
        match.setFortressDiscovered(false);
        match.setBlazeRodObtained(false);
        match.setBastionDiscovered(false);

        unfreezeAllPlayers(); // Allow movement

        // All runners become vulnerable
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) runner.setInvulnerable(false);
        }

        // Start tracking + UI + effects
        plugin.getTrackerManager().giveCompassToAll();
        plugin.getTrackerManager().startTracking();
        plugin.getUiManager().startUIUpdates();
        plugin.getPotionEffectManager().applyEffects();

        plugin.getUiManager().sendTitle(
                cfg().getMessage("prehunt.hunt-begun-title"),
                cfg().getMessage("prehunt.hunt-begun-subtitle"));
        plugin.getUiManager().sendToAll(cfg().getMessage("prehunt.hunt-begun-broadcast"));
    }

    /*
     * Stops the game and resets everything back to WAITING.
     */
    public void stopGame() {
        match.accumulatePausedTime();
        match.setState(GameState.FINISHED);
        match.setEndTime(System.currentTimeMillis());

        // Cancel countdown if running
        if (countdownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = -1;
        }

        // Cancel headstart if running
        if (headstartTaskId != -1) {
            Bukkit.getScheduler().cancelTask(headstartTaskId);
            headstartTaskId = -1;
        }
        headstartCounter = null;
        stopPauseTimeout();

        plugin.getTrackerManager().stopTracking();
        plugin.getUiManager().stopUIUpdates();
        plugin.getPotionEffectManager().clearEffects();
        unfreezeAllPlayers();
        plugin.getWorldManager().teleportToMainWorld();

        // Reset match + managers
        match.setState(GameState.WAITING);
        match.setSeed(null);
        match.setWorldName(null);
        match.clearAllPlayers();
        plugin.getPlayerManager().reset();
        plugin.getStatsManager().reset();
        plugin.getGameListener().clearSavedItems();
        plugin.getChatManager().resetDefaults();
    }

    /*
     * Called when the Ender Dragon dies.
     */
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
        plugin.getWorldManager().teleportToMainWorld();

        // Delay reset to allow players to see victory screen
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            match.setState(GameState.WAITING);
            match.setSeed(null);
            match.setWorldName(null);
            match.clearAllPlayers();
            plugin.getPlayerManager().reset();
            plugin.getGameListener().clearSavedItems();
            plugin.getChatManager().resetDefaults();
        }, 200L);
    }

    /*
     * Called when runner dies.
     */
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
        plugin.getWorldManager().teleportToMainWorld();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            match.setState(GameState.WAITING);
            match.setSeed(null);
            match.setWorldName(null);
            match.clearAllPlayers();
            plugin.getPlayerManager().reset();
            plugin.getGameListener().clearSavedItems();
            plugin.getChatManager().resetDefaults();
        }, 200L);
    }

    /*
     * Checks if the Ender Dragon is alive in the End.
     */
    public boolean isDragonAlive() {
        World endWorld = match.getEndWorld();
        if (endWorld == null) return false;
        return endWorld.getEntitiesByClass(EnderDragon.class)
                .stream()
                .anyMatch(dragon -> dragon.getHealth() > 0);
    }

    /*
     * Freezes all players completely (no movement).
     */
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

    /*
     * Clears inventory, armor, offhand, and all advancements.
     */
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

        // Reset all advancements
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            AdvancementProgress progress = player.getAdvancementProgress(iterator.next());
            progress.getAwardedCriteria().forEach(progress::revokeCriteria);
        }
    }

    /*
     * Unfreezes hunters fully, runners partially (invulnerable & no horizontal movement).
     * Used during PRE_HUNT.
     */
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

    /*
     * Fully unfreezes all players.
     */
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

    /*
     * Returns true if game is actively running or paused.
     */
    public boolean isGameActive() {
        return match.getState() == GameState.RUNNING ||
               match.getState() == GameState.PRE_HUNT ||
               match.getState() == GameState.HEADSTART ||
               match.getState() == GameState.PAUSED;
    }

    /*
     * Pauses the game without owner validation. Used for internal logic (right now, only when runner leaves the game)
     */
    public boolean pauseGame() {
        if (match.getState() != GameState.RUNNING && match.getState() != GameState.PRE_HUNT && match.getState() != GameState.HEADSTART) return false;
        pauseInternal();
        return true;
    }

    /*
     * Pauses the game with owner validation.
     */
    public boolean pauseGame(UUID ownerUuid) {
        if (match.getState() != GameState.RUNNING && match.getState() != GameState.PRE_HUNT && match.getState() != GameState.HEADSTART) return false;

        // Only owner or admin can pause
        if (!match.isOwner(ownerUuid)) {
            Player player = Bukkit.getPlayer(ownerUuid);
            if (player == null || !player.hasPermission("manhunt.admin")) return false;
        }

        pauseInternal();
        return true;
    }

    /*
     * Infects a runner, converting them to a hunter permanently.
     * Called when a runner dies in Infection mode.
     */
    public void infectPlayer(UUID runnerUuid) {
        if (match.getGameMode() != xyz.qincai.manhunt.game.ManhuntGameMode.INFECTION) return;
        if (match.getState() != GameState.RUNNING) return;

        plugin.getPlayerManager().infectRunnerToHunter(runnerUuid);
        Player infectedPlayer = Bukkit.getPlayer(runnerUuid);
        String name = infectedPlayer != null ? infectedPlayer.getName() : "Unknown";
        plugin.getUiManager().sendToAll(cfg().getMessage("infection.broadcast", "{player}", name));

        if (match.getRunnerUuids().isEmpty()) {
            huntersWin();
        }
    }

    /*
     * Internal pause logic shared by both pauseGame() methods.
     */
    private void pauseInternal() {
        match.setPrePauseState(match.getState());
        match.setState(GameState.PAUSED);
        match.setPausedAt(System.currentTimeMillis());

        // Cancel headstart timer if headstart was running
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

        // Freeze daylight cycle
        if (match.getGameWorld() != null) {
            match.getGameWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }

        setAllPlayersInvulnerable(true);
        clearMobTargets();

        plugin.getUiManager().showPauseTitle();
        plugin.getUiManager().sendToAll(cfg().getMessage("pause.broadcast"));

        // If a whole team disconnected (causing this pause), start the pause-timeout.
        // When it expires the OPPOSING team wins (e.g. all runners gone -> hunters win).
        boolean runnersOffline = isTeamFullyOffline(match.getRunnerUuids());
        boolean huntersOffline = isTeamFullyOffline(match.getHunterUuids());
        if (cfg().isPauseTimeoutEnabled() && (runnersOffline || huntersOffline)) {
            match.setPauseTimeoutHuntersWin(runnersOffline);
            startPauseTimeout();
        }
    }

    /*
     * Returns true if every player in the given set is currently offline.
     */
    private boolean isTeamFullyOffline(java.util.Set<UUID> uuids) {
        if (uuids.isEmpty()) return false;
        for (UUID id : uuids) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) return false;
        }
        return true;
    }

    /*
     * Starts the pause-timeout countdown. When it reaches zero while still paused,
     * the opposition team wins. Cancelled automatically on resume or stop.
     */
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

    /*
     * Cancels the pause-timeout task.
     */
    private void stopPauseTimeout() {
        if (pauseTimeoutTaskId != -1) {
            Bukkit.getScheduler().cancelTask(pauseTimeoutTaskId);
            pauseTimeoutTaskId = -1;
        }
    }

    /*
     * Resumes the game from PAUSED state.
     */
    public boolean resumeGame(UUID ownerUuid) {
        if (match.getState() != GameState.PAUSED) return false;

        // Cancel any pending pause-timeout (the game is being resumed)
        stopPauseTimeout();

        // Only owner or admin can resume
        if (!match.isOwner(ownerUuid)) {
            Player player = Bukkit.getPlayer(ownerUuid);
            if (player == null || !player.hasPermission("manhunt.admin")) return false;
        }

        match.accumulatePausedTime();

        GameState previousState = match.getPrePauseState();
        match.setState(previousState);

        // Restore daylight cycle
        if (match.getGameWorld() != null) {
            match.getGameWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        }

        setAllPlayersInvulnerable(false);

        // Restore movement + tracking depending on previous state
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

    /*
     * Returns true if the game is currently paused.
     */
    public boolean isGamePaused() {
        return match.getState() == GameState.PAUSED;
    }

    /*
     * Sets all players (runners + hunters) to invulnerable or vulnerable.
     * Used during pause/resume transitions.
     */
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

    /*
     * Clears all mob targets in the active game world.
     * Prevents mobs from continuing to chase players during pause.
     */
    private void clearMobTargets() {
        World world = match.getGameWorld();
        if (world == null) return;

        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (entity instanceof org.bukkit.entity.Mob mob) {
                // If mob is targeting a player, remove the target
                if (mob.getTarget() instanceof Player) {
                    mob.setTarget(null);
                }
            }
        }
    }
}
