package xyz.qincai.manhunt.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

import java.util.List;
import java.util.UUID;

public class PotionEffectManager {
    private final ManhuntNG plugin;

    public PotionEffectManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    public void applyEffects() {
        Match match = plugin.getGameManager().getMatch();

        List<PotionEffect> runnerEffects = plugin.getConfigManager().getRunnerPotionEffects();
        if (match.getRunnerUuid() != null && !runnerEffects.isEmpty()) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) {
                for (PotionEffect effect : runnerEffects) {
                    runner.addPotionEffect(effect);
                }
            }
        }

        List<PotionEffect> hunterEffects = plugin.getConfigManager().getHunterPotionEffects();
        if (!hunterEffects.isEmpty()) {
            for (UUID uuid : match.getHunterUuids()) {
                Player hunter = Bukkit.getPlayer(uuid);
                if (hunter != null) {
                    for (PotionEffect effect : hunterEffects) {
                        hunter.addPotionEffect(effect);
                    }
                }
            }
        }
    }

    public void clearEffects() {
        Match match = plugin.getGameManager().getMatch();

        if (match.getRunnerUuid() != null) {
            Player runner = Bukkit.getPlayer(match.getRunnerUuid());
            if (runner != null) {
                clearConfiguredEffects(runner, plugin.getConfigManager().getRunnerPotionEffects());
            }
        }

        for (UUID uuid : match.getHunterUuids()) {
            Player hunter = Bukkit.getPlayer(uuid);
            if (hunter != null) {
                clearConfiguredEffects(hunter, plugin.getConfigManager().getHunterPotionEffects());
            }
        }
    }

    public void applyHunterEffects(UUID hunterUuid) {
        List<PotionEffect> hunterEffects = plugin.getConfigManager().getHunterPotionEffects();
        if (hunterEffects.isEmpty()) return;

        Player hunter = Bukkit.getPlayer(hunterUuid);
        if (hunter == null) return;

        for (PotionEffect effect : hunterEffects) {
            hunter.addPotionEffect(effect);
        }
    }

    private void clearConfiguredEffects(Player player, List<PotionEffect> configuredEffects) {
        for (PotionEffect effect : configuredEffects) {
            player.removePotionEffect(effect.getType());
        }
    }
}
