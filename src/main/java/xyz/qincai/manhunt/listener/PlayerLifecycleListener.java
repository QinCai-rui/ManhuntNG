package xyz.qincai.manhunt.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.UUID;

/*
 * Handles player join, quit, respawn, and world-change events
 * Restores reconnecting players to their saved location (just in case other plugins cancel vanilla),
 * gives hunters a compass on mid-game join, auto-pauses when
 * an entire team disconnects, and restores inventory on respawn
 */
public class PlayerLifecycleListener implements Listener {
    private final ManhuntNG plugin;
    private final GameListenerState state;

    public PlayerLifecycleListener(ManhuntNG plugin, GameListenerState state) {
        this.plugin = plugin;
        this.state = state;
    }

    /*
     * Handles player joining the server.
     * If game is inactive -> they become spectator.
     * If hunter joins during RUNNING -> give compass after short delay.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (!plugin.getGameManager().isGameActive()) {
            plugin.getPlayerManager().setRole(player.getUniqueId(), PlayerRole.SPECTATOR);
            match.addSpectator(player.getUniqueId());
            plugin.getWorldManager().teleportToLobby(player);
            return;
        }

        // Reconnecting participant: teleport back to where they left the game
        Location savedLocation = state.removeSavedLocation(player.getUniqueId());
        if (savedLocation != null && savedLocation.getWorld() != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.teleport(savedLocation);
                }
            }, 5L);
        }

        // Hunters joining mid-game -> get compass
        if (plugin.getPlayerManager().isHunter(player.getUniqueId())) {
            if (match.getState() == GameState.RUNNING) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.getTrackerManager().giveCompassToPlayer(player);
                    }
                }, 20L);
            }
        }

        // Re-apply the role nametag (covers mid-game rejoins)
        plugin.getNameTagManager().applyTag(player, plugin.getPlayerManager().getRole(player.getUniqueId()));
    }

    // Updates runner last-known location when switching worlds.
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.RUNNING &&
                match.isRunner(player.getUniqueId())) {
            plugin.getTrackerManager().updateRunnerLastKnown(player);
        }
    }

    /*
     * Saves location of any participant leaving an active game so they can be
     * teleported back when they reconnect. 
     * If every runner or every hunter disconnects, the game is auto-paused.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (plugin.getGameManager().isGameActive() && match.isParticipant(uuid)) {
            state.saveLocation(uuid, player.getLocation());
        }

        if (match.getState() != GameState.RUNNING && match.getState() != GameState.HEADSTART) return;

        // No team left -> nothing to pause for
        if (match.getRunnerUuids().isEmpty() || match.getHunterUuids().isEmpty()) return;

        // Check if any runner/hunter (other than the one quitting) is still online.
        // The quitting player is excluded because they are no longer part of the
        // active game even if the server still reports them as present.
        boolean anyRunnerOnline = match.getRunnerUuids().stream()
                .filter(id -> !id.equals(uuid))
                .anyMatch(id -> {
                    Player p = Bukkit.getPlayer(id);
                    return p != null && p.isOnline();
                });

        boolean anyHunterOnline = match.getHunterUuids().stream()
                .filter(id -> !id.equals(uuid))
                .anyMatch(id -> {
                    Player p = Bukkit.getPlayer(id);
                    return p != null && p.isOnline();
                });

        // Only pause when EVERY runner or EVERY hunter has disconnected
        if (!anyRunnerOnline) {
            plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("pause.runners-disconnected"));
            plugin.getGameManager().pauseGame();
        } else if (!anyHunterOnline) {
            plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("pause.hunters-disconnected"));
            plugin.getGameManager().pauseGame();
        }
    }

    /*
     * Handles respawn logic:
     * - Hunter: restore saved armour/offhand, give compass (only during RUNNING), announce
     * - Runner: reapply potion effects, broadcast lives remaining
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() != GameState.RUNNING && match.getState() != GameState.HEADSTART) return;

        if (plugin.getPlayerManager().isHunter(uuid)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack[] armor = state.removeSavedArmor(uuid);
                ItemStack offhand = state.removeSavedOffhand(uuid);

                if (!player.isOnline()) return;

                player.setGameMode(GameMode.SURVIVAL);

                if (armor != null) player.getInventory().setArmorContents(armor);
                if (offhand != null) player.getInventory().setItemInOffHand(offhand);

                // Only give compass during RUNNING
                if (match.getState() == GameState.RUNNING) {
                    plugin.getTrackerManager().giveCompassToPlayer(player);
                }
                plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("respawn.broadcast", "{player}", player.getName()));
            }, 1L);
        }

        if (plugin.getPlayerManager().isRunner(uuid)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                player.setGameMode(GameMode.SURVIVAL);

                // Reapply runner potion effects
                plugin.getPotionEffectManager().applyRunnerEffects(uuid);

                int runnerLimit = plugin.getConfigManager().getRunnerRespawnLimit();
                int livesLeft = runnerLimit < 0 ? -1 : runnerLimit - plugin.getPlayerManager().getRunnerRespawnCount(uuid) + 1;
                if (livesLeft >= 0) {
                    plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("respawn.runner",
                            "{player}", player.getName(), "{lives}", String.valueOf(livesLeft)));
                } else {
                    plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("respawn.broadcast", "{player}", player.getName()));
                }
            }, 1L);
        }
    }
}
