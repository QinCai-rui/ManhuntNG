package xyz.qincai.manhunt.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/*
 * Shared mutable state for the splitted game listeners.
 * Has the saved armour/locations, pause-message cooldowns,
 * and utils methods used across CombatListener, PlayerLifecycleListener,
 * WorldInteractionListener, and GamePhaseListener.
 */
public class GameListenerState {
    private final ManhuntNG plugin;

    // Cooldown to prevent spam warning/info messages
    private final Map<UUID, Long> pauseMessageCooldowns = new HashMap<>();

    // Saved armour for hunters who keep inventory on death
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();

    // Saved offhand for hunters who keep inventory on death
    private final Map<UUID, ItemStack> savedOffhand = new HashMap<>();

    // Saved location for participants who disconnect mid-game
    private final Map<UUID, Location> savedLocations = new HashMap<>();

    public GameListenerState(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    // Clears all state. Called when a game ends or resets.
    public void clearSavedItems() {
        savedArmor.clear();
        savedOffhand.clear();
        savedLocations.clear();
    }

    // Saves the location a player was at when they disconnected mid-game. just in case
    public void saveLocation(UUID uuid, Location location) {
        savedLocations.put(uuid, location);
    }

    // Returns and removes the saved location for a reconnecting player. ^
    public Location removeSavedLocation(UUID uuid) {
        return savedLocations.remove(uuid);
    }

    // Saves a clone of the hunter's armor array before death.
    public void saveArmor(UUID uuid, ItemStack[] armor) {
        savedArmor.put(uuid, armor.clone());
    }

    // Returns and removes the saved armour for a respawning player.
    public ItemStack[] removeSavedArmor(UUID uuid) {
        return savedArmor.remove(uuid);
    }

    // Saves a clone of the hunter's offhand item before death.
    public void saveOffhand(UUID uuid, ItemStack offhand) {
        savedOffhand.put(uuid, offhand.clone());
    }

    // Returns and removes the saved offhand for a respawning plaer.
    public ItemStack removeSavedOffhand(UUID uuid) {
        return savedOffhand.remove(uuid);
    }

    /*
     * Sends a "game paused" message with cooldown to avoid spam.
     * Prunes entries older than 60 seconds each call.
     */
    public void sendPauseBlockedMessage(Player player) {
        long now = System.currentTimeMillis();
        pauseMessageCooldowns.entrySet().removeIf(e -> now - e.getValue() > 60_000);

        UUID uuid = player.getUniqueId();
        if (now - pauseMessageCooldowns.getOrDefault(uuid, 0L) > 5000) {
            player.sendMessage(plugin.getConfigManager().getMessageComponent("pause.blocked"));
            pauseMessageCooldowns.put(uuid, now);
        }
    }

    /*
     * Cancels restricted actions during HEADSTART (hunter-only),
     * and during PRE_HUNT / COUNTDOWN / PAUSED (runner & hunter).
     * If the game is paused the player also receives a throttled warning. see above
     */
    public void cancelRestrictedAction(Cancellable event, Player player) {
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.HEADSTART) {
            if (plugin.getPlayerManager().isHunter(uuid)) {
                event.setCancelled(true);
            }
            return;
        }

        if (match.getState() == GameState.PRE_HUNT ||
            match.getState() == GameState.COUNTDOWN ||
            match.getState() == GameState.PAUSED) {

            if (plugin.getPlayerManager().isRunner(uuid) ||
                plugin.getPlayerManager().isHunter(uuid)) {

                event.setCancelled(true);
                if (plugin.getGameManager().isGamePaused()) sendPauseBlockedMessage(player);
            }
        }
    }

    // removes all tracking compasses from a dying player's drops and inventory
    // so the compass is never obtainable by other players.
    // note: a compass is regiven to hunters on respawn
    public void destroyTrackingCompass(Player player, PlayerDeathEvent event) {
        event.getDrops().removeIf(item -> plugin.getTrackerManager().isTrackerCompass(item));

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (plugin.getTrackerManager().isTrackerCompass(item)) {
                player.getInventory().clear(i);
            }
        }
    }
}
