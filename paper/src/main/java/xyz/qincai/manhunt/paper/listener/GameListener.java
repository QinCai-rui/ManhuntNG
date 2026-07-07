package xyz.qincai.manhunt.paper.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.qincai.manhunt.PaperManhuntNG;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameListener implements Listener {
    private final PaperManhuntNG plugin;
    private final Map<UUID, Long> pauseMessageCooldowns = new HashMap<>();
<<<<<<< HEAD:src/main/java/xyz/qincai/manhunt/listener/GameListener.java
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, ItemStack> savedOffhand = new HashMap<>();
    private final NamespacedKey trackerKey;
    private final Map<World.Environment, Location> runnerLastKnownLocations = new HashMap<>();

    public GameListener(PaperManhuntNG plugin) {
        this.plugin = plugin;
        this.trackerKey = new NamespacedKey(plugin, "tracking_compass");
    }

    public void clearSavedItems() {
        savedArmor.clear();
        savedOffhand.clear();
    }

    private void sendPauseBlockedMessage(Player player) {
        long now = System.currentTimeMillis();
        pauseMessageCooldowns.entrySet().removeIf(e -> now - e.getValue() > 60_000);

        UUID uuid = player.getUniqueId();
        if (now - pauseMessageCooldowns.getOrDefault(uuid, 0L) > 5000) {
            player.sendMessage(Component.text("The game is paused \u2014 action blocked", NamedTextColor.GRAY));
            pauseMessageCooldowns.put(uuid, now);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (!plugin.getGameManager().isGameActive()) {
            plugin.getPlayerRegistry().setRole(player.getUniqueId(), PlayerRole.SPECTATOR);
            match.addSpectator(player.getUniqueId());
            return;
        }

        if (plugin.getPlayerRegistry().isHunter(player.getUniqueId())) {
            if (match.getState() == GameState.RUNNING) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        giveCompassToPlayer(player);
                    }
                }, 20L);
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();
        if (match.getState() == GameState.RUNNING
                && player.getUniqueId().equals(match.getRunnerUuid())) {
            updateRunnerLastKnown(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Match match = plugin.getGameManager().getMatch();
        if (match.getState() != GameState.RUNNING) return;
        if (!event.getPlayer().getUniqueId().equals(match.getRunnerUuid())) return;
        plugin.getUIFacade().sendToAll("\u00a7eRunner has disconnected — pausing game!");
        plugin.getGameManager().pauseGame();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() != GameState.RUNNING) return;

        destroyTrackingCompass(player, event);

        if (plugin.getPlayerRegistry().isRunner(uuid)) {
            Component vanilla = event.deathMessage();
            if (vanilla != null) {
                event.deathMessage(Component.text()
                        .append(Component.text(player.getName() + " (Runner) ", NamedTextColor.RED))
                        .append(vanilla.colorIfAbsent(NamedTextColor.WHITE))
                        .build());
            }
            plugin.getPlayerRegistry().eliminateRunner(uuid);
            plugin.getGameManager().huntersWin();
        } else if (plugin.getPlayerRegistry().isHunter(uuid)) {
            Component vanilla = event.deathMessage();
            if (vanilla != null) {
                event.deathMessage(Component.text()
                        .append(Component.text("(Hunter) ", NamedTextColor.GOLD))
                        .append(vanilla.colorIfAbsent(NamedTextColor.WHITE))
                        .build());
            }

            plugin.getPlayerRegistry().addHunterRespawn(uuid);

            if (!plugin.getConfigProvider().isHunterInfiniteRespawns()) {
                int limit = plugin.getConfigProvider().getHunterRespawnLimit();
                if (plugin.getPlayerRegistry().getHunterRespawnCount(uuid) > limit) {
                    plugin.getPlayerRegistry().eliminateHunter(uuid);
                    return;
                }
            }

            if (plugin.getConfigProvider().isHunterKeepInventory()) {
                event.setKeepInventory(true);
                event.getDrops().clear();
            } else {
                event.setKeepInventory(false);
<<<<<<< HEAD:src/main/java/xyz/qincai/manhunt/listener/GameListener.java

                if (plugin.getConfigManager().isHunterKeepArmor()) {
=======
                if (!plugin.getConfigProvider().isHunterKeepArmor()) {
                    event.getDrops().clear();
                } else {
>>>>>>> feat/fabric-support:paper/src/main/java/xyz/qincai/manhunt/paper/listener/GameListener.java
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    savedArmor.put(uuid, armor.clone());

                    for (ItemStack item : armor) {
                        if (item != null && item.getType() != Material.AIR) {
                            event.getDrops().remove(item);
                        }
                    }

                    if (plugin.getConfigManager().isHunterKeepOffhand()) {
                        ItemStack offhand = player.getInventory().getItemInOffHand();
                        if (offhand != null && offhand.getType() != Material.AIR) {
                            savedOffhand.put(uuid, offhand.clone());
                            event.getDrops().remove(offhand);
                        }
                    }
                }
            }
        }
    }

    private void destroyTrackingCompass(Player player, PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isTrackerCompass);
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isTrackerCompass(item)) {
                player.getInventory().clear(i);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() != GameState.RUNNING) return;

        if (plugin.getPlayerRegistry().isHunter(uuid)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack[] armor = savedArmor.remove(uuid);
                ItemStack offhand = savedOffhand.remove(uuid);

                if (!player.isOnline()) return;
                player.setGameMode(GameMode.SURVIVAL);

<<<<<<< HEAD:src/main/java/xyz/qincai/manhunt/listener/GameListener.java
                if (armor != null) {
                    player.getInventory().setArmorContents(armor);
                }
                if (offhand != null) {
                    player.getInventory().setItemInOffHand(offhand);
                }

                plugin.getTrackerManager().giveCompassToPlayer(player);
                plugin.getUiManager().sendToAll("\u00a7e" + player.getName() + " has respawned!");
=======
                giveCompassToPlayer(player);
                plugin.getUIFacade().sendToAll("\u00a7e" + player.getName() + " has respawned!");
>>>>>>> feat/fabric-support:paper/src/main/java/xyz/qincai/manhunt/paper/listener/GameListener.java
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.PAUSED) {
            event.setCancelled(true);
            sendPauseBlockedMessage(damager);
            return;
        }

        if (match.getState() == GameState.PRE_HUNT) {
            event.setCancelled(true);

            if (plugin.getPlayerRegistry().isRunner(damager.getUniqueId()) &&
                    plugin.getPlayerRegistry().isHunter(victim.getUniqueId())) {
                plugin.getGameManager().startHunt();
            }
            return;
        }

        if (match.getState() != GameState.RUNNING) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.PAUSED) {
            if (event.getTo() == null) return;
            if (plugin.getPlayerRegistry().isRunner(uuid) || plugin.getPlayerRegistry().isHunter(uuid)) {
                event.setTo(event.getFrom());
            }
            return;
        }

        if (match.getState() == GameState.PRE_HUNT) {
            if (event.getTo() == null) return;
            if (plugin.getPlayerRegistry().isRunner(uuid) || plugin.getPlayerRegistry().isHunter(uuid)) {
                org.bukkit.Location to = event.getTo();
                org.bukkit.Location from = event.getFrom();
                to.setX(from.getX());
                to.setY(from.getY());
                to.setZ(from.getZ());
            }
            return;
        }

        if (match.getState() == GameState.COUNTDOWN) {
            if (event.getTo() == null) return;
            if (plugin.getPlayerRegistry().isRunner(uuid) || plugin.getPlayerRegistry().isHunter(uuid)) {
                event.setTo(event.getFrom());
            }
            return;
        }

        if (match.getState() == GameState.RUNNING && plugin.getPlayerRegistry().isRunner(uuid)) {
            World fromWorld = event.getFrom().getWorld();
            World toWorld = event.getTo().getWorld();
            if (!fromWorld.equals(toWorld)) {
                updateRunnerLastKnown(Bukkit.getPlayer(uuid));
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.PRE_HUNT || match.getState() == GameState.COUNTDOWN || match.getState() == GameState.PAUSED) {
            if (plugin.getPlayerRegistry().isRunner(player.getUniqueId()) ||
                    plugin.getPlayerRegistry().isHunter(player.getUniqueId())) {
                event.setCancelled(true);
                if (plugin.getGameManager().isGamePaused()) sendPauseBlockedMessage(player);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.PRE_HUNT || match.getState() == GameState.COUNTDOWN || match.getState() == GameState.PAUSED) {
            if (plugin.getPlayerRegistry().isRunner(player.getUniqueId()) ||
                    plugin.getPlayerRegistry().isHunter(player.getUniqueId())) {
                event.setCancelled(true);
                if (plugin.getGameManager().isGamePaused()) sendPauseBlockedMessage(player);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.PRE_HUNT || match.getState() == GameState.COUNTDOWN || match.getState() == GameState.PAUSED) {
            if (plugin.getPlayerRegistry().isRunner(player.getUniqueId()) ||
                    plugin.getPlayerRegistry().isHunter(player.getUniqueId())) {
                event.setCancelled(true);
                if (plugin.getGameManager().isGamePaused()) sendPauseBlockedMessage(player);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (isTrackerCompass(event.getItemDrop().getItemStack())
                && plugin.getPlayerRegistry().isHunter(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (match.getState() == GameState.PRE_HUNT || match.getState() == GameState.COUNTDOWN || match.getState() == GameState.PAUSED) {
            if (plugin.getPlayerRegistry().isRunner(player.getUniqueId()) ||
                    plugin.getPlayerRegistry().isHunter(player.getUniqueId())) {
                event.setCancelled(true);
                if (plugin.getGameManager().isGamePaused()) sendPauseBlockedMessage(player);
            }
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (!plugin.getGameManager().isGamePaused()) return;

        Match match = plugin.getGameManager().getMatch();
        if (event.getBlock().getWorld().equals(org.bukkit.Bukkit.getWorld(match.getGameWorldName())) ||
                event.getBlock().getWorld().equals(org.bukkit.Bukkit.getWorld(match.getNetherWorldName())) ||
                event.getBlock().getWorld().equals(org.bukkit.Bukkit.getWorld(match.getEndWorldName()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (!plugin.getGameManager().isGamePaused()) return;

        Match match = plugin.getGameManager().getMatch();
        if (event.getBlock().getWorld().equals(org.bukkit.Bukkit.getWorld(match.getGameWorldName())) ||
                event.getBlock().getWorld().equals(org.bukkit.Bukkit.getWorld(match.getNetherWorldName())) ||
                event.getBlock().getWorld().equals(org.bukkit.Bukkit.getWorld(match.getEndWorldName()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getGameManager().isGamePaused()) return;
        if (!plugin.getPlayerRegistry().isRunner(player.getUniqueId()) &&
                !plugin.getPlayerRegistry().isHunter(player.getUniqueId())) return;
        event.setCancelled(true);
        sendPauseBlockedMessage(player);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!plugin.getGameManager().isGamePaused()) return;
        if (!(event.getTarget() instanceof Player target)) return;

        Match match = plugin.getGameManager().getMatch();
        org.bukkit.World world = event.getEntity().getWorld();
        if (!(world.equals(org.bukkit.Bukkit.getWorld(match.getGameWorldName())) || world.equals(org.bukkit.Bukkit.getWorld(match.getNetherWorldName())) || world.equals(org.bukkit.Bukkit.getWorld(match.getEndWorldName())))) return;

        if (plugin.getPlayerRegistry().isRunner(target.getUniqueId()) || plugin.getPlayerRegistry().isHunter(target.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnderDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) return;

        Match match = plugin.getGameManager().getMatch();
        if (match.getState() != GameState.RUNNING) return;

        plugin.getGameManager().runnerWins();
    }

    @EventHandler
    public void onAdvancementGrant(PlayerAdvancementDoneEvent event) {
        if (!plugin.getGameManager().isGameActive()) return;

        Player player = event.getPlayer();
        Match match = plugin.getGameManager().getMatch();

        if (!plugin.getPlayerRegistry().isRunner(player.getUniqueId())) return;

        NamespacedKey key = event.getAdvancement().getKey();

        switch (key.toString()) {
            case "minecraft:nether/find_fortress" -> match.setFortressDiscovered(true);
            case "minecraft:nether/find_bastion" -> match.setBastionDiscovered(true);
            case "minecraft:nether/obtain_blaze_rod" -> match.setBlazeRodObtained(true);
            case "minecraft:story/follow_ender_eye" -> match.setStrongholdDiscovered(true);
        }
    }

    private void giveCompassToPlayer(Player player) {
        if (findCompass(player) == null) {
            player.getInventory().addItem(createTrackerCompass());
        }
    }

    private void updateRunnerLastKnown(Player runner) {
        if (runner == null) return;
        runnerLastKnownLocations.put(runner.getWorld().getEnvironment(), runner.getLocation().clone());
    }

    private boolean isTrackerCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(trackerKey, PersistentDataType.BYTE);
    }

    private ItemStack createTrackerCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.getPersistentDataContainer().set(trackerKey, PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
        return compass;
    }

    private ItemStack findCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS) {
                if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(trackerKey, PersistentDataType.BYTE)) {
                    return item;
                }
            }
        }
        return null;
    }
}
