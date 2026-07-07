package xyz.qincai.manhunt.fabric;

import io.github.miniplaceholders.core.ComponentLike;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.platform.fabric.KeyImpl;
import net.kyori.adventure.platform.fabric.PlayerLocales;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.ScoreboardServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Player.NameFormat;
import net.minecraft.world.entity.player.Player.Inventory;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.Type;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.criteria.ScoreboardNameFormat;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.platform.UIFacade;
import xyz.qincai.manhunt.ui.GamePhase;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FabricUIFacade implements UIFacade {
    private final ManhuntNG plugin;
    private volatile Audience serverAudience; // Use Audience for sending messages
    private Scoreboard scoreboard;
    private Objective objective;
    private int actionBarTaskId = -1;
    private int scoreboardTaskId = -1;
    private GamePhase currentPhase = GamePhase.OVERWORLD_PREP;
    private String[] currentEntries = new String[7]; // For scoreboard lines
    private MinecraftServer server;

    public FabricUIFacade(ManhuntNG plugin, MinecraftServer server) {
        this.plugin = plugin;
        this.server = server;
        // Initialize Audience provider on server start
        // Using ServerLifecycleEvents to ensure the server is available
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(s -> {
            this.serverAudience = FabricServerAudiences.of(s).audience();
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            this.serverAudience = null; // Clear audience on server stop
        });
    }
    
    private Audience getAudience() {
        if (serverAudience == null) {
            throw new IllegalStateException("Server is not running or Audience not initialized!");
        }
        return serverAudience;
    }

    @Override
    public void setCurrentPhase(GamePhase phase) {
        this.currentPhase = phase;
    }

    @Override
    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    @Override
    public void updatePhase() {
        Match match = plugin.getGameManager().getMatch();
        if (match.getRunnerUuid() == null) {
            currentPhase = GamePhase.WAITING;
            return;
        }

        ServerPlayer runner = server.getPlayerList().getPlayer(match.getRunnerUuid());
        if (runner == null) {
             currentPhase = GamePhase.WAITING; // Runner left or is null
             return;
        }
        
        // Compare dimension ResourceKey for accurate dimension checking
        ResourceKey<Level> dimensionKey = runner.level().dimension();

        if (dimensionKey.equals(Level.END)) {
            if (match.isBlazeRodObtained()) { // Simplified check, adjust as needed
                  currentPhase = GamePhase.FINALE;
            } else {
                  currentPhase = GamePhase.END_RUSH;
            }
        } else if (dimensionKey.equals(Level.NETHER)) {
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
        } else { // Overworld
            if (match.isStrongholdDiscovered()) {
                currentPhase = GamePhase.STRONGHOLD_DIVE;
            } else if (match.isBlazeRodObtained()) {
                currentPhase = GamePhase.RETURN_EYES;
            } else {
                currentPhase = GamePhase.OVERWORLD_PREP;
            }
        }
    }

    @Override
    public void startUIUpdates() {
        stopUIUpdates(); // Ensure previous updates are stopped
        setupScoreboard();

        int actionBarInterval = plugin.getConfigProvider().getTrackingUpdateTicks(); 
        if (plugin.getConfigProvider().isActionBarEnabled()) {
            actionBarTaskId = plugin.getScheduler().runTaskTimer(this::updateActionBar, 0L, actionBarInterval);
        }
        if (plugin.getConfigProvider().isScoreboardEnabled()) {
            scoreboardTaskId = plugin.getScheduler().runTaskTimer(this::updateScoreboard, 0L, 20L); 
        }
    }

    @Override
    public void stopUIUpdates() {
        if (actionBarTaskId != -1) {
            plugin.getScheduler().cancelTask(actionBarTaskId);
            actionBarTaskId = -1;
        }
        if (scoreboardTaskId != -1) {
            plugin.getScheduler().cancelTask(scoreboardTaskId);
            scoreboardTaskId = -1;
        }
        if (objective != null) {
            objective = null; 
            scoreboard = null;
        }
        for (int i = 0; i < currentEntries.length; i++) {
            currentEntries[i] = null;
        }
    }

    private void setupScoreboard() {
        this.scoreboard = server.getScoreboard();
        this.objective = scoreboard.getObjective("manhunt");
        if (objective == null) {
             objective = scoreboard.addObjective("manhunt", ObjectiveCriteria.DUMMY, Component.literal("Manhunt"), ObjectiveCriteria.RenderType.INTEGER);
        }
        objective.setDisplaySlot(ObjectiveCriteria.RenderType.SIDEBAR.slot());
    }

    private void updateScoreboard() {
        Match match = plugin.getGameManager().getMatch();
        if (objective == null) setupScoreboard();
        if (objective == null) return;

        try {
            updateScoreboardLine(0, match.getRunnerUuid() != null
                    ? "Runner: " + getRunnerName(match)
                    : "Runner: None");
            updateScoreboardLine(1, ""); 
            updateScoreboardLine(2, "Hunters: " + match.getHunterUuids().size());
            updateScoreboardLine(3, " ");
            updateScoreboardLine(4, "Time: " + formatTime(match.getElapsedSeconds()));
            updateScoreboardLine(5, "  ");
            updateScoreboardLine(6, "Dimension: " + getDimensionName(match));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating scoreboard", e);
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
             if (player.getScoreboard() != scoreboard) {
                 player.setScoreboard(scoreboard); 
             }
             player.displayObjective(objective); 
        }
    }

    private void updateScoreboardLine(int index, String text) {
        String old = currentEntries[index];
        if (text.equals(old)) return;

        try {
            if (old != null) {
                objective.getScore(Component.literal(old)).resetScore();
            }
            MutableComponent scoreTextComp = Component.literal(text);
            objective.getScore(scoreTextComp).setScore(7 - index); 
            currentEntries[index] = text;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to update scoreboard line " + index + " with text: " + text, e);
        }
    }

    private String getRunnerName(Match match) {
        ServerPlayer runner = server.getPlayerList().getPlayer(match.getRunnerUuid());
        return runner != null ? runner.getDisplayName().getString() : "Unknown";
    }

    private String formatTime(long elapsedSeconds) {
        long hours = elapsedSeconds / 3600;
        long minutes = (elapsedSeconds % 3600) / 60;
        long seconds = elapsedSeconds % 60;
        if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String getDimensionName(Match match) {
        UUID runnerUuid = match.getRunnerUuid();
        if (runnerUuid == null) return "Overworld";
        ServerPlayer runner = server.getPlayerList().getPlayer(runnerUuid);
        if (runner == null) return "Overworld";
        
        // Using ResourceLocation path for dimension comparison
        return switch (runner.level().dimension().location().getPath()) {
            case "the_end" -> "End";
            case "nether" -> "Nether";
            default -> "Overworld";
        };
    }

    private void updateActionBar() {
        Match match = plugin.getGameManager().getMatch();
        if (match.getState() != GameState.RUNNING && match.getState() != GameState.PAUSED) return;

        if (match.getState() == GameState.PAUSED) {
            getAudience().sendActionBar(Component.text("GAME PAUSED").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            return;
        }

        updatePhase();
        Component phaseComp = Component.text(currentPhase.getDisplay()).color(NamedTextColor.GOLD);

        boolean showDistance = plugin.getConfigProvider().isTrackingShowDistance();
        ServerPlayer runner = match.getRunnerUuid() != null ? server.getPlayerList().getPlayer(match.getRunnerUuid()) : null;

        Collection<ServerPlayer> hunters = new ArrayList<>();
        for (UUID hunterUuid : match.getHunterUuids()) {
            ServerPlayer hunter = server.getPlayerList().getPlayer(hunterUuid);
            if (hunter != null) hunters.add(hunter);
        }

        for (ServerPlayer hunter : hunters) {
            if (showDistance && runner != null && hunter.level().dimension().equals(runner.level().dimension())) {
                 int dist = (int) Math.round(hunter.position().distanceTo(runner.position()));
                 Component trackingComp = Component.text("Runner — ").color(NamedTextColor.GOLD)
                      .append(Component.text(dist + "m").color(NamedTextColor.WHITE));
                 sendActionBarToPlayer(hunter, trackingComp);
            } else {
                 sendActionBarToPlayer(hunter, phaseComp);
            }
        }
        if (runner != null) sendActionBarToPlayer(runner, phaseComp);
    }

    private void sendActionBarToAll(Component message) {
        getAudience().sendActionBar(message);
    }

    private void sendActionBarToPlayer(ServerPlayer player, Component message) {
        player.getAudience().sendActionBar(message); // Use Adventure's Audience on player
    }

    @Override
    public void showPauseTitle() {
        sendTitleToAllPlayers(
            Component.text("GAME PAUSED").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
            Component.text("Use /manhunt resume to continue").color(NamedTextColor.GRAY),
            10, 24 * 60 * 60 * 20, 10 // fadeIn, stay, fadeOut ticks
        );
    }

    @Override
    public void hidePauseTitle() {
        sendTitleToAllPlayers(Component.empty(), Component.empty(), -1, -1, -1);
    }

    @Override
    public void sendTitle(String title, String subtitle) {
        sendTitleToAllPlayers(
            Component.text(title).color(NamedTextColor.GOLD),
            subtitle != null ? Component.text(subtitle).color(NamedTextColor.YELLOW) : Component.empty(),
            10, 60, 10 // Default times: 0.5s, 3s, 0.5s
        );
    }

    private void sendTitleToAllPlayers(Component title, Component subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        Collection<ServerPlayer> players = server.getPlayerList().getPlayers();
        
        // Using Adventure's Title API
        net.kyori.adventure.title.Title adventureTitle = net.kyori.adventure.title.Title.title(title, subtitle, net.kyori.adventure.title.Times.times(Duration.ofMillis(fadeInTicks * 50L), Duration.ofMillis(stayTicks * 50L), Duration.ofMillis(fadeOutTicks * 50L)));
        
        for (ServerPlayer player : players) {
            player.getAudience().openSubtitle(adventureTitle); // Audience API handles sending titles
        }
    }

    @Override
    public void sendToAll(String message) {
        Component msg = Component.text(message).color(NamedTextColor.WHITE);
        sendActionBarToAll(msg); // Send as action bar
    }

    @Override
    public void broadcastMessage(String message) {
        Component msg = Component.text(message).color(NamedTextColor.WHITE);
        server.getPlayerList().broadcastSystemMessage(msg, false); // Send as chat message
    }
}
