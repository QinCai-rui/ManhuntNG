package xyz.qincai.manhunt.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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

public class GameListenerState {
    private final ManhuntNG plugin;
    private final Map<UUID, Long> pauseMessageCooldowns = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, ItemStack> savedOffhand = new HashMap<>();
    private final Map<UUID, Location> savedLocations = new HashMap<>();

    public GameListenerState(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void clearSavedItems() {
        savedArmor.clear();
        savedOffhand.clear();
        savedLocations.clear();
    }

    public void saveLocation(UUID uuid, Location location) {
        savedLocations.put(uuid, location);
    }

    public Location removeSavedLocation(UUID uuid) {
        return savedLocations.remove(uuid);
    }

    public void saveArmor(UUID uuid, ItemStack[] armor) {
        savedArmor.put(uuid, armor.clone());
    }

    public ItemStack[] removeSavedArmor(UUID uuid) {
        return savedArmor.remove(uuid);
    }

    public void saveOffhand(UUID uuid, ItemStack offhand) {
        savedOffhand.put(uuid, offhand.clone());
    }

    public ItemStack removeSavedOffhand(UUID uuid) {
        return savedOffhand.remove(uuid);
    }

    public void sendPauseBlockedMessage(Player player) {
        long now = System.currentTimeMillis();
        pauseMessageCooldowns.entrySet().removeIf(e -> now - e.getValue() > 60_000);

        UUID uuid = player.getUniqueId();
        if (now - pauseMessageCooldowns.getOrDefault(uuid, 0L) > 5000) {
            player.sendMessage(plugin.getConfigManager().getMessageComponent("pause.blocked"));
            pauseMessageCooldowns.put(uuid, now);
        }
    }

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
