package xyz.qincai.manhunt.listener;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.TeleportCause;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/*
 * Fixes portal teleportation for generated Manhunt worlds.
 * Vanilla/Paper portal-linking fails to resolve the destination world when
 * multiple worlds of the same environment exist (e.g. world_nether and
 * manhunt_<timestamp>_nether). This listener intercepts portal-caused
 * teleports originating from a Manhunt game world, cancels the vanilla
 * teleport, and re-teleports the player into the correct Manhunt dimension
 * using vanilla coordinate scaling (1:8 for nether, 1:1 for end) and a
 * portal search in the destination world.
 */
public class PortalListener implements Listener {
    private final ManhuntNG plugin;
    private final Set<UUID> teleporting = new HashSet<>();

    public PortalListener(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        TeleportCause cause = event.getCause();
        if (cause != TeleportCause.NETHER_PORTAL && cause != TeleportCause.END_PORTAL) {
            return;
        }

        Player player = event.getPlayer();
        if (teleporting.remove(player.getUniqueId())) {
            return;
        }

        if (!plugin.getGameManager().isGameActive()) {
            return;
        }

        Match match = plugin.getGameManager().getMatch();
        World fromWorld = event.getFrom().getWorld();
        if (fromWorld == null) {
            return;
        }

        World toWorld = resolveDestinationWorld(match, fromWorld, cause);
        if (toWorld == null) {
            return;
        }

        Location to = event.getTo();
        Location target = computeDestination(fromWorld, toWorld, to, cause);

        event.setCancelled(true);
        teleporting.add(player.getUniqueId());
        player.teleport(target, cause);
    }

    private World resolveDestinationWorld(Match match, World fromWorld, TeleportCause cause) {
        World gameWorld = match.getGameWorld();
        World netherWorld = match.getNetherWorld();
        World endWorld = match.getEndWorld();

        if (gameWorld != null && fromWorld.equals(gameWorld)) {
            return cause == TeleportCause.END_PORTAL ? endWorld : netherWorld;
        }
        if (netherWorld != null && fromWorld.equals(netherWorld)) {
            return gameWorld;
        }
        if (endWorld != null && fromWorld.equals(endWorld)) {
            return gameWorld;
        }
        return null;
    }

    private Location computeDestination(World fromWorld, World toWorld, Location originalTo, TeleportCause cause) {
        double scale = cause == TeleportCause.NETHER_PORTAL
                ? (fromWorld.getEnvironment() == World.Environment.NETHER ? 8.0 : 1.0 / 8.0)
                : 1.0;

        double x = originalTo.getX() * scale;
        double y = originalTo.getY();
        double z = originalTo.getZ() * scale;

        return new Location(toWorld, x, y, z, originalTo.getYaw(), originalTo.getPitch());
    }
}
