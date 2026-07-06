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
import org.bukkit.scoreboard.Scoreboard;
import xyz.qincai.manhunt.ManhuntNG;
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

    private String[] currentEntries = new String[8];

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
        setupScoreboard();
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
        objective = scoreboard.registerNewObjective("manhunt", Criteria.DUMMY, Component.text("Manhunt", NamedTextColor.GOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    private void updateScoreboard() {
        Match match = plugin.getGameManager().getMatch();
        if (objective == null) return;

        updateLine(0, match.getRunnerUuid() != null
                ? "Runner: " + getRunnerName(match)
                : "Runner: None");
        updateLine(1, "");
        updateLine(2, "Hunters: " + plugin.getPlayerManager().getAliveHunterCount());
        updateLine(3, " ");
        updateLine(4, "Time: " + formatTime(match.getElapsedSeconds()));
        updateLine(5, "  ");
        updateLine(6, "Dimension: " + getDimension(match));
        updateLine(7, "Dragon: " + getDragonStatus(match));

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.setScoreboard(scoreboard);
        }
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
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
        return String.format("%02d:%02d:%02d",
                elapsedSeconds / 3600,
                (elapsedSeconds % 3600) / 60,
                elapsedSeconds % 60);
    }

    private String getDimension(Match match) {
        if (match.getRunnerUuid() == null) return "Overworld";
        Player runner = Bukkit.getPlayer(match.getRunnerUuid());
        if (runner == null) return "Overworld";
        return switch (runner.getWorld().getEnvironment()) {
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> "Overworld";
        };
    }

    private String getDragonStatus(Match match) {
        World endWorld = match.getEndWorld();
        if (endWorld == null) return "Unknown";
        boolean alive = endWorld.getEntitiesByClass(org.bukkit.entity.EnderDragon.class)
                .stream()
                .anyMatch(dragon -> dragon.getHealth() > 0);
        return alive ? "Alive" : "Dead";
    }

    private void updateActionBar() {
        Match match = plugin.getGameManager().getMatch();
        if (match.getState() != GameState.RUNNING && match.getState() != GameState.PAUSED) return;

        if (match.getState() == GameState.PAUSED) {
            Component pausedBar = Component.text("\u00a7e\u23F8 PAUSED", NamedTextColor.GOLD);
            for (UUID uuid : match.getHunterUuids()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) player.sendActionBar(pausedBar);
            }
            if (match.getRunnerUuid() != null) {
                Player runner = Bukkit.getPlayer(match.getRunnerUuid());
                if (runner != null) runner.sendActionBar(pausedBar);
            }
            return;
        }

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

    public void showPauseTitle() {
        Match match = plugin.getGameManager().getMatch();
        Component titleComp = Component.text("\u00a7e\u00a7lGAME PAUSED", NamedTextColor.GOLD);
        Component subtitleComp = Component.text("\u00a77Use /manhunt resume to continue", NamedTextColor.GRAY);
        Title titleObj = Title.title(titleComp, subtitleComp, Title.Times.times(
                Duration.ofMillis(500), Duration.ofHours(24), Duration.ofMillis(500)));

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.showTitle(titleObj);
        }
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) runner.showTitle(titleObj);
        }
    }

    public void hidePauseTitle() {
        Match match = plugin.getGameManager().getMatch();

        for (UUID uuid : match.getHunterUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.resetTitle();
        }
        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) runner.resetTitle();
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
