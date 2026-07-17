package xyz.qincai.manhunt.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

public class WorldInteractionListener implements Listener {
    private final ManhuntNG plugin;
    private final GameListenerState state;

    public WorldInteractionListener(ManhuntNG plugin, GameListenerState state) {
        this.plugin = plugin;
        this.state = state;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        state.cancelRestrictedAction(event, event.getPlayer());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        state.cancelRestrictedAction(event, event.getPlayer());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        state.cancelRestrictedAction(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (plugin.getTrackerManager().isTrackerCompass(event.getItemDrop().getItemStack())
                && plugin.getPlayerManager().isHunter(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        state.cancelRestrictedAction(event, event.getPlayer());
    }
}
