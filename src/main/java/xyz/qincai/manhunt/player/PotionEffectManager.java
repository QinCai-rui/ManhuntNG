package xyz.qincai.manhunt.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.Match;

import java.util.List;
import java.util.UUID;

/*
 * Handles applying and clearing configured potion effects
 * for runners and hunters during the game.
 *
 * Effects are defined in config.yml
 * This simply applies and removes effects based on those lists.
 */
public class PotionEffectManager {
    private final ManhuntNG plugin;

    public PotionEffectManager(ManhuntNG plugin) {
        this.plugin = plugin;
    }

    /*
     * Applies configured potion effects to all runners and hunters.
     * Called when the hunt starts (RUNNING state) or force-start.
     */
    public void applyEffects() {
        Match match = plugin.getGameManager().getMatch();

        // Apply runner effects to all runners
        List<PotionEffect> runnerEffects = plugin.getConfigManager().getRunnerPotionEffects();
        if (!runnerEffects.isEmpty()) {
            for (UUID runnerUuid : match.getRunnerUuids()) {
                Player runner = Bukkit.getPlayer(runnerUuid);
                if (runner != null) {
                    for (PotionEffect effect : runnerEffects) {
                        runner.addPotionEffect(effect);
                    }
                }
            }
        }

        // Apply hunter effects
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

    /*
     * Clears ALL active potion effects from all runners and hunters.
     * Called when the game ends or is stopped.
     *
     * Note: This clears ALL effects, including but not limited to configured ones.
     * This ensures players return to a clean state ready for next game
     */
    public void clearEffects() {
        Match match = plugin.getGameManager().getMatch();

        // Clear effects from all runners
        for (UUID runnerUuid : match.getRunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerUuid);
            if (runner != null) {
                runner.getActivePotionEffects().stream()
                        .map(PotionEffect::getType)
                        .forEach(runner::removePotionEffect);
            }
        }

        // Clear hunter effects
        for (UUID uuid : match.getHunterUuids()) {
            Player hunter = Bukkit.getPlayer(uuid);
            if (hunter != null) {
                hunter.getActivePotionEffects().stream()
                        .map(PotionEffect::getType)
                        .forEach(hunter::removePotionEffect);
            }
        }
    }

    /*
     * Applies hunter effects to a single hunter.
     * Used when a hunter respawns mid-game.
     */
    public void applyHunterEffects(UUID hunterUuid) {
        List<PotionEffect> hunterEffects = plugin.getConfigManager().getHunterPotionEffects();
        if (hunterEffects.isEmpty()) return;

        Player hunter = Bukkit.getPlayer(hunterUuid);
        if (hunter == null) return;

        for (PotionEffect effect : hunterEffects) {
            hunter.addPotionEffect(effect);
        }
    }

    /*
     * Removes only the configured potion effects from a player.
     * NOTE: (Currently unused)
     */
    private void clearConfiguredEffects(Player player, List<PotionEffect> configuredEffects) {
        for (PotionEffect effect : configuredEffects) {
            player.removePotionEffect(effect.getType());
        }
    }
}
