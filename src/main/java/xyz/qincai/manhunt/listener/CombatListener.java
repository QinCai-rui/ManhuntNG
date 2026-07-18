package xyz.qincai.manhunt.listener;

import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;

import java.util.UUID;

/*
 * Handles PvP rules and death logic for runners and hunters.
 * - PvP is blocked during PAUSED / HEADSTART / PRE_HUNT / COUNTDOWN.
 * - Runner hitting hunter during PRE_HUNT starts the hunt.
 * - Runner death: infection -> conversion, elimination, or respawn limits.
 * - Hunter death: respawn limits, inventory/armour keep rules.
 * - Ender Dragon death -> runner win.
 */
public class CombatListener implements Listener {
    private final ManhuntNG plugin;
    private final GameListenerState state;

    public CombatListener(ManhuntNG plugin, GameListenerState state) {
        this.plugin = plugin;
        this.state = state;
    }

    /*
     * Handles PvP rules:
     * - PAUSED -> cancel
     * - HEADSTART -> cancel (prep phase)
     * - PRE_HUNT -> runner hitting hunter starts hunt
     * - RUNNING -> allow
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Resolve the actual damager (handle projectiles)
        Player damager = null;
        if (event.getDamager() instanceof Player player) {
            damager = player;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                damager = shooter;
            }
        }
        if (damager == null) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        Match match = plugin.getGameManager().getMatch();

        // No combat during pause
        if (match.getState() == GameState.PAUSED) {
            event.setCancelled(true);
            state.sendPauseBlockedMessage(damager);
            return;
        }

        // HEADSTART: no combat
        if (match.getState() == GameState.HEADSTART) {
            event.setCancelled(true);
            return;
        }

        // PRE_HUNT: runner must hit hunter to start game
        if (match.getState() == GameState.PRE_HUNT) {
            event.setCancelled(true);

            if (plugin.getPlayerManager().isRunner(damager.getUniqueId()) &&
                plugin.getPlayerManager().isHunter(victim.getUniqueId())) {
                plugin.getGameManager().startHunt();
            }
            return;
        }

        // No combat outside RUNNING
        if (match.getState() != GameState.RUNNING) {
            event.setCancelled(true);
        }
    }

    /*
     * Handles player death logic for runner + hunters.
     * - Runner death -> hunters win (or infection conversion)
     * - Hunter death -> respawn logic, inventory rules, respawn limits
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() != GameState.RUNNING && match.getState() != GameState.HEADSTART) return;

        // Remove tracking compasses so other players can't pick up
        state.destroyTrackingCompass(player, event);

        // Record kill/death statistics uniformly for all deaths
        Player killer = player.getKiller();
        if (killer != null) {
            plugin.getStatsManager().recordKill(killer.getUniqueId(), uuid);
        } else {
            plugin.getStatsManager().recordDeath(uuid);
        }

        // Runner death
        if (plugin.getPlayerManager().isRunner(uuid)) {
            // Prefix the vanilla death message
            Component vanilla = event.deathMessage();
            if (vanilla != null) {
                event.deathMessage(Component.text()
                        .append(plugin.getConfigManager().getMessageComponent("death.runner-prefix"))
                        .append(vanilla.colorIfAbsent(NamedTextColor.WHITE))
                        .build());
            }

            // Infection mode: runner becomes a hunter (only during RUNNING)
            if (match.getGameMode() == xyz.qincai.manhunt.game.ManhuntGameMode.INFECTION) {
                // Only convert to hunter if match is RUNNING
                if (match.getState() == GameState.RUNNING) {
                    if (plugin.getConfigManager().isRunnerKeepInventory()) {
                        event.setKeepInventory(true);
                        event.getDrops().clear();
                    } else {
                        event.setKeepInventory(false);
                    }
                    plugin.getGameManager().infectPlayer(uuid);
                    return;
                }

                // For HEADSTART or other states, handle as normal elimination
                plugin.getPlayerManager().eliminateRunner(uuid);
                plugin.getGameManager().huntersWin();
                return;
            }

            // Normal mode: check respawn limit before eliminating
            plugin.getPlayerManager().addRunnerRespawn(uuid);

            // Handle keepInventory for runners
            if (plugin.getConfigManager().isRunnerKeepInventory()) {
                event.setKeepInventory(true);
                event.getDrops().clear();
            } else {
                event.setKeepInventory(false);
            }

            int runnerLimit = plugin.getConfigManager().getRunnerRespawnLimit();
            if (runnerLimit >= 0 && plugin.getPlayerManager().getRunnerRespawnCount(uuid) > runnerLimit) {
                // No lives left - eliminate
                plugin.getPlayerManager().eliminateRunner(uuid);
                plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("death.runner-eliminated", "{player}", player.getName()));
                if (match.getRunnerUuids().isEmpty()) {
                    plugin.getGameManager().huntersWin();
                }
                return;
            }

            // Lives remaining - broadcast lives message
            int livesLeft = runnerLimit < 0 ? -1 : runnerLimit - plugin.getPlayerManager().getRunnerRespawnCount(uuid) + 1;
            if (livesLeft >= 0) {
                plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("death.runner-lives",
                        "{player}", player.getName(), "{lives}", String.valueOf(livesLeft)));
            }
            return;
        }

        // Hunter death -> respawn logic
        if (plugin.getPlayerManager().isHunter(uuid)) {
            Component vanilla = event.deathMessage();
            if (vanilla != null) {
                event.deathMessage(Component.text()
                        .append(plugin.getConfigManager().getMessageComponent("death.hunter-prefix"))
                        .append(vanilla.colorIfAbsent(NamedTextColor.WHITE))
                        .build());
            }

            plugin.getPlayerManager().addHunterRespawn(uuid);

            // Check respawn limit
            int hunterLimit = plugin.getConfigManager().getHunterRespawnLimit();
            if (hunterLimit >= 0 && plugin.getPlayerManager().getHunterRespawnCount(uuid) > hunterLimit) {
                plugin.getPlayerManager().eliminateHunter(uuid);
                plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("death.hunter-eliminated", "{player}", player.getName()));
                if (match.getHunterUuids().isEmpty()) {
                    plugin.getGameManager().runnerWins();
                }
                return;
            }

            // Inventory rules
            if (plugin.getConfigManager().isHunterKeepInventory()) {
                event.setKeepInventory(true);
                event.getDrops().clear();
            } else {
                event.setKeepInventory(false);

                // Keep armour/offhand if enabled
                if (plugin.getConfigManager().isHunterKeepArmor()) {
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    state.saveArmor(uuid, armor);

                    for (ItemStack item : armor) {
                        if (item != null && item.getType() != Material.AIR) {
                            event.getDrops().remove(item);
                        }
                    }
                }

                if (plugin.getConfigManager().isHunterKeepOffhand()) {
                    ItemStack offhand = player.getInventory().getItemInOffHand();
                    if (offhand != null && offhand.getType() != Material.AIR) {
                        state.saveOffhand(uuid, offhand);
                        event.getDrops().remove(offhand);
                    }
                }
            }
        }
    }

    // Ender Dragon death -> runner wins
    @EventHandler
    public void onEnderDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) return;

        Match match = plugin.getGameManager().getMatch();
        if (match.getState() != GameState.RUNNING) return;

        plugin.getGameManager().runnerWins();
    }
}
