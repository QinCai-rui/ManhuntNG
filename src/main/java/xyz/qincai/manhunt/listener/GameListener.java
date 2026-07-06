package xyz.qincai.manhunt.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.UUID;

public class GameListener implements Listener {
    private final ManhuntNG plugin;

    public GameListener(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getGameManager().isGameActive()) {
            plugin.getPlayerManager().setRole(player.getUniqueId(), PlayerRole.SPECTATOR);
            plugin.getGameManager().getMatch().addSpectator(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() != GameState.RUNNING) return;

        if (plugin.getPlayerManager().isRunner(uuid)) {
            event.setDeathMessage("\u00a7c\u00a7l" + player.getName() + " (Runner) has been eliminated!");
            plugin.getPlayerManager().eliminateRunner(uuid);
            plugin.getStatsManager().recordDeath(uuid);
            plugin.getGameManager().huntersWin();
        } else if (plugin.getPlayerManager().isHunter(uuid)) {
            plugin.getPlayerManager().addHunterRespawn(uuid);

            if (!plugin.getConfigManager().isHunterInfiniteRespawns()) {
                int limit = plugin.getConfigManager().getHunterRespawnLimit();
                if (plugin.getPlayerManager().getHunterRespawnCount(uuid) > limit) {
                    plugin.getPlayerManager().eliminateHunter(uuid);
                    event.setDeathMessage("\u00a7c" + player.getName() + " (Hunter) has been eliminated!");
                } else {
                    event.setDeathMessage("\u00a7e" + player.getName() + " (Hunter) died! Respawning...");
                }
            } else {
                event.setDeathMessage("\u00a7e" + player.getName() + " (Hunter) died! Respawning...");
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() != GameState.RUNNING) return;

        if (plugin.getPlayerManager().isHunter(uuid)) {
            if (match.getGameWorld() != null) {
                event.setRespawnLocation(match.getGameWorld().getSpawnLocation());
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.setGameMode(GameMode.SURVIVAL);
                    plugin.getUiManager().sendToAll("\u00a7e" + player.getName() + " has respawned!");
                }, 1L);
            }
        } else if (plugin.getPlayerManager().isRunner(uuid)) {
            event.setRespawnLocation(match.getGameWorld().getSpawnLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.PAUSED) {
            event.setCancelled(true);
            return;
        }

        if (match.getState() == GameState.PRE_HUNT) {
            event.setCancelled(true);

            if (plugin.getPlayerManager().isRunner(damager.getUniqueId()) &&
                    plugin.getPlayerManager().isHunter(victim.getUniqueId())) {
                plugin.getGameManager().startHunt();
            }
            return;
        }

        if (match.getState() != GameState.RUNNING) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.COUNTDOWN || match.getState() == GameState.PRE_HUNT || match.getState() == GameState.PAUSED) {
            if (event.getTo() == null) return;
            if (plugin.getPlayerManager().isRunner(uuid) || plugin.getPlayerManager().isHunter(uuid)) {
                if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                        event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                    event.setTo(event.getFrom());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.PRE_HUNT || match.getState() == GameState.COUNTDOWN || match.getState() == GameState.PAUSED) {
            if (plugin.getPlayerManager().isRunner(player.getUniqueId()) ||
                    plugin.getPlayerManager().isHunter(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.PRE_HUNT || match.getState() == GameState.COUNTDOWN || match.getState() == GameState.PAUSED) {
            if (plugin.getPlayerManager().isRunner(player.getUniqueId()) ||
                    plugin.getPlayerManager().isHunter(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.PRE_HUNT || match.getState() == GameState.COUNTDOWN || match.getState() == GameState.PAUSED) {
            if (plugin.getPlayerManager().isRunner(player.getUniqueId()) ||
                    plugin.getPlayerManager().isHunter(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.PRE_HUNT || match.getState() == GameState.COUNTDOWN || match.getState() == GameState.PAUSED) {
            if (plugin.getPlayerManager().isRunner(player.getUniqueId()) ||
                    plugin.getPlayerManager().isHunter(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEnderDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) return;

        Match match = plugin.getGameManager().getMatch();
        if (match.getState() != GameState.RUNNING) return;

        plugin.getGameManager().runnerWins();
    }
}
