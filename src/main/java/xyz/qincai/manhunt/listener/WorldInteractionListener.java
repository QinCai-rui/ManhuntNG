package xyz.qincai.manhunt.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import xyz.qincai.manhunt.ManhuntNG;

/*
 * Prevents restricted world interactions during frozen game phases.
 * HEADSTART: runners can interact, hunters cannot.
 * PRE_HUNT / COUNTDOWN / PAUSED: both teams are frozen.
 * Also prevents hunters from dropping their tracking compass.
 */
public class WorldInteractionListener implements Listener {
    private final ManhuntNG plugin;
    private final GameListenerState state;

    public WorldInteractionListener(ManhuntNG plugin, GameListenerState state) {
        this.plugin = plugin;
        this.state = state;
    }

    // Prevent interaction during restricted phases
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        state.cancelRestrictedAction(event, event.getPlayer());
    }

    // Prevent block breaking during restricted phases
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        state.cancelRestrictedAction(event, event.getPlayer());
    }

    // Prevent block placing during restricted phases
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        state.cancelRestrictedAction(event, event.getPlayer());
    }

    // Prevent hunters from dropping their tracking compass
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (plugin.getTrackerManager().isTrackerCompass(event.getItemDrop().getItemStack())
                && plugin.getPlayerManager().isHunter(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // Prevent hunters from moving their tracking compass OUT of their own inventory
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        // Cancel inventory interactions during restricted phases
        state.cancelRestrictedAction(event, player);

        if (!plugin.getPlayerManager().isHunter(player.getUniqueId())) {
            return;
        }

        boolean clickedTopInventory = event.getRawSlot() < event.getView().getTopInventory().getSize();
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if (plugin.getTrackerManager().isTrackerCompass(currentItem)
                && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && !clickedTopInventory) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getTrackerManager().isTrackerCompass(cursorItem)
                && clickedTopInventory) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == InventoryAction.HOTBAR_SWAP && clickedTopInventory) {
            // getHotbarButton() returns -1 for off-hand swap
            ItemStack hotbarItem = event.getHotbarButton() == -1
                    ? player.getInventory().getItemInOffHand()
                    : player.getInventory().getItem(event.getHotbarButton());

            if (plugin.getTrackerManager().isTrackerCompass(hotbarItem)) {
                event.setCancelled(true);
                return;
            }
        }

        if ((event.getAction() == InventoryAction.DROP_ALL_CURSOR
                || event.getAction() == InventoryAction.DROP_ONE_CURSOR)
                && plugin.getTrackerManager().isTrackerCompass(cursorItem)) {
            event.setCancelled(true);
        }
    }

    // Prevent hunters from dragging their tracking compass into other inventories (eg containers)
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();

        // Cancel inventory interactions during restricted phases
        state.cancelRestrictedAction(event, player);

        if (!plugin.getPlayerManager().isHunter(player.getUniqueId())) {
            return;
        }

        if (!plugin.getTrackerManager().isTrackerCompass(event.getOldCursor())) {
            return;
        }

        int topInventorySize = event.getView().getTopInventory().getSize();

        if (event.getRawSlots().stream().anyMatch(slot -> slot < topInventorySize)) {
            event.setCancelled(true);
        }
    }

    // Prevent entity interaction during restricted phases
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        state.cancelRestrictedAction(event, event.getPlayer());
    }
}
