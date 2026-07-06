package xyz.qincai.manhunt.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;

import java.time.Duration;
import java.util.UUID;

public class UIManager {
    private final ManhuntNG plugin;
    private org.bukkit.scoreboard.Scoreboard scoreboard;
    private long actionBarTaskId = -1;
    private long scoreboardTaskId = -1;
    private GamePhase currentPhase = GamePhase.OVERWORLD_PREP;

    public UIManager(ManhuntNG plugin) {
        this.plugin = plugin;
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

        if (runner.getWorld().getEnvironment() == World.Environment.THE_END) {
            currentPhase = GamePhase.END_RUSH;
        } else if (runner.getWorld().getEnvironment() == World.Environment.NETHER) {
            if (runner.getInventory().contains(org.bukkit.Material.BLAZE_ROD)) {
                currentPhase = GamePhase.BASTION_ROUTE;
            } else {
                currentPhase = GamePhase.NETHER_RUSH;
            }
        } else {
            if (runner.getInventory().contains(org.bukkit.Material.ENDER_PEARL)) {
                currentPhase = GamePhase.RETURN_EYES;
            } else if (runner.getInventory().contains(org.bukkit.Material.BLAZE_ROD)) {
                currentPhase = GamePhase.RETURN_EYES;
            } else {
                currentPhase = GamePhase.OVERWORLD_PREP;
            }
        }
    }

    public void startUIUpdates() {
        stopUIUpdates();
        if (plugin.getConfigManager().isActionBarEnabled()) {
            actionBarTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::updateActionBar, 0L, 20L).getTaskId();
        }
        if (plugin.getConfigManager().isScoreboardEnabled()) {
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
    }

    private void updateActionBar() {
        Match match = plugin.getGameManager().getMatch();
        if (match.getState() != GameState.RUNNING) return;

        updatePhase();

        Component actionBar = Component.text(currentPhase.getDisplay(), NamedTextColor.GOLD);

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.sendActionBar(actionBar);
        }
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) runner.sendActionBar(actionBar);
        }
    }

    private void updateScoreboard() {
        Match match = plugin.getGameManager().getMatch();

        Objective obj = scoreboard.getObjective("manhunt");
        if (obj != null) obj.unregister();

        obj = scoreboard.registerNewObjective("manhunt", Criteria.DUMMY, Component.text("Manhunt", NamedTextColor.GOLD));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = 6;

        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            String runnerName = runner != null ? runner.getName() : "Unknown";
            Score runnerScore = obj.getScore(Component.text("Runner: " + runnerName, NamedTextColor.RED));
            runnerScore.setScore(line--);
        }

        obj.getScore(Component.empty()).setScore(line--);

        int aliveHunters = plugin.getPlayerManager().getAliveHunterCount();
        Score huntersScore = obj.getScore(Component.text("Hunters: " + aliveHunters, NamedTextColor.AQUA));
        huntersScore.setScore(line--);

        obj.getScore(Component.empty()).setScore(line--);

        long elapsed = match.getElapsedSeconds();
        String timeStr = String.format("%02d:%02d:%02d", elapsed / 3600, (elapsed % 3600) / 60, elapsed % 60);
        Score timeScore = obj.getScore(Component.text("Time: " + timeStr, NamedTextColor.YELLOW));
        timeScore.setScore(line--);

        obj.getScore(Component.empty()).setScore(line--);

        String dimension = "Overworld";
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) {
                dimension = switch (runner.getWorld().getEnvironment()) {
                    case NETHER -> "Nether";
                    case THE_END -> "End";
                    default -> "Overworld";
                };
            }
        }
        Score dimScore = obj.getScore(Component.text("Dimension: " + dimension, NamedTextColor.GREEN));
        dimScore.setScore(line--);

        obj.getScore(Component.empty()).setScore(line--);

        String dragonStatus = "Alive";
        if (match.getGameWorld() != null) {
            for (org.bukkit.entity.Entity entity : match.getGameWorld().getEntities()) {
                if (entity instanceof org.bukkit.entity.EnderDragon dragon) {
                    if (dragon.getHealth() <= 0) {
                        dragonStatus = "Dead";
                    }
                    break;
                }
            }
        }
        Score dragonScore = obj.getScore(Component.text("Dragon: " + dragonStatus, NamedTextColor.LIGHT_PURPLE));
        dragonScore.setScore(line);

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.setScoreboard(scoreboard);
        }
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) runner.setScoreboard(scoreboard);
        }
    }

    public void sendTitle(String title, String subtitle) {
        Match match = plugin.getGameManager().getMatch();
        Component titleComp = Component.text(title, NamedTextColor.GOLD);
        Component subtitleComp = subtitle != null ? Component.text(subtitle, NamedTextColor.YELLOW) : Component.empty();

        Title titleObj = Title.title(titleComp, subtitleComp, Title.Times.times(
                Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500)));

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.showTitle(titleObj);
        }
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) runner.showTitle(titleObj);
        }
    }

    public void sendToAll(String message) {
        Match match = plugin.getGameManager().getMatch();
        Component msg = Component.text(message, NamedTextColor.WHITE);

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.sendMessage(msg);
        }
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) runner.sendMessage(msg);
        }
    }

    public void broadcastMessage(String message) {
        Component msg = Component.text(message, NamedTextColor.WHITE);
        Bukkit.getServer().sendMessage(msg);
    }
}
