package xyz.qincai.manhunt.listener;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;

import java.util.UUID;

public class GamePhaseListener implements Listener {
    private final ManhuntNG plugin;
    private final GameListenerState state;

    public GamePhaseListener(ManhuntNG plugin, GameListenerState state) {
        this.plugin = plugin;
        this.state = state;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.PAUSED) {
            if (event.getTo() == null) return;
            if (plugin.getPlayerManager().isRunner(uuid) || plugin.getPlayerManager().isHunter(uuid)) {
                event.setTo(event.getFrom());
            }
            return;
        }

        if (match.getState() == GameState.HEADSTART) {
            if (event.getTo() == null) return;
            if (plugin.getPlayerManager().isHunter(uuid)) {
                event.setTo(event.getFrom());
            }
            return;
        }

        if (match.getState() == GameState.PRE_HUNT) {
            if (event.getTo() == null) return;
            if (plugin.getPlayerManager().isRunner(uuid) || plugin.getPlayerManager().isHunter(uuid)) {
                var to = event.getTo();
                var from = event.getFrom();
                to.setX(from.getX());
                to.setY(from.getY());
                to.setZ(from.getZ());
            }
            return;
        }

        if (match.getState() == GameState.COUNTDOWN) {
            if (event.getTo() == null) return;
            if (plugin.getPlayerManager().isRunner(uuid) || plugin.getPlayerManager().isHunter(uuid)) {
                event.setTo(event.getFrom());
            }
            return;
        }

        if (match.getState() == GameState.RUNNING && plugin.getPlayerManager().isRunner(uuid)) {
            World fromWorld = event.getFrom().getWorld();
            World toWorld = event.getTo().getWorld();
            if (!fromWorld.equals(toWorld)) {
                plugin.getTrackerManager().updateRunnerLastKnown(Bukkit.getPlayer(uuid));
            }
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (!plugin.getGameManager().isGamePaused()) return;

        Match match = plugin.getGameManager().getMatch();
        World world = event.getBlock().getWorld();

        if (world.equals(match.getGameWorld()) ||
            world.equals(match.getNetherWorld()) ||
            world.equals(match.getEndWorld())) {

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (!plugin.getGameManager().isGamePaused()) return;

        Match match = plugin.getGameManager().getMatch();
        World world = event.getBlock().getWorld();

        if (world.equals(match.getGameWorld()) ||
            world.equals(match.getNetherWorld()) ||
            world.equals(match.getEndWorld())) {

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getGameManager().isGamePaused()) return;

        if (!plugin.getPlayerManager().isRunner(player.getUniqueId()) &&
            !plugin.getPlayerManager().isHunter(player.getUniqueId())) return;

        event.setCancelled(true);
        state.sendPauseBlockedMessage(player);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!plugin.getGameManager().isGamePaused()) return;
        if (!(event.getTarget() instanceof Player target)) return;

        Match match = plugin.getGameManager().getMatch();
        World world = event.getEntity().getWorld();

        if (!(world.equals(match.getGameWorld()) ||
              world.equals(match.getNetherWorld()) ||
              world.equals(match.getEndWorld()))) return;

        if (plugin.getPlayerManager().isRunner(target.getUniqueId()) ||
            plugin.getPlayerManager().isHunter(target.getUniqueId())) {

            event.setCancelled(true);
        }
    }
}
