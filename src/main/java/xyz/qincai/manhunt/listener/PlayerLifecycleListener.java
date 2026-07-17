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

public class PlayerLifecycleListener implements Listener {
    private final ManhuntNG plugin;
    private final GameListenerState state;

    public PlayerLifecycleListener(ManhuntNG plugin, GameListenerState state) {
        this.plugin = plugin;
        this.state = state;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (!plugin.getGameManager().isGameActive()) {
            plugin.getPlayerManager().setRole(player.getUniqueId(), PlayerRole.SPECTATOR);
            match.addSpectator(player.getUniqueId());
            return;
        }

        Location savedLocation = state.removeSavedLocation(player.getUniqueId());
        if (savedLocation != null && savedLocation.getWorld() != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.teleport(savedLocation);
                }
            }, 5L);
        }

        if (plugin.getPlayerManager().isHunter(player.getUniqueId())) {
            if (match.getState() == GameState.RUNNING) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.getTrackerManager().giveCompassToPlayer(player);
                    }
                }, 20L);
            }
        }

        plugin.getNameTagManager().applyTag(player, plugin.getPlayerManager().getRole(player.getUniqueId()));
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.RUNNING &&
                match.isRunner(player.getUniqueId())) {
            plugin.getTrackerManager().updateRunnerLastKnown(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (plugin.getGameManager().isGameActive() && match.isParticipant(uuid)) {
            state.saveLocation(uuid, player.getLocation());
        }

        if (match.getState() != GameState.RUNNING && match.getState() != GameState.HEADSTART) return;

        if (match.getRunnerUuids().isEmpty() || match.getHunterUuids().isEmpty()) return;

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

        if (!anyRunnerOnline) {
            plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("pause.runners-disconnected"));
            plugin.getGameManager().pauseGame();
        } else if (!anyHunterOnline) {
            plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("pause.hunters-disconnected"));
            plugin.getGameManager().pauseGame();
        }
    }

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
