package xyz.qincai.manhunt.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.config.ConfigManager;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;

import java.time.Duration;
import java.util.UUID;

public class UIManager {
    private final ManhuntNG plugin;
    private Scoreboard scoreboard;
    private Objective objective;
    private int actionBarTaskId = -1;
    private int scoreboardTaskId = -1;
    private GamePhase currentPhase = GamePhase.OVERWORLD_PREP;

    private String[] currentEntries = new String[7];

    public UIManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    private ConfigManager cfg() {
        return plugin.getConfigManager();
    }

    public void init() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    }

    public void setCurrentPhase(GamePhase phase) {
        this.currentPhase = phase;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public void updatePhase() {
        Match match = plugin.getGameManager().getMatch();
        if (match.getGameWorld() == null || match.getRunnerUuid() == null) return;

        Player runner = Bukkit.getPlayer(match.getRunnerUuid());
        if (runner == null) return;

        World.Environment env = runner.getWorld().getEnvironment();

        if (env == World.Environment.THE_END) {
            World endWorld = match.getEndWorld();
            EnderDragon dragon = endWorld != null ? endWorld.getEntitiesByClass(EnderDragon.class)
                    .stream().findFirst().orElse(null) : null;
            boolean dragonDamaged = dragon != null && dragon.getHealth() < dragon.getMaxHealth();
            boolean allCrystalsDestroyed = endWorld != null && endWorld.getEntitiesByClass(EnderCrystal.class).isEmpty();
            if (dragonDamaged || allCrystalsDestroyed) {
                currentPhase = GamePhase.FINALE;
            } else {
                currentPhase = GamePhase.END_RUSH;
            }
        } else if (env == World.Environment.NETHER) {
            if (match.isFortressDiscovered()) {
                if (match.isBlazeRodObtained()) {
                    currentPhase = GamePhase.BLAZE_ROD_RUN;
                } else {
                    currentPhase = GamePhase.FORTRESS_RUN;
                }
            } else if (match.isBastionDiscovered()) {
                currentPhase = GamePhase.BASTION_ROUTE;
            } else {
                currentPhase = GamePhase.NETHER_RUSH;
            }
        } else {
            if (match.isStrongholdDiscovered()) {
                currentPhase = GamePhase.STRONGHOLD_DIVE;
            } else if (match.isBlazeRodObtained()) {
                currentPhase = GamePhase.RETURN_EYES;
            } else {
                currentPhase = GamePhase.OVERWORLD_PREP;
            }
        }
    }

    public void startUIUpdates() {
        stopUIUpdates();
        setupScoreboard();
        if (cfg().isActionBarEnabled()) {
            actionBarTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::updateActionBar, 0L, 20L).getTaskId();
        }
        if (cfg().isScoreboardEnabled()) {
            scoreboardTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::updateScoreboard, 0L, 20L).getTaskId();
        }
    }

    public void stopUIUpdates() {
        if (actionBarTaskId != -1) {
            Bukkit.getScheduler().cancelTask(actionBarTaskId);
            actionBarTaskId = -1;
        }
        if (scoreboardTaskId != -1) {
            Bukkit.getScheduler().cancelTask(scoreboardTaskId);
            scoreboardTaskId = -1;
        }
        if (objective != null) {
            objective.unregister();
            objective = null;
        }
        for (int i = 0; i < currentEntries.length; i++) {
            currentEntries[i] = null;
        }
    }

    private void setupScoreboard() {
        if (objective != null) return;
        objective = scoreboard.registerNewObjective("manhunt", Criteria.DUMMY,
                MiniMessage.miniMessage().deserialize(cfg().getMessage("scoreboard.title")));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    private void updateScoreboard() {
        Match match = plugin.getGameManager().getMatch();
        if (objective == null) return;

        int runnerCount = match.getRunnerUuids().size();
        if (runnerCount > 0) {
            if (runnerCount == 1) {
                updateLine(0, cfg().getMessage("scoreboard.runner") + getRunnerName(match));
            } else {
                updateLine(0, cfg().getMessage("scoreboard.runners") + runnerCount);
            }
        } else {
            updateLine(0, cfg().getMessage("scoreboard.runner-none"));
        }
        updateLine(1, "");
        updateLine(2, cfg().getMessage("scoreboard.hunters") + plugin.getPlayerManager().getAliveHunterCount());
        updateLine(3, " ");
        updateLine(4, cfg().getMessage("scoreboard.time") + formatTime(match.getElapsedSeconds()));
        updateLine(5, "  ");
        updateLine(6, cfg().getMessage("scoreboard.dimension") + getDimension(match));

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.setScoreboard(scoreboard);
        }
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) runner.setScoreboard(scoreboard);
        }
    }

    private void updateLine(int index, String text) {
        String old = currentEntries[index];
        if (text.equals(old)) return;

        if (old != null) {
            objective.getScore(old).resetScore();
        }
        objective.getScore(text).setScore(7 - index);
        currentEntries[index] = text;
    }

    private String getRunnerName(Match match) {
        Player runner = Bukkit.getPlayer(match.getRunnerUuid());
        return runner != null ? runner.getName() : "Unknown";
    }

    private String formatTime(long elapsedSeconds) {
        long hours = elapsedSeconds / 3600;
        long minutes = (elapsedSeconds % 3600) / 60;
        long seconds = elapsedSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String getDimension(Match match) {
        if (match.getRunnerUuid() == null) return cfg().getMessage("scoreboard.dim-overworld");
        Player runner = Bukkit.getPlayer(match.getRunnerUuid());
        if (runner == null) return cfg().getMessage("scoreboard.dim-overworld");
        return switch (runner.getWorld().getEnvironment()) {
            case NETHER -> cfg().getMessage("scoreboard.dim-nether");
            case THE_END -> cfg().getMessage("scoreboard.dim-end");
            default -> cfg().getMessage("scoreboard.dim-overworld");
        };
    }

    private void updateActionBar() {
        Match match = plugin.getGameManager().getMatch();
        if (match.getState() != GameState.RUNNING && match.getState() != GameState.HEADSTART && match.getState() != GameState.PAUSED) return;

        if (match.getState() == GameState.PAUSED) {
            Component pausedBar = MiniMessage.miniMessage().deserialize(cfg().getMessage("pause.action-bar"));

            // Show the pause-timeout countdown and who wins if it expires
            if (cfg().isPauseTimeoutEnabled()) {
                int remaining = match.getPauseTimeoutRemaining();
                if (remaining >= 0) {
                    String winner = match.isPauseTimeoutHuntersWin()
                            ? cfg().getMessage("game.hunters-win-broadcast")
                            : cfg().getMessage("game.runner-wins-broadcast");
                    pausedBar = pausedBar.append(MiniMessage.miniMessage().deserialize(
                            cfg().getMessage("pause.action-bar-timeout",
                                    "{winner}", winner,
                                    "{time}", formatTime(remaining))));
                }
            }

            for (UUID uuid : match.getHunterUuids()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) player.sendActionBar(pausedBar);
            }
            for (UUID runnerUuid : match.getRunnerUuids()) {
                Player runner = Bukkit.getPlayer(runnerUuid);
                if (runner != null) runner.sendActionBar(pausedBar);
            }
            return;
        }

        updatePhase();

        Component actionBar = Component.text(currentPhase.getDisplay(cfg()), NamedTextColor.GOLD);

        boolean showDistance = cfg().isTrackingShowDistance();

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            if (showDistance) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (plugin.getTrackerManager().isTrackerCompass(hand)) {
                    Player trackedRunner = plugin.getTrackerManager().findNearestRunner(player);
                    if (trackedRunner != null) {
                        if (player.getWorld().equals(trackedRunner.getWorld())) {
                            int dist = (int) Math.round(player.getLocation().distance(trackedRunner.getLocation()));
                            player.sendActionBar(MiniMessage.miniMessage().deserialize(
                                    cfg().getMessage("actionbar.runner-distance",
                                            "{distance}", String.valueOf(dist))));
                        } else {
                            String portal = switch (trackedRunner.getWorld().getEnvironment()) {
                                case NETHER -> cfg().getMessage("actionbar.portal-nether");
                                case THE_END -> cfg().getMessage("actionbar.portal-end");
                                default -> cfg().getMessage("actionbar.portal-default");
                            };
                            Location lastLoc = plugin.getTrackerManager().getRunnerLastKnownLocation(
                                    trackedRunner.getUniqueId(),
                                    player.getWorld().getEnvironment());
                            if (lastLoc != null) {
                                int dist = (int) Math.round(player.getLocation().distance(lastLoc));
                                player.sendActionBar(MiniMessage.miniMessage().deserialize(
                                        cfg().getMessage("actionbar.tracking",
                                                "{portal}", portal,
                                                "{distance}", String.valueOf(dist))));
                            } else {
                                player.sendActionBar(MiniMessage.miniMessage().deserialize(
                                        cfg().getMessage("actionbar.tracking-no-distance",
                                                "{portal}", portal)));
                            }
                        }
                        continue;
                    }
                }
            }

            player.sendActionBar(actionBar);
        }
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) {
                runner.sendActionBar(actionBar);
            }
        }
    }

    public void showPauseTitle() {
        Match match = plugin.getGameManager().getMatch();
        Component titleComp = MiniMessage.miniMessage().deserialize(cfg().getMessage("pause.title"))
                .decoration(TextDecoration.BOLD, true);
        Component subtitleComp = MiniMessage.miniMessage().deserialize(cfg().getMessage("pause.subtitle"));
        Title titleObj = Title.title(titleComp, subtitleComp, Title.Times.times(
                Duration.ofMillis(500), Duration.ofHours(24), Duration.ofMillis(500)));

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.showTitle(titleObj);
        }
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) runner.showTitle(titleObj);
        }
    }

    public void hidePauseTitle() {
        Match match = plugin.getGameManager().getMatch();

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.resetTitle();
        }
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) runner.resetTitle();
        }
    }

    public void sendTitle(String title, String subtitle) {
        Match match = plugin.getGameManager().getMatch();
        Component titleComp = MiniMessage.miniMessage().deserialize(title);
        Component subtitleComp = subtitle != null ? MiniMessage.miniMessage().deserialize(subtitle) : Component.empty();

        Title titleObj = Title.title(titleComp, subtitleComp, Title.Times.times(
                Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500)));

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.showTitle(titleObj);
        }
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) runner.showTitle(titleObj);
        }
    }

    public void sendToAll(String message) {
        Match match = plugin.getGameManager().getMatch();
        Component msg = MiniMessage.miniMessage().deserialize(message);

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.sendMessage(msg);
        }
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) runner.sendMessage(msg);
        }
    }

    public void broadcastMessage(String message) {
        Component msg = MiniMessage.miniMessage().deserialize(message);
        Bukkit.getServer().sendMessage(msg);
    }
}
