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

public class CombatListener implements Listener {
    private final ManhuntNG plugin;
    private final GameListenerState state;

    public CombatListener(ManhuntNG plugin, GameListenerState state) {
        this.plugin = plugin;
        this.state = state;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.PAUSED) {
            event.setCancelled(true);
            state.sendPauseBlockedMessage(damager);
            return;
        }

        if (match.getState() == GameState.HEADSTART) {
            event.setCancelled(true);
            return;
        }

        if (match.getState() == GameState.PRE_HUNT) {
            event.setCancelled(true);

            if (plugin.getPlayerManager().isRunner(damager.getUniqueId()) &&
                plugin.getPlayerManager().isHunter(victim.getUniqueId())) {
                plugin.getGameManager().startHunt();
            }
            return;
        }

        if (match.getState() != GameState.RUNNING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() != GameState.RUNNING && match.getState() != GameState.HEADSTART) return;

        state.destroyTrackingCompass(player, event);

        if (plugin.getPlayerManager().isRunner(uuid)) {
            Component vanilla = event.deathMessage();
            if (vanilla != null) {
                event.deathMessage(Component.text()
                        .append(plugin.getConfigManager().getMessageComponent("death.runner-prefix"))
                        .append(vanilla.colorIfAbsent(NamedTextColor.WHITE))
                        .build());
            }

            if (match.getGameMode() == xyz.qincai.manhunt.game.ManhuntGameMode.INFECTION) {
                plugin.getStatsManager().recordDeath(uuid);

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

                plugin.getPlayerManager().eliminateRunner(uuid);
                plugin.getGameManager().huntersWin();
                return;
            }

            plugin.getStatsManager().recordDeath(uuid);
            plugin.getPlayerManager().addRunnerRespawn(uuid);

            if (plugin.getConfigManager().isRunnerKeepInventory()) {
                event.setKeepInventory(true);
                event.getDrops().clear();
            } else {
                event.setKeepInventory(false);
            }

            int runnerLimit = plugin.getConfigManager().getRunnerRespawnLimit();
            if (runnerLimit >= 0 && plugin.getPlayerManager().getRunnerRespawnCount(uuid) > runnerLimit) {
                plugin.getPlayerManager().eliminateRunner(uuid);
                plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("death.runner-eliminated", "{player}", player.getName()));
                if (match.getRunnerUuids().isEmpty()) {
                    plugin.getGameManager().huntersWin();
                }
                return;
            }

            int livesLeft = runnerLimit < 0 ? -1 : runnerLimit - plugin.getPlayerManager().getRunnerRespawnCount(uuid) + 1;
            if (livesLeft >= 0) {
                plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("death.runner-lives",
                        "{player}", player.getName(), "{lives}", String.valueOf(livesLeft)));
            }
            return;
        }

        if (plugin.getPlayerManager().isHunter(uuid)) {
            Component vanilla = event.deathMessage();
            if (vanilla != null) {
                event.deathMessage(Component.text()
                        .append(plugin.getConfigManager().getMessageComponent("death.hunter-prefix"))
                        .append(vanilla.colorIfAbsent(NamedTextColor.WHITE))
                        .build());
            }

            plugin.getPlayerManager().addHunterRespawn(uuid);

            int hunterLimit = plugin.getConfigManager().getHunterRespawnLimit();
            if (hunterLimit >= 0 && plugin.getPlayerManager().getHunterRespawnCount(uuid) > hunterLimit) {
                plugin.getPlayerManager().eliminateHunter(uuid);
                plugin.getUiManager().sendToAll(plugin.getConfigManager().getMessage("death.hunter-eliminated", "{player}", player.getName()));
                if (match.getHunterUuids().isEmpty()) {
                    plugin.getGameManager().runnerWins();
                }
                return;
            }

            if (plugin.getConfigManager().isHunterKeepInventory()) {
                event.setKeepInventory(true);
                event.getDrops().clear();
            } else {
                event.setKeepInventory(false);

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

    @EventHandler
    public void onEnderDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) return;

        Match match = plugin.getGameManager().getMatch();
        if (match.getState() != GameState.RUNNING) return;

        plugin.getGameManager().runnerWins();
    }
}
