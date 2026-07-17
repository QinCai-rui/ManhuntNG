package xyz.qincai.manhunt.listener;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

public class AdvancementListener implements Listener {
    private final ManhuntNG plugin;

    public AdvancementListener(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAdvancementGrant(PlayerAdvancementDoneEvent event) {
        if (!plugin.getGameManager().isGameActive()) return;

        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (!plugin.getPlayerManager().isRunner(player.getUniqueId())) return;

        NamespacedKey key = event.getAdvancement().getKey();

        switch (key.toString()) {
            case "minecraft:nether/find_fortress" -> match.setFortressDiscovered(true);
            case "minecraft:nether/find_bastion" -> match.setBastionDiscovered(true);
            case "minecraft:nether/obtain_blaze_rod" -> match.setBlazeRodObtained(true);
            case "minecraft:story/follow_ender_eye" -> match.setStrongholdDiscovered(true);
        }
    }
}
