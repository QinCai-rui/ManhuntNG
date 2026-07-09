package xyz.qincai.manhunt.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;

import java.util.UUID;

/**
 * Applies configurable role suffixes (e.g. red [H] for hunters, green [R] for runners)
 * to a player's nametag. Uses MiniMessage so colours are not limited to legacy Bukkit codes.
 *
 * The suffix can be shown on the overhead nametag and/or the tab list, both toggleable.
 */
public class NameTagManager {
    private final ManhuntNG plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public NameTagManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void applyTag(Player player, PlayerRole role) {
        if (player == null) return;

        if (!plugin.getConfigManager().isNameTagsEnabled() || role == PlayerRole.SPECTATOR) {
            clearTag(player);
            return;
        }

        String suffixStr = plugin.getConfigManager().getNameTagSuffix(role);
        Component suffix = (suffixStr == null || suffixStr.isBlank())
                ? Component.empty()
                : miniMessage.deserialize(suffixStr);

        Component tag = Component.text(player.getName());
        if (!suffix.equals(Component.empty())) {
            tag = tag.append(Component.space()).append(suffix);
        }

        if (plugin.getConfigManager().isNameTagsOverheadEnabled()) {
            player.customName(tag);
            player.setCustomNameVisible(true);
        } else {
            player.customName(null);
            player.setCustomNameVisible(false);
        }

        if (plugin.getConfigManager().isNameTagsTabListEnabled()) {
            player.displayName(tag);
        } else {
            player.displayName(null);
        }
    }

    public void applyTag(UUID uuid, PlayerRole role) {
        applyTag(Bukkit.getPlayer(uuid), role);
    }

    public void clearTag(Player player) {
        if (player == null) return;
        player.customName(null);
        player.setCustomNameVisible(false);
        player.displayName(null);
    }

    public void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearTag(player);
        }
    }
}
