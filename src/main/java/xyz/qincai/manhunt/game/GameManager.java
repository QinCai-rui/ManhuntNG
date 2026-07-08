package xyz.qincai.manhunt.game;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
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
    private java.util.concurrent.atomic.AtomicInteger headstartCounter;
    private boolean forceStart;

    public GameManager(ManhuntNG plugin) {
        this.plugin = plugin;
        this.match = new Match(); // Each GameManager owns a single Match instance
    }

    public Match getMatch() {
        return match;
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
            plugin.getUiManager().broadcastMessage("<red>No runners selected!");
            return;
        }
        if (match.getHunterUuids().isEmpty()) {
            plugin.getUiManager().broadcastMessage("<red>No hunters selected!");
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
        int initialCountdown = plugin.getConfigManager().getPreHuntCountdown();
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
                    showCountdownTitle(player, "<yellow>" + current, "<gray>Game starting in...", 1250);
                }
            }

            // Send countdown title to all runners
            for (UUID runnerUuid : match.getRunnerUuids()) {
                Player runner = Bukkit.getPlayer(runnerUuid);
                if (runner != null) {
                    showCountdownTitle(runner, "<yellow>" + current, "<gray>Game starting in...", 1250);
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
            plugin.getUiManager().broadcastMessage("<red>Failed to create game world!");
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

            startHeadstartTimer(plugin.getConfigManager().getHeadstartDuration());
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

            plugin.getUiManager().sendTitle("<red>The Hunt Has Begun!", "<gray>Runner is on the loose!");
            plugin.getUiManager().sendToAll("<green>The hunt has started! (Force started)");
        } else {
            // Normal start -> PRE_HUNT phase
            match.setState(GameState.PRE_HUNT);
            unfreezeHorizontalAllPlayers(); // Runner & hunters frozen horizontally but invulnerable

            plugin.getUiManager().sendTitle("<gold>Pre-Hunt", "<gray>Runner must damage a hunter to start the hunt");
            plugin.getUiManager().sendToAll("<yellow>Pre-Hunt phase! The runner must punch any hunter to start the hunt.");
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
            plugin.getConfigManager().getMessage("headstart.title"),
            plugin.getConfigManager().getMessage("headstart.subtitle", "{duration}", String.valueOf(initialDuration))
        );
        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                showCountdownTitle(player,
                    plugin.getConfigManager().getMessage("headstart.hunter-title"),
                    plugin.getConfigManager().getMessage("headstart.hunter-subtitle", "{duration}", String.valueOf(initialDuration)),
                    2000, 500);
            }
        }
        plugin.getUiManager().sendToAll("<yellow>Head start! Runner has <red>" + initialDuration + "<yellow> seconds to run!");

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
                    showCountdownTitle(player, "<red>" + current, "<gray>You are frozen!", 1250);
                }
            }

            // Send remaining time to all runners
            for (UUID runnerUuid : match.getRunnerUuids()) {
                Player runner = Bukkit.getPlayer(runnerUuid);
                if (runner != null) {
                    showCountdownTitle(runner, "<yellow>" + current, "<gray>Head start remaining", 1250);
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
        if (plugin.getConfigManager().isHeadstartCompassEnabled()) {
            plugin.getTrackerManager().giveCompassToAll();
        }
        plugin.getTrackerManager().startTracking();
        plugin.getUiManager().startUIUpdates();
        plugin.getPotionEffectManager().applyEffects();

        plugin.getUiManager().sendTitle("<red>The Hunt Has Begun!", "<gray>Runner is on the loose!");
        plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("headstart.ended"));
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

        plugin.getUiManager().sendTitle("<red>The Hunt Has Begun!", "<gray>Runner is on the loose!");
        plugin.getUiManager().sendToAll("<green>The hunt has started!");
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

        plugin.getTrackerManager().stopTracking();
        plugin.getUiManager().stopUIUpdates();
        plugin.getPotionEffectManager().clearEffects();
        unfreezeAllPlayers();
        plugin.getWorldManager().teleportToMainWorld();

        // Reset match + managers
        match.setState(GameState.WAITING);
        match.setSeed(null);
        match.setWorldName(null);
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

        plugin.getUiManager().sendTitle("<gold>Runner Wins!", "<gray>The Ender Dragon has been defeated!");
        plugin.getUiManager().broadcastMessage("<gold><bold>The Runner has won the game!");

        unfreezeAllPlayers();
        plugin.getWorldManager().teleportToMainWorld();

        // Delay reset to allow players to see victory screen
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            match.setState(GameState.WAITING);
            match.setSeed(null);
            match.setWorldName(null);
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
        
        plugin.getUiManager().sendTitle("<red>Hunters Win!", "<gray>The Runner has been eliminated!");
        plugin.getUiManager().broadcastMessage("<red><bold>The Hunters have won the game!");

        unfreezeAllPlayers();
        plugin.getWorldManager().teleportToMainWorld();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            match.setState(GameState.WAITING);
            match.setSeed(null);
            match.setWorldName(null);
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
        player.getInventory().setArmorContents(null);
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
        Match match = plugin.getGameManager().getMatch();
        if (match.getGameMode() != GameMode.INFECTION) return;

        plugin.getPlayerManager().infectRunnerToHunter(runnerUuid);
        Player infectedPlayer = Bukkit.getPlayer(runnerUuid);
        String name = infectedPlayer != null ? infectedPlayer.getName() : "Unknown";
        plugin.getUiManager().sendToAll("<red>" + name + " <gray>has been infected and is now a Hunter!");

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
        plugin.getUiManager().sendToAll("<yellow>Game has been paused!");
    }

    /*
     * Resumes the game from PAUSED state.
     */
    public boolean resumeGame(UUID ownerUuid) {
        if (match.getState() != GameState.PAUSED) return false;

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
            if (remaining <= 0) remaining = plugin.getConfigManager().getHeadstartDuration();
            startHeadstartTimer(remaining);
        }

        plugin.getUiManager().hidePauseTitle();
        plugin.getUiManager().sendTitle("<green>Game Resumed", "<gray>The game has been resumed");
        plugin.getUiManager().sendToAll("<green>Game has been resumed!");

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
