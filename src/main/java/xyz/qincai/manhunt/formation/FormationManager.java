package xyz.qincai.manhunt.formation;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

import java.util.List;
import java.util.UUID;

public class FormationManager {
    private final ManhuntNG plugin;

    public FormationManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void teleportToFormation() {
        Match match = plugin.getGameManager().getMatch();
        if (match.getRunnerUuid() == null) return;

        Player runner = org.bukkit.Bukkit.getPlayer(match.getRunnerUuid());
        if (runner == null) return;

        double radius = plugin.getConfigManager().getHunterCircleRadius();
        List<UUID> hunters = List.copyOf(match.getHunterUuids());
        int hunterCount = hunters.size();

        runner.teleport(runner.getLocation().add(0, 1, 0));

        if (hunterCount == 0) return;

        double angleStep = 2 * Math.PI / hunterCount;

        for (int i = 0; i < hunterCount; i++) {
            UUID hunterUuid = hunters.get(i);
            Player hunter = org.bukkit.Bukkit.getPlayer(hunterUuid);
            if (hunter == null) continue;

            double angle = angleStep * i;
            double x = runner.getLocation().getX() + radius * Math.cos(angle);
            double z = runner.getLocation().getZ() + radius * Math.sin(angle);
            double y = runner.getLocation().getY();

            World world = runner.getWorld();
            Location loc = new Location(world, x, y, z);
            loc.setDirection(runner.getLocation().toVector().subtract(loc.toVector()).normalize());

            hunter.teleport(loc);
        }
    }
}
