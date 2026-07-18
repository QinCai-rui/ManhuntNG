package xyz.qincai.manhunt.listener;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

/*
 * Tracks runner advancements to update progression flags.
 * These flags are used by the UI and stats systems to show game progress
 * (fortress discovered, bastion discovered, blaze rod obtained, stronghold entered).
 */
public class AdvancementListener implements Listener {
    private final ManhuntNG plugin;

    public AdvancementListener(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAdvancementGrant(PlayerAdvancementDoneEvent event) {
        // Only track advancements when game is active
        if (!plugin.getGameManager().isGameActive()) return;

        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        // Only runner advancements matter for progression
        if (!plugin.getPlayerManager().isRunner(player.getUniqueId())) return;

        NamespacedKey key = event.getAdvancement().getKey();

        // Update progression (using advancement keys)
        switch (key.toString()) {
            case "minecraft:nether/find_fortress" -> match.setFortressDiscovered(true);
            case "minecraft:nether/find_bastion" -> match.setBastionDiscovered(true);
            case "minecraft:nether/loot_bastion" -> match.setBastionDiscovered(true);
            case "minecraft:nether/obtain_blaze_rod" -> match.setBlazeRodObtained(true);
            case "minecraft:story/follow_ender_eye" -> match.setStrongholdDiscovered(true);
        }
    }
}
